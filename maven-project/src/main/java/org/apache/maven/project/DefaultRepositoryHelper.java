package org.apache.maven.project;

import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.profiles.build.ProfileAdvisor;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactStatus;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.MavenTools;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import java.util.*;
import java.io.*;

/**
 * This is a temporary class. These methods are originally from the DefaultMavenProjectHelper. This class will be
 * eliminated when Mercury is integrated.
 */
public class DefaultRepositoryHelper implements RepositoryHelper, Initializable, LogEnabled {

    private Logger logger;

    public static final String MAVEN_MODEL_VERSION = "4.0.0";

    private ArtifactFactory artifactFactory;

    private ArtifactResolver artifactResolver;

    private MavenTools mavenTools;

    private ProfileAdvisor profileAdvisor;

    private MavenXpp3Reader modelReader;

    private Logger getLogger() {
        return logger;
    }

    public Model findModelFromRepository(Artifact artifact,
                                         List remoteArtifactRepositories,
                                         ArtifactRepository localRepository)
            throws ProjectBuildingException {

        String projectId = safeVersionlessKey(artifact.getGroupId(), artifact.getArtifactId());
        remoteArtifactRepositories = normalizeToArtifactRepositories(remoteArtifactRepositories, projectId);

        Artifact projectArtifact;

        // if the artifact is not a POM, we need to construct a POM artifact based on the artifact parameter given.
        if ("pom".equals(artifact.getType())) {
            projectArtifact = artifact;
        } else {
            getLogger().warn("Attempting to build MavenProject instance for Artifact (" + artifact.getGroupId() + ":"
                    + artifact.getArtifactId() + ":" + artifact.getVersion() + ") of type: "
                    + artifact.getType() + "; constructing POM artifact instead.");

            projectArtifact = artifactFactory.createProjectArtifact(artifact.getGroupId(),
                    artifact.getArtifactId(),
                    artifact.getVersion(),
                    artifact.getScope());
        }

        Model legacy_model;
        try {
            artifactResolver.resolve(projectArtifact, remoteArtifactRepositories, localRepository);

            File file = projectArtifact.getFile();

            legacy_model = readModelLegacy(projectId, file, true);

            String downloadUrl = null;

            ArtifactStatus status = ArtifactStatus.NONE;

            DistributionManagement distributionManagement = legacy_model.getDistributionManagement();

            if (distributionManagement != null) {
                downloadUrl = distributionManagement.getDownloadUrl();

                status = ArtifactStatus.valueOf(distributionManagement.getStatus());
            }

            checkStatusAndUpdate(projectArtifact, status, file, remoteArtifactRepositories, localRepository);

            // TODO: this is gross. Would like to give it the whole model, but maven-artifact shouldn't depend on that
            // Can a maven-core implementation of the Artifact interface store it, and be used in the exceptions?
            if (downloadUrl != null) {
                projectArtifact.setDownloadUrl(downloadUrl);
            } else {
                projectArtifact.setDownloadUrl(legacy_model.getUrl());
            }
        }
        catch (ArtifactResolutionException e) {
            throw new ProjectBuildingException(projectId, "Error getting POM for '" + projectId + "' from the repository: " + e.getMessage(), e);
        }
        catch (ArtifactNotFoundException e) {
            throw new ProjectBuildingException(projectId, "POM '" + projectId + "' not found in repository: " + e.getMessage(), e);
        }

        return legacy_model;
    }

    public List buildArtifactRepositories(Model model)
            throws ProjectBuildingException {
        try {
            return mavenTools.buildArtifactRepositories(model.getRepositories());
        }
        catch (InvalidRepositoryException e) {
            String projectId = safeVersionlessKey(model.getGroupId(), model.getArtifactId());

            throw new ProjectBuildingException(projectId, e.getMessage(), e);
        }
    }

    /*
    * Order is:
    *
    * 1. model profile repositories
    * 2. model repositories
    * 3. superModel profile repositories
    * 4. superModel repositories
    * 5. parentSearchRepositories
    */
    public LinkedHashSet collectInitialRepositories(Model model,
                                                    Model superModel,
                                                    List parentSearchRepositories,
                                                    File pomFile,
                                                    boolean validProfilesXmlLocation,
                                                    ProfileActivationContext profileActivationContext)
            throws ProjectBuildingException {
        LinkedHashSet collected = new LinkedHashSet();

        collectInitialRepositoriesFromModel(collected, model, pomFile, validProfilesXmlLocation, profileActivationContext);

        collectInitialRepositoriesFromModel(collected, superModel, null, validProfilesXmlLocation, profileActivationContext);

        if ((parentSearchRepositories != null) && !parentSearchRepositories.isEmpty()) {
            collected.addAll(parentSearchRepositories);
        }

        return collected;
    }

