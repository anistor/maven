package org.apache.maven.model;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Extension;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Notifier;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryBase;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;
import org.apache.maven.model.Site;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public final class ModelUtils
{

    // TODO: Replace this with MODELLO-191

    private static final ModelPartCloner<Dependency> DEPENDENCY_CLONER = new ModelPartCloner<Dependency>()
    {
        public Dependency cloneModelPart( Dependency src )
        {
            return ModelUtils.cloneDependency( src );
        }
    };

    private static final ModelPartCloner<Plugin> PLUGIN_CLONER = new ModelPartCloner<Plugin>()
    {
        public Plugin cloneModelPart( Plugin src )
        {
            return ModelUtils.clonePlugin( src );
        }
    };

    private static final ModelPartCloner<Extension> EXTENSION_CLONER = new ModelPartCloner<Extension>()
    {
        public Extension cloneModelPart( Extension src )
        {
            return ModelUtils.cloneExtension( src );
        }
    };

    private static final ModelPartCloner<Resource> RESOURCE_CLONER = new ModelPartCloner<Resource>()
    {
        public Resource cloneModelPart( Resource src )
        {
            return ModelUtils.cloneResource( src );
        }
    };

    private static final ModelPartCloner<Notifier> NOTIFIER_CLONER = new ModelPartCloner<Notifier>()
    {
        public Notifier cloneModelPart( Notifier src )
        {
            return ModelUtils.cloneNotifier( src );
        }
    };

    private static final ModelPartCloner<Contributor> CONTRIBUTOR_CLONER = new ModelPartCloner<Contributor>()
    {
        public Contributor cloneModelPart( Contributor src )
        {
            return ModelUtils.cloneContributor( src );
        }
    };

    private static final ModelPartCloner<Developer> DEVELOPER_CLONER = new ModelPartCloner<Developer>()
    {
        public Developer cloneModelPart( Developer src )
        {
            return ModelUtils.cloneDeveloper( src );
        }
    };

    private static final ModelPartCloner<License> LICENSE_CLONER = new ModelPartCloner<License>()
    {
        public License cloneModelPart( License src )
        {
            return ModelUtils.cloneLicense( src );
        }
    };

    private static final ModelPartCloner<MailingList> MAILING_LIST_CLONER = new ModelPartCloner<MailingList>()
    {
        public MailingList cloneModelPart( MailingList src )
        {
            return ModelUtils.cloneMailingList( src );
        }
    };

    private static final ModelPartCloner<Repository> REPOSITORY_CLONER = new ModelPartCloner<Repository>()
    {
        public Repository cloneModelPart( Repository src )
        {
            return ModelUtils.cloneRepository( src );
        }
    };

    private static final ModelPartCloner<Profile> PROFILE_CLONER = new ModelPartCloner<Profile>()
    {
        public Profile cloneModelPart( Profile src )
        {
            return ModelUtils.cloneProfile( src );
        }
    };

    private static final ModelPartCloner<ReportPlugin> REPORT_PLUGIN_CLONER = new ModelPartCloner<ReportPlugin>()
    {
        public ReportPlugin cloneModelPart( ReportPlugin src )
        {
            return ModelUtils.cloneReportPlugin( src );
        }
    };

    private static final ModelPartCloner<ReportSet> REPORT_SET_CLONER = new ModelPartCloner<ReportSet>()
    {
        public ReportSet cloneModelPart( ReportSet src )
        {
            return ModelUtils.cloneReportSet( src );
        }
    };

    private static final ModelPartCloner<Exclusion> DEPENDENCY_EXCLUSION_CLONER = new ModelPartCloner<Exclusion>()
    {
        public Exclusion cloneModelPart( Exclusion src )
        {
            return ModelUtils.cloneExclusion( src );
        }
    };

    private static final ModelPartCloner<PluginExecution> PLUGIN_EXECUTION_CLONER = new ModelPartCloner<PluginExecution>()
    {
        public PluginExecution cloneModelPart( PluginExecution src )
        {
            return ModelUtils.clonePluginExecution( src );
        }
    };

    public static ReportSet cloneReportSet( ReportSet src )
    {
        if ( src == null )
        {
            return null;
        }
        
        ReportSet result = new ReportSet();
        
        result.setConfiguration( cloneConfiguration( src.getConfiguration() ) );
        result.setId( src.getId() );
        result.setInherited( src.getInherited() );
        result.setReports( cloneListOfStrings( src.getReports() ) );
        
        return result;
    }

    public static ReportPlugin cloneReportPlugin( ReportPlugin src )
    {
        if ( src == null )
        {
            return null;
        }
        
        ReportPlugin result = new ReportPlugin();
        
        result.setArtifactId( src.getArtifactId() );
        result.setConfiguration( cloneConfiguration( src.getConfiguration() ) );
        result.setGroupId( src.getGroupId() );
        result.setInherited( src.getInherited() );
        result.setReportSets( cloneList( src.getReportSets(), REPORT_SET_CLONER ) );
        result.setVersion( src.getVersion() );
        
        return result;
    }

    public static Profile cloneProfile( Profile src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Profile result = new Profile();
        
        cloneModelBaseFields( src, result );
        
        result.setActivation( cloneActivation( src.getActivation() ) );
        
        BuildBase resultBuild = null;
        if ( src.getBuild() != null )
        {
            resultBuild = new BuildBase();
            cloneBuildBaseFields( src.getBuild(), resultBuild );
        }
        result.setBuild( resultBuild );
        
        result.setId( src.getId() );
        result.setSource( src.getSource() );
        
        return result;
    }

    private static void cloneModelBaseFields( ModelBase src, ModelBase result )
    {
        result.setDependencies( cloneList( src.getDependencies(), DEPENDENCY_CLONER ) );
        result.setDependencyManagement( cloneDependencyManagement( src.getDependencyManagement() ) );
        result.setDistributionManagement( cloneDistributionManagement( src.getDistributionManagement() ) );
        
        result.setModules( cloneListOfStrings( src.getModules() ) );

        result.setPluginRepositories( cloneList( src.getPluginRepositories(), REPOSITORY_CLONER ) );
        result.setProperties( cloneProperties( src.getProperties() ) );
        result.setReporting( cloneReporting( src.getReporting() ) );
        result.setRepositories( cloneList( src.getRepositories(), REPOSITORY_CLONER ) );
    }

    public static Reporting cloneReporting( Reporting src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Reporting result = new Reporting();
        
        result.setExcludeDefaults( src.isExcludeDefaults() );
        result.setOutputDirectory( src.getOutputDirectory() );
        result.setPlugins( cloneList( src.getPlugins(), REPORT_PLUGIN_CLONER ) );
        
        return result;
    }

    public static Activation cloneActivation( Activation src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Activation result = new Activation();
        result.setActiveByDefault( src.isActiveByDefault() );
        result.setFile( cloneActivationFile( src.getFile() ) );
        result.setJdk( src.getJdk() );
        result.setOs( cloneActivationOs( src.getOs() ) );
        result.setProperty( cloneActivationProperty( src.getProperty() ) );
        
        return result;
    }

    public static ActivationProperty cloneActivationProperty( ActivationProperty src )
    {
        if ( src == null )
        {
            return null;
        }
        
        ActivationProperty result = new ActivationProperty();
        
        result.setName( src.getName() );
        result.setValue( src.getValue() );
        
        return result;
    }

    public static ActivationOS cloneActivationOs( ActivationOS src )
    {
        if ( src == null )
        {
            return null;
        }
        
        ActivationOS result = new ActivationOS();
        
        result.setArch( src.getArch() );
        result.setFamily( src.getFamily() );
        result.setName( src.getName() );
        result.setVersion( src.getVersion() );
        
        return result;
    }

    public static ActivationFile cloneActivationFile( ActivationFile src )
    {
        if ( src == null )
        {
            return null;
        }
        
        ActivationFile result = new ActivationFile();
        
        result.setExists( src.getExists() );
        result.setMissing( src.getMissing() );

        return result;
    }

    public static Repository cloneRepository( Repository src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Repository result = new Repository();
        
        result.setReleases( cloneRepositoryPolicy( src.getReleases() ) );
        result.setSnapshots( cloneRepositoryPolicy( src.getSnapshots() ) );
        
        cloneRepositoryBaseFields( src, result );
        
        return result;
    }

    public static RepositoryPolicy cloneRepositoryPolicy( RepositoryPolicy src )
    {
        if ( src == null )
        {
            return null;
        }
        
        RepositoryPolicy result = new RepositoryPolicy();
        
        result.setChecksumPolicy( src.getChecksumPolicy() );
        result.setEnabled( src.getEnabled() );
        result.setUpdatePolicy( src.getUpdatePolicy() );
        
        return result;
    }

    public static MailingList cloneMailingList( MailingList src )
    {
        if ( src == null )
        {
            return null;
        }
        
        MailingList result = new MailingList();
        
        result.setArchive( src.getArchive() );
        result.setName( src.getName() );
        result.setOtherArchives( src.getOtherArchives() );
        result.setPost( src.getPost() );
        result.setSubscribe( src.getSubscribe() );
        result.setUnsubscribe( src.getUnsubscribe() );
        
        return result;
    }

    public static Model cloneModel( Model src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Model result = new Model();
        
        cloneModelBaseFields( src, result );

        result.setArtifactId( src.getArtifactId() );
        result.setBuild( cloneBuild( src.getBuild() ) );
        result.setCiManagement( cloneCiManagement( src.getCiManagement() ) );
        
        result.setContributors( cloneList( src.getContributors(), CONTRIBUTOR_CLONER ) );
        
        result.setDescription( src.getDescription() );
        result.setDevelopers( cloneList( src.getDevelopers(), DEVELOPER_CLONER ) );
        
        result.setGroupId( src.getGroupId() );
        result.setInceptionYear( src.getInceptionYear() );
        result.setIssueManagement( cloneIssueManagement( src.getIssueManagement() ) );
        result.setLicenses( cloneList( src.getLicenses(), LICENSE_CLONER ) );
        
        result.setMailingLists( cloneList( src.getMailingLists(), MAILING_LIST_CLONER ) );
        result.setModelVersion( src.getModelVersion() );
        result.setName( src.getName() );
        result.setOrganization( cloneOrganization( src.getOrganization() ) );
        result.setPackaging( src.getPackaging() );
        result.setParent( cloneParent( src.getParent() ) );
        
        result.setPrerequisites( clonePrerequisites( src.getPrerequisites() ) );
        result.setProfiles( cloneList( src.getProfiles(), PROFILE_CLONER ) );
        
        result.setScm( cloneScm( src.getScm() ) );
        result.setUrl( src.getUrl() );
        result.setVersion( src.getVersion() );

        result.setPomFile( src.getPomFile() );

        return result;
    }

    public static Scm cloneScm( Scm src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Scm result = new Scm();
        
        result.setConnection( src.getConnection() );
        result.setDeveloperConnection( src.getDeveloperConnection() );
        result.setTag( src.getTag() );
        result.setUrl( src.getUrl() );
        
        return result;
    }

    public static Prerequisites clonePrerequisites( Prerequisites src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Prerequisites result = new Prerequisites();
        
        result.setMaven( src.getMaven() );

        return result;
    }

    public static Organization cloneOrganization( Organization src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Organization result = new Organization();
        
        result.setName( src.getName() );
        result.setUrl( src.getUrl() );
        
        return result;
    }

    public static License cloneLicense( License src )
    {
        if ( src == null )
        {
            return null;
        }
        
        License result = new License();
        
        result.setComments( src.getComments() );
        result.setDistribution( src.getDistribution() );
        result.setName( src.getName() );
        result.setUrl( src.getUrl() );
        
        return result;
    }

    public static IssueManagement cloneIssueManagement( IssueManagement src )
    {
        if ( src == null )
        {
            return null;
        }
        
        IssueManagement result = new IssueManagement();
        
        result.setSystem( src.getSystem() );
        result.setUrl( src.getUrl() );
        
        return result;
    }

    public static DistributionManagement cloneDistributionManagement( DistributionManagement src )
    {
        if ( src == null )
        {
            return null;
        }
        
        DistributionManagement result = new DistributionManagement();
        
        result.setDownloadUrl( src.getDownloadUrl() );
        result.setRelocation( cloneRelocation( src.getRelocation() ) );
        result.setRepository( cloneDeploymentRepository( src.getRepository() ) );
        result.setSite( cloneSite( src.getSite() ) );
        result.setSnapshotRepository( cloneDeploymentRepository( src.getSnapshotRepository() ) );
        result.setStatus( src.getStatus() );
        
        return result;
    }

    public static Site cloneSite( Site src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Site result = new Site();
        
        result.setId( src.getId() );
        result.setName( src.getName() );
        result.setUrl( src.getUrl() );
        
        return result;
    }

    public static DeploymentRepository cloneDeploymentRepository( DeploymentRepository src )
    {
        if ( src == null )
        {
            return null;
        }
        
        DeploymentRepository result = new DeploymentRepository();
        
        result.setUniqueVersion( src.isUniqueVersion() );
        
        cloneRepositoryBaseFields( src, result );
        
        return result;
    }

    private static void cloneRepositoryBaseFields( RepositoryBase src, RepositoryBase result )
    {
        result.setId( src.getId() );
        result.setLayout( src.getLayout() );
        result.setName( src.getName() );
        result.setUrl( src.getUrl() );
    }

    public static Relocation cloneRelocation( Relocation src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Relocation result = new Relocation();
        
        result.setArtifactId( src.getArtifactId() );
        result.setGroupId( src.getGroupId() );
        result.setMessage( src.getMessage() );
        result.setVersion( src.getVersion() );
        
        return result;
    }

    public static DependencyManagement cloneDependencyManagement( DependencyManagement src )
    {
        if ( src == null )
        {
            return null;
        }
        
        DependencyManagement result = new DependencyManagement();
        result.setDependencies( cloneList( src.getDependencies(), DEPENDENCY_CLONER ) );
        
        return result;
    }

    private static <T> List<T> cloneList( List<T> src, ModelPartCloner<T> cloner )
    {
        List<T> result = null;
        if ( src != null )
        {
            result = new ArrayList<T>( src.size() );
            for ( Iterator<T> it = src.iterator(); it.hasNext(); )
            {
                result.add( cloner.cloneModelPart( it.next() ) );
            }
        }
        
        return result;
    }

    public static Contributor cloneContributor( Contributor src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Contributor result = new Contributor();
        cloneContributorFields( src, result );
        
        return result;
    }
    
    public static Developer cloneDeveloper( Developer src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Developer result = new Developer();
        
        result.setId( src.getId() );
        
        cloneContributorFields( src, result );
        
        return result;
    }
    
    private static void cloneContributorFields( Contributor src, Contributor result )
    {
        result.setEmail( src.getEmail() );
        result.setName( src.getName() );
        result.setOrganization( src.getOrganization() );
        result.setOrganizationUrl( src.getOrganizationUrl() );
        result.setProperties( cloneProperties( src.getProperties() ) );
        result.setRoles( cloneListOfStrings( src.getRoles() ) );
        result.setTimezone( src.getTimezone() );
        result.setUrl( src.getUrl() );
    }

    public static CiManagement cloneCiManagement( CiManagement src )
    {
        if ( src == null )
        {
            return null;
        }
        
        CiManagement result = new CiManagement();
        
        result.setNotifiers( cloneList( src.getNotifiers(), NOTIFIER_CLONER ) );
        result.setSystem( src.getSystem() );
        result.setUrl( src.getUrl() );
        
        return result;
    }

    public static Notifier cloneNotifier( Notifier src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Notifier result = new Notifier();
        result.setType( src.getType() );
        result.setAddress( src.getAddress() );
        result.setConfiguration( cloneProperties( src.getConfiguration() ) );
        result.setSendOnError( src.isSendOnError() );
        result.setSendOnFailure( src.isSendOnFailure() );
        result.setSendOnSuccess( src.isSendOnSuccess() );
        result.setSendOnWarning( src.isSendOnWarning() );
        
        return result;
    }

    public static Properties cloneProperties( Properties src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Properties result = new Properties();
        result.putAll( src );
        
        return result;
    }

    public static Build cloneBuild( Build src )
    {
        if ( src == null )
        {
            return null;
        }

        Build result = new Build();
        
        cloneBuildBaseFields( src, result );
        
        result.setExtensions( cloneList( src.getExtensions(), EXTENSION_CLONER ) );
        result.setOutputDirectory( src.getOutputDirectory() );
        
        result.setScriptSourceDirectory( src.getScriptSourceDirectory() );
        result.setSourceDirectory( src.getSourceDirectory() );
        result.setTestOutputDirectory( src.getTestOutputDirectory() );
        result.setTestSourceDirectory( src.getTestSourceDirectory() );
        
        return result;
    }

    public static void cloneBuildBaseFields( BuildBase src, BuildBase result )
    {
        result.setDefaultGoal( src.getDefaultGoal() );
        result.setDirectory( src.getDirectory() );
        
        result.setFilters( cloneListOfStrings( src.getFilters() ) );
        result.setFinalName( src.getFinalName() );
        
        result.setPluginManagement( clonePluginManagement( src.getPluginManagement() ) );
        result.setPlugins( cloneList( src.getPlugins(), PLUGIN_CLONER ) );
 
        result.setResources( cloneList( src.getResources(), RESOURCE_CLONER ) );
        
        result.setTestResources( cloneList( src.getTestResources(), RESOURCE_CLONER ) );
    }

    public static PluginManagement clonePluginManagement( PluginManagement src )
    {
        PluginManagement pMgmt = null;
        if ( src != null )
        {
            pMgmt = new PluginManagement();
            pMgmt.setPlugins( cloneList( src.getPlugins(), PLUGIN_CLONER ) );
        }
        
        return pMgmt;
    }

    public static Resource cloneResource( Resource src )
    {
        Resource result = null;
        if ( src != null )
        {
            result = new Resource();
            
            result.setDirectory( src.getDirectory() );
            result.setExcludes( cloneListOfStrings( src.getExcludes() ) );
            result.setFiltering( src.getFiltering() );
            result.setIncludes( cloneListOfStrings( src.getIncludes() ) );
            result.setMergeId( src.getMergeId() );
            result.setTargetPath( src.getTargetPath() );
        }
        
        return result;
    }

    public static Plugin clonePlugin( Plugin src )
    {
        Plugin result = null;
        if ( src != null )
        {
            result = new Plugin();
            result.setArtifactId( src.getArtifactId() );
            
            result.setConfiguration( cloneConfiguration( src.getConfiguration() ) );
            
            result.setDependencies( cloneList( src.getDependencies(), DEPENDENCY_CLONER ) );
            result.setExecutions( cloneList( src.getExecutions(), PLUGIN_EXECUTION_CLONER ) );
            
            result.setExtensions( src.isExtensions() );
            result.setGroupId( src.getGroupId() );
            result.setInherited( src.getInherited() );
            result.setVersion( src.getVersion() );
        }
        
        return result;
    }

    public static PluginExecution clonePluginExecution( PluginExecution src )
    {
        PluginExecution result = null;
        
        if ( src != null )
        {
            result = new PluginExecution();
            
            result.setId( src.getId() );
            result.setGoals( cloneListOfStrings( src.getGoals() ) );
            result.setConfiguration( cloneConfiguration( src.getConfiguration() ) );
            result.setInherited( src.getInherited() );
            result.setPhase( src.getPhase() );
        }
        
        return result;
    }

    public static Object cloneConfiguration( Object configuration )
    {
        if ( configuration == null )
        {
            return null;
        }
        
        return new Xpp3Dom( (Xpp3Dom) configuration );
    }

    public static Dependency cloneDependency( Dependency src )
    {
        Dependency result = null;
        if ( src != null )
        {
            result = new Dependency();
            
            result.setArtifactId( src.getArtifactId() );
            result.setClassifier( src.getClassifier() );
            result.setExclusions( cloneList( src.getExclusions(), DEPENDENCY_EXCLUSION_CLONER ) );
            result.setGroupId( src.getGroupId() );
            result.setOptional( src.isOptional() );
            result.setScope( src.getScope() );
            result.setSystemPath( src.getSystemPath() );
            result.setType( src.getType() );
            result.setVersion( src.getVersion() );
        }
        
        return result;
    }

    public static Exclusion cloneExclusion( Exclusion src )
    {
        Exclusion result = null;
        if ( src != null )
        {
            result = new Exclusion();
            result.setArtifactId( src.getArtifactId() );
            result.setGroupId( src.getGroupId() );
        }
        
        return result;
    }

    public static List<String> cloneListOfStrings( List<String> src )
    {
        List<String> result = null;
        if ( src != null )
        {
            result = new ArrayList<String>( src.size() );
            for ( String item : src )
            {
                result.add( item );
            }
        }

        return result;
    }

    public static Extension cloneExtension( Extension src )
    {
        Extension rExt = new Extension();
        rExt.setArtifactId( src.getArtifactId() );
        rExt.setGroupId( src.getGroupId() );
        rExt.setVersion( src.getVersion() );
        
        return rExt;
    }

    public static Exclusion cloneDependencyExclusion( Exclusion src )
    {
        if ( src == null )
        {
            return null;
        }
        
        Exclusion result = new Exclusion();

        result.setArtifactId( src.getArtifactId() );
        result.setGroupId( src.getGroupId() );
        
        return result;
    }

    public static Parent cloneParent( Parent src )
    {
        if ( src == null )
        {
            return null;
        }

        Parent result = new Parent();
        result.setArtifactId( src.getArtifactId() );
        result.setGroupId( src.getGroupId() );
        result.setRelativePath( src.getRelativePath() );
        result.setVersion( src.getVersion() );
        
        return result;
    }
    
    public static interface ModelPartCloner<T>
    {
        T cloneModelPart( T src );
    }

}
