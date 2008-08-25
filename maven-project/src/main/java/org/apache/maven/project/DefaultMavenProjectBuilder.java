package org.apache.maven.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.MavenTools;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.profiles.MavenProfilesBuilder;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.activation.DefaultProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.profiles.build.ProfileAdvisor;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.build.model.ModelLineageBuilder;
import org.apache.maven.project.builder.PomArtifactResolver;
import org.apache.maven.project.builder.ProjectBuilder;
import org.apache.maven.project.inheritance.ModelInheritanceAssembler;
import org.apache.maven.project.injection.ModelDefaultsInjector;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.interpolation.ModelInterpolator;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.project.validation.ModelValidator;
import org.apache.maven.project.workspace.ProjectWorkspace;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.*;
import java.net.URL;
import java.util.*;

/*:apt

 -----
 POM lifecycle
 -----

POM Lifecycle

 Order of operations when building a POM

 * inheritance
 * path translation
 * interpolation
 * defaults injection

 Current processing is:

 * inheritance
 * interpolation
 * defaults injection
 * path translation

 I'm not sure how this is working at all ... i think i have a case where this is failing but i need to
 encapsulate as a test so i can fix it. Also need to think of the in working build directory versus looking
 things up from the repository i.e buildFromSource vs buildFromRepository.

Notes

 * when the model is read it may not have a groupId, as it must be inherited

 * the inheritance assembler must use models that are unadulterated!

*/

/**
 * @version $Id$
 */
public class DefaultMavenProjectBuilder
        implements MavenProjectBuilder,
        Initializable, LogEnabled {
    protected MavenProfilesBuilder profilesBuilder;

    protected ArtifactResolver artifactResolver;

    protected ArtifactMetadataSource artifactMetadataSource;

    private ArtifactFactory artifactFactory;

    private ModelInheritanceAssembler modelInheritanceAssembler;

    private ModelValidator validator;

    // TODO: make it a component
    private MavenXpp3Reader modelReader;

    private PathTranslator pathTranslator;

    private ModelDefaultsInjector modelDefaultsInjector;

    private ModelInterpolator modelInterpolator;

    private ModelLineageBuilder modelLineageBuilder;

    private ProfileAdvisor profileAdvisor;

    private MavenTools mavenTools;

    private ProjectWorkspace projectWorkspace;

    private ProjectBuilder projectBuilder;

    private RepositoryHelper repositoryHelper;

    private Logger logger;

    //DO NOT USE, it is here only for backward compatibility reasons. The existing
    // maven-assembly-plugin (2.2-beta-1) is accessing it via reflection.

    // the aspect weaving seems not to work for reflection from plugin.
    private Map processedProjectCache = new HashMap();

    private static final String MAVEN_MODEL_VERSION = "4.0.0";

    public void initialize() {
        modelReader = new MavenXpp3Reader();
    }

    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    public MavenProject build(File projectDescriptor,
                              ArtifactRepository localRepository,
                              ProfileManager profileManager)
            throws ProjectBuildingException {
        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setLocalRepository(localRepository)
                .setGlobalProfileManager(profileManager);

        return build(projectDescriptor, config);
    }

    public MavenProject build(File projectDescriptor,
                              ProjectBuilderConfiguration config)
            throws ProjectBuildingException {
        MavenProject project = null;//projectWorkspace.getProject( projectDescriptor );

        if (project == null) {
            project = readModelFromLocalPath("unknown", projectDescriptor, new PomArtifactResolver(config.getLocalRepository(),
                    repositoryHelper.buildArtifactRepositories(getSuperModel()), artifactResolver), config);
            project = buildInternal(project.getModel(),
                    config,
                    projectDescriptor,
                    project.getParentFile(),
                    true,
                    true);
        }
        return project;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public MavenProject buildFromRepository(Artifact artifact,
                                            List remoteArtifactRepositories,
                                            ArtifactRepository localRepository,
                                            boolean allowStub)
            throws ProjectBuildingException

    {
        return buildFromRepository(artifact, remoteArtifactRepositories, localRepository);
    }


    public MavenProject buildFromRepository(Artifact artifact,
                                            List remoteArtifactRepositories,
                                            ArtifactRepository localRepository)
            throws ProjectBuildingException {
        MavenProject project = null;
        if (!Artifact.LATEST_VERSION.equals(artifact.getVersion()) && !Artifact.RELEASE_VERSION.equals(artifact.getVersion())) {
            project = projectWorkspace.getProject(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
        }

        if (project == null) {
            Model model = repositoryHelper.findModelFromRepository(artifact, remoteArtifactRepositories, localRepository);

            ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setLocalRepository(localRepository);
            project = readModelFromLocalPath("unknown", artifact.getFile(), new PomArtifactResolver(config.getLocalRepository(),
                    repositoryHelper.buildArtifactRepositories(getSuperModel()), artifactResolver), config);
            //TODO: Construct parent
            project = buildInternal(project.getModel(), config, artifact.getFile(), project.getParentFile(),
                    false, false);
        }

        return project;
    }

    // what is using this externally? jvz.
    public MavenProject buildStandaloneSuperProject()
            throws ProjectBuildingException {
        //TODO mkleint - use the (Container, Properties) constructor to make system properties embeddable
        return buildStandaloneSuperProject(new DefaultProjectBuilderConfiguration());
    }

    public MavenProject buildStandaloneSuperProject(ProfileManager profileManager)
            throws ProjectBuildingException {
        //TODO mkleint - use the (Container, Properties) constructor to make system properties embeddable
        return buildStandaloneSuperProject(new DefaultProjectBuilderConfiguration().setGlobalProfileManager(profileManager));
    }

    public MavenProject buildStandaloneSuperProject(ProjectBuilderConfiguration config)
            throws ProjectBuildingException {
        Model superModel = getSuperModel();

        superModel.setGroupId(STANDALONE_SUPERPOM_GROUPID);

        superModel.setArtifactId(STANDALONE_SUPERPOM_ARTIFACTID);

        superModel.setVersion(STANDALONE_SUPERPOM_VERSION);

        superModel = ModelUtils.cloneModel(superModel);

        ProfileManager profileManager = config.getGlobalProfileManager();

        List activeProfiles = new ArrayList();
        if (profileManager != null) {
            List activated = profileAdvisor.applyActivatedProfiles(superModel, null, false, profileManager.getProfileActivationContext());
            if (!activated.isEmpty()) {
                activeProfiles.addAll(activated);
            }

            activated = profileAdvisor.applyActivatedExternalProfiles(superModel, null, profileManager);
            if (!activated.isEmpty()) {
                activeProfiles.addAll(activated);
            }
        }

        MavenProject project;
        try {
            project = new MavenProject(superModel, artifactFactory, mavenTools, repositoryHelper, this, config);
        } catch (InvalidRepositoryException e) {
            throw new ProjectBuildingException(STANDALONE_SUPERPOM_GROUPID + ":"
                    + STANDALONE_SUPERPOM_ARTIFACTID,
                    "Maven super-POM contains an invalid repository!",
                    e);
        }

        getLogger().debug("Activated the following profiles for standalone super-pom: " + activeProfiles);
        //project.setActiveProfiles(activeProfiles);

        try {
            interpolateModelAndInjectDefault(project.getModel(), null, null, config, activeProfiles);

            project.setRemoteArtifactRepositories(mavenTools.buildArtifactRepositories(superModel.getRepositories()));
            project.setPluginArtifactRepositories(mavenTools.buildArtifactRepositories(superModel.getRepositories()));
        }
        catch (InvalidRepositoryException e) {
            throw new ProjectBuildingException(STANDALONE_SUPERPOM_GROUPID + ":"
                    + STANDALONE_SUPERPOM_ARTIFACTID,
                    "Maven super-POM contains an invalid repository!",
                    e);
        }
        catch (ModelInterpolationException e) {
            throw new ProjectBuildingException(STANDALONE_SUPERPOM_GROUPID + ":"
                    + STANDALONE_SUPERPOM_ARTIFACTID,
                    "Maven super-POM contains an invalid expressions!",
                    e);
        }

     //   project.setOriginalModel(superModel);

        project.setExecutionRoot(true);

        return project;
    }

    /**
     * @since 2.0.x
     */
    public MavenProject buildWithDependencies(File projectDescriptor,
                                              ArtifactRepository localRepository,
                                              ProfileManager profileManager)
            throws ProjectBuildingException {
        return buildProjectWithDependencies(projectDescriptor, localRepository, profileManager).getProject();
    }

    /**
     * @since 2.1
     */
    public MavenProjectBuildingResult buildProjectWithDependencies(File projectDescriptor,
                                                                   ArtifactRepository localRepository,
                                                                   ProfileManager profileManager)
            throws ProjectBuildingException {
        ProjectBuilderConfiguration config = new DefaultProjectBuilderConfiguration().setLocalRepository(localRepository)
                .setGlobalProfileManager(profileManager);

        return buildProjectWithDependencies(projectDescriptor, config);
    }

    public MavenProjectBuildingResult buildProjectWithDependencies(File projectDescriptor,
                                                                   ProjectBuilderConfiguration config)
            throws ProjectBuildingException {
        MavenProject project = build(projectDescriptor, config);
        Artifact projectArtifact = project.getArtifact();

        String projectId = safeVersionlessKey(project.getGroupId(), project.getArtifactId());

        Map managedVersions = project.getManagedVersionMap();

        try {
            project.setDependencyArtifacts(project.createArtifacts(artifactFactory, null, null));
        }
        catch (InvalidDependencyVersionException e) {
            throw new ProjectBuildingException(projectId,
                    "Unable to build project due to an invalid dependency version: " +
                            e.getMessage(), projectDescriptor, e);
        }

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                .setArtifact(projectArtifact)
                .setArtifactDependencies(project.getDependencyArtifacts())
                .setLocalRepository(config.getLocalRepository())
                .setRemoteRepostories(project.getRemoteArtifactRepositories())
                .setManagedVersionMap(managedVersions)
                .setMetadataSource(artifactMetadataSource);

        ArtifactResolutionResult result = artifactResolver.resolve(request);

        project.setArtifacts(result.getArtifacts());

        return new MavenProjectBuildingResult(project, result);
    }

    public void calculateConcreteState(MavenProject project, ProjectBuilderConfiguration config)
            throws ModelInterpolationException {
        new MavenProjectRestorer(pathTranslator, modelInterpolator, getLogger()).calculateConcreteState(project, config);
    }

    public void restoreDynamicState(MavenProject project, ProjectBuilderConfiguration config)
            throws ModelInterpolationException {
        new MavenProjectRestorer(pathTranslator, modelInterpolator, getLogger()).restoreDynamicState(project, config);
    }

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    private Logger getLogger() {
        return logger;
    }

    private MavenProject buildInternal(Model model,
                                       ProjectBuilderConfiguration config,
                                       File projectDescriptor,
                                       File parentDescriptor,
                                       boolean isReactorProject,
                                       boolean fromSourceTree)
            throws ProjectBuildingException {

        MavenProject superProject;
        try {
            superProject = new MavenProject(getSuperModel(), artifactFactory, mavenTools, repositoryHelper, this, config);
        } catch (InvalidRepositoryException e) {
            throw new ProjectBuildingException(STANDALONE_SUPERPOM_GROUPID + ":"
                    + STANDALONE_SUPERPOM_ARTIFACTID,
                    "Maven super-POM contains an invalid repository!",
                    e);
        }

        String projectId = safeVersionlessKey(model.getGroupId(), model.getArtifactId());

        // FIXME: Find a way to pass in this context, so it's never null!
        ProfileActivationContext profileActivationContext;

        ProfileManager externalProfileManager = config.getGlobalProfileManager();
        if (externalProfileManager != null) {
            // used to trigger the caching of SystemProperties in the container context...
            try {
                externalProfileManager.getActiveProfiles();
            }
            catch (ProfileActivationException e) {
                throw new ProjectBuildingException(projectId, "Failed to activate external profiles.", projectDescriptor, e);
            }

            profileActivationContext = externalProfileManager.getProfileActivationContext();
        } else {
            profileActivationContext = new DefaultProfileActivationContext(config.getExecutionProperties(), false);
        }

        LinkedHashSet activeInSuperPom = new LinkedHashSet();
        List activated = profileAdvisor.applyActivatedProfiles(getSuperModel(), projectDescriptor, isReactorProject, profileActivationContext);
        if (!activated.isEmpty()) {
            activeInSuperPom.addAll(activated);
        }

        activated = profileAdvisor.applyActivatedExternalProfiles(getSuperModel(), projectDescriptor, externalProfileManager);
        if (!activated.isEmpty()) {
            activeInSuperPom.addAll(activated);
        }

        superProject.setActiveProfiles(activated);

        MavenProject project;
        try {
            project = interpolateModelAndInjectDefault(model, projectDescriptor, parentDescriptor, config, activated);
        }
        catch (ModelInterpolationException e) {
            throw new InvalidProjectModelException(projectId, e.getMessage(), projectDescriptor, e);
        }
        catch (InvalidRepositoryException e) {
            throw new InvalidProjectModelException(projectId, e.getMessage(), projectDescriptor, e);
        }

        if (fromSourceTree) {
            Build build = project.getBuild();

            // NOTE: setting this script-source root before path translation, because
            // the plugin tools compose basedir and scriptSourceRoot into a single file.
            project.addScriptSourceRoot(build.getScriptSourceDirectory());

            project.addCompileSourceRoot(build.getSourceDirectory());

            project.addTestCompileSourceRoot(build.getTestSourceDirectory());

            // Only track the file of a POM in the source tree
            project.setFile(projectDescriptor);
        }

        projectWorkspace.storeProjectByCoordinate(project);
        projectWorkspace.storeProjectByFile(project);

        return project;
    }

    private MavenProject interpolateModelAndInjectDefault(Model model,
                                             File pomFile,
                                             File parentFile,
                                             ProjectBuilderConfiguration config,
                                             List activeProfiles
    )
            throws ProjectBuildingException, ModelInterpolationException, InvalidRepositoryException {
        File projectDir = null;
        if (pomFile != null) {
            projectDir = pomFile.getAbsoluteFile().getParentFile();
        }

        Build dynamicBuild = model.getBuild();
        if(dynamicBuild != null) {
            model.setBuild(ModelUtils.cloneBuild(dynamicBuild));    
        }
        model = modelInterpolator.interpolate(model, projectDir, config, getLogger().isDebugEnabled());

        if(dynamicBuild != null && model.getBuild() != null) {
            mergeDeterministicBuildElements(model.getBuild(), dynamicBuild);
            model.setBuild(dynamicBuild);
        }

        // interpolation is before injection, because interpolation is off-limits in the injected variables
        modelDefaultsInjector.injectDefaults(model);

        // We will return a different project object using the new model (hence the need to return a project, not just modify the parameter)
        MavenProject project = new MavenProject(model, artifactFactory, mavenTools, repositoryHelper, this, config);
        project.setActiveProfiles(activeProfiles);

        Artifact projectArtifact = artifactFactory.createBuildArtifact(project.getGroupId(), project.getArtifactId(),
                project.getVersion(), project.getPackaging());
        project.setArtifact(projectArtifact);
        project.setParentFile(parentFile);
        validateModel(model, pomFile);
        return project;
    }

    // TODO: Remove this!
    @SuppressWarnings("unchecked")
    private void mergeDeterministicBuildElements(Build interpolatedBuild,
                                                 Build dynamicBuild) {
        List<Plugin> dPlugins = dynamicBuild.getPlugins();

        if (dPlugins != null) {
            List<Plugin> iPlugins = interpolatedBuild.getPlugins();

            for (int i = 0; i < dPlugins.size(); i++) {
                Plugin dPlugin = dPlugins.get(i);
                Plugin iPlugin = iPlugins.get(i);

                dPlugin.setGroupId(iPlugin.getGroupId());
                dPlugin.setArtifactId(iPlugin.getArtifactId());
                dPlugin.setVersion(iPlugin.getVersion());

                dPlugin.setDependencies(iPlugin.getDependencies());
            }
        }

        PluginManagement dPluginMgmt = dynamicBuild.getPluginManagement();

        if (dPluginMgmt != null) {
            PluginManagement iPluginMgmt = interpolatedBuild.getPluginManagement();
            dPlugins = dPluginMgmt.getPlugins();
            if (dPlugins != null) {
                List<Plugin> iPlugins = iPluginMgmt.getPlugins();

                for (int i = 0; i < dPlugins.size(); i++) {
                    Plugin dPlugin = dPlugins.get(i);
                    Plugin iPlugin = iPlugins.get(i);

                    dPlugin.setGroupId(iPlugin.getGroupId());
                    dPlugin.setArtifactId(iPlugin.getArtifactId());
                    dPlugin.setVersion(iPlugin.getVersion());

                    dPlugin.setDependencies(iPlugin.getDependencies());
                }
            }
        }

        if (dynamicBuild.getExtensions() != null) {
            dynamicBuild.setExtensions(interpolatedBuild.getExtensions());
        }
    }

    /**
     * @param isReactorProject
     * @noinspection CollectionDeclaredAsConcreteClass
     * @todo We need to find an effective way to unit test parts of this method!
     * @todo Refactor this into smaller methods with discrete purposes.
     */
    /*
    private MavenProject assembleLineage(Model model,
                                         LinkedList lineage,
                                         ProjectBuilderConfiguration config,
                                         File pomFile,
                                         Set aggregatedRemoteWagonRepositories,
                                         boolean strict,
                                         boolean isReactorProject)
            throws ProjectBuildingException, InvalidRepositoryException {
        ModelLineage modelLineage = new DefaultModelLineage();

        modelLineage.setOrigin(model, pomFile, new ArrayList(aggregatedRemoteWagonRepositories), isReactorProject);

        modelLineageBuilder.resumeBuildingModelLineage(modelLineage, config, !strict, isReactorProject);

        // FIXME: Find a way to pass in this context, so it's never null!
        ProfileActivationContext profileActivationContext;
        ProfileManager externalProfileManager = config.getGlobalProfileManager();

        if (externalProfileManager != null) {
            profileActivationContext = externalProfileManager.getProfileActivationContext();
        } else {
            profileActivationContext = new DefaultProfileActivationContext(config.getExecutionProperties(), false);
        }

        MavenProject lastProject = null;
        for (ModelLineageIterator it = modelLineage.lineageIterator(); it.hasNext();) {
            Model currentModel = (Model) it.next();

            File currentPom = it.getPOMFile();

            MavenProject project = new MavenProject(currentModel, artifactFactory, mavenTools, repositoryHelper, this);
            project.setFile(currentPom);

            if (lastProject != null) {
                // TODO: Use cached parent project here, and stop looping, if possible...
                lastProject.setParent(project);
                project = lastProject.getParent();

                lastProject.setParentArtifact(artifactFactory.createParentArtifact(project.getGroupId(), project
                        .getArtifactId(), project.getVersion()));
            }

            // NOTE: the caching aspect may replace the parent project instance, so we apply profiles here.
            // TODO: Review this...is that a good idea, to allow application of profiles when other profiles could have been applied already?
            project.setActiveProfiles(profileAdvisor.applyActivatedProfiles(project.getModel(), project.getFile(), isReactorProject, profileActivationContext));

            lineage.addFirst(project);

            lastProject = project;
        }

        MavenProject result = (MavenProject) lineage.getLast();

        if (externalProfileManager != null) {
            LinkedHashSet active = new LinkedHashSet();

            List existingActiveProfiles = result.getActiveProfiles();
            if ((existingActiveProfiles != null) && !existingActiveProfiles.isEmpty()) {
                active.addAll(existingActiveProfiles);
            }

            profileAdvisor.applyActivatedExternalProfiles(result.getModel(), pomFile, externalProfileManager);
        }

        return result;
    }
    */
    private Model superModel;

    private Model getSuperModel()
            throws ProjectBuildingException {
        if (superModel != null) {
            return superModel;
        }

        URL url = DefaultMavenProjectBuilder.class.getResource("pom-" + MAVEN_MODEL_VERSION + ".xml");

        String projectId = safeVersionlessKey(STANDALONE_SUPERPOM_GROUPID, STANDALONE_SUPERPOM_ARTIFACTID);

        Reader reader = null;
        try {
            reader = ReaderFactory.newXmlReader(url.openStream());
            String modelSource = IOUtil.toString(reader);

            if (modelSource.indexOf("<modelVersion>" + MAVEN_MODEL_VERSION) < 0) {
                throw new InvalidProjectModelException(projectId, "Not a v" + MAVEN_MODEL_VERSION + " POM.", new File("."));
            }

            StringReader sReader = new StringReader(modelSource);

            superModel = modelReader.read(sReader, STRICT_MODEL_PARSING);
            return superModel;
        }
        catch (XmlPullParserException e) {
            throw new InvalidProjectModelException(projectId, "Parse error reading POM. Reason: " + e.getMessage(), e);
        }
        catch (IOException e) {
            throw new ProjectBuildingException(projectId, "Failed build model from URL \'" + url.toExternalForm() +
                    "\'\nError: \'" + e.getLocalizedMessage() + "\'", e);
        }
        finally {
            IOUtil.close(reader);
        }
    }

    private MavenProject readModelFromLocalPath(String projectId,
                                         File projectDescriptor,
                                         PomArtifactResolver resolver,
                                         ProjectBuilderConfiguration config
                                       )
            throws ProjectBuildingException {
        if (projectDescriptor == null) {
            throw new IllegalArgumentException("projectDescriptor: null, Project Id =" + projectId);
        }

        if (projectBuilder == null) {
            throw new IllegalArgumentException("projectBuilder: not initialized");
        }

        MavenProject mavenProject;
        try {
            mavenProject = projectBuilder.buildFromLocalPath(new FileInputStream(projectDescriptor),
                    Arrays.asList(getSuperModel()), null, null, resolver,
                    projectDescriptor.getParentFile(), config);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ProjectBuildingException(projectId, "File = " + projectDescriptor.getAbsolutePath(), e);
        }

        return mavenProject;

    }

    private void validateModel(Model model,
                               File pomFile)
            throws InvalidProjectModelException {
        // Must validate before artifact construction to make sure dependencies are good
        ModelValidationResult validationResult = validator.validate(model);

        String projectId = safeVersionlessKey(model.getGroupId(), model.getArtifactId());

        if (validationResult.getMessageCount() > 0) {
            for(String s : (List<String>) validationResult.getMessages())
            {
                System.out.println(s);
            }
            throw new InvalidProjectModelException(projectId, "Failed to validate POM", pomFile,
                    validationResult);
        }
    }

    private static String safeVersionlessKey(String groupId,
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
}