    private List normalizeToArtifactRepositories(List remoteArtifactRepositories,
                                                String projectId)
            throws ProjectBuildingException {
        List normalized = new ArrayList(remoteArtifactRepositories.size());

        boolean normalizationNeeded = false;
        for (Iterator it = remoteArtifactRepositories.iterator(); it.hasNext();) {
            Object item = it.next();

            if (item instanceof ArtifactRepository) {
                normalized.add(item);
            } else if (item instanceof Repository) {
                Repository repo = (Repository) item;
                try {
                    item = mavenTools.buildArtifactRepository(repo);

                    normalized.add(item);
                    normalizationNeeded = true;
                }
                catch (InvalidRepositoryException e) {
                    throw new ProjectBuildingException(projectId, "Error building artifact repository for id: " + repo.getId(), e);
                }
            } else {
                throw new ProjectBuildingException(projectId, "Error building artifact repository from non-repository information item: " + item);
            }
        }

        if (normalizationNeeded) {
            return normalized;
        } else {
            return remoteArtifactRepositories;
        }
    }

    private String safeVersionlessKey(String groupId,
                                      String artifactId) {
        String gid = groupId;

        if (StringUtils.isEmpty(gid)) {
            gid = "unknown";
        }

        String aid = artifactId;

        if (StringUtils.isEmpty(aid)) {
            aid = "unknown";
        }

        return ArtifactUtils.versionlessKey(gid, aid);
    }

    private void checkModelVersion(String modelSource,
                                   String projectId,
                                   File file)
            throws InvalidProjectModelException {
        if (modelSource.indexOf("<modelVersion>4.0.0") < 0) {
            throw new InvalidProjectModelException(projectId, "Not a v" + MAVEN_MODEL_VERSION + " POM.", file);
        }
    }

    private Model readModelLegacy(String projectId,
                                  File file,
                                  boolean strict)
            throws ProjectBuildingException {
        Reader reader = null;
        try {
            reader = ReaderFactory.newXmlReader(file);

            String modelSource = IOUtil.toString(reader);

            checkModelVersion(modelSource, projectId, file);

            StringReader sReader = new StringReader(modelSource);

            try {
                return new MavenXpp3Reader().read(sReader, strict);
            }
            catch (XmlPullParserException e) {
                throw new InvalidProjectModelException(projectId, "Parse error reading POM. Reason: " + e.getMessage(),
                        file, e);
            }
        }
        catch (FileNotFoundException e) {
            throw new ProjectBuildingException(projectId,
                    "Could not find the model file '" + file.getAbsolutePath() + "'.", file, e);
        }
        catch (IOException e) {
            throw new ProjectBuildingException(projectId, "Failed to build model from file '" +
                    file.getAbsolutePath() + "'.\nError: \'" + e.getLocalizedMessage() + "\'", file, e);
        }
        finally {
            IOUtil.close(reader);
        }
    }

    private void collectInitialRepositoriesFromModel(LinkedHashSet collected,
                                                     Model model,
                                                     File pomFile,
                                                     boolean validProfilesXmlLocation,
                                                     ProfileActivationContext profileActivationContext)
            throws ProjectBuildingException {

        Set reposFromProfiles = profileAdvisor.getArtifactRepositoriesFromActiveProfiles(model, pomFile, validProfilesXmlLocation, profileActivationContext);

        if ((reposFromProfiles != null) && !reposFromProfiles.isEmpty()) {
            collected.addAll(reposFromProfiles);
        }

        List modelRepos = model.getRepositories();

        if ((modelRepos != null) && !modelRepos.isEmpty()) {
            try {
                collected.addAll(mavenTools.buildArtifactRepositories(modelRepos));
            }
            catch (InvalidRepositoryException e) {
                throw new ProjectBuildingException(safeVersionlessKey(model.getGroupId(), model.getArtifactId()),
                        "Failed to construct ArtifactRepository instances for repositories declared in: " +
                                model.getId(), e);
            }
        }
    }
        private void checkStatusAndUpdate( Artifact projectArtifact,
                                       ArtifactStatus status,
                                       File file,
                                       List remoteArtifactRepositories,
                                       ArtifactRepository localRepository )
        throws ArtifactNotFoundException
    {
        // TODO: configurable actions dependant on status
        if ( !projectArtifact.isSnapshot() && ( status.compareTo( ArtifactStatus.DEPLOYED ) < 0 ) )
        {
            // use default policy (enabled, daily update, warn on bad checksum)
            ArtifactRepositoryPolicy policy = new ArtifactRepositoryPolicy();
            // TODO: re-enable [MNG-798/865]
            policy.setUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER );

            if ( policy.checkOutOfDate( new Date( file.lastModified() ) ) )
            {
                getLogger().info(
                    projectArtifact.getArtifactId() + ": updating metadata due to status of '" + status + "'" );
                try
                {
                    projectArtifact.setResolved( false );
                    artifactResolver.resolveAlways( projectArtifact, remoteArtifactRepositories, localRepository );
                }
                catch ( ArtifactResolutionException e )
                {
                    getLogger().warn( "Error updating POM - using existing version" );
                    getLogger().debug( "Cause", e );
                }
                catch ( ArtifactNotFoundException e )
                {
                    getLogger().warn( "Error updating POM - not found. Removing local copy." );
                    getLogger().debug( "Cause", e );
                    file.delete();
                    throw e;
                }
            }
        }
    }


    public void initialize() throws InitializationException {
        modelReader = new MavenXpp3Reader();
    }

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }
}