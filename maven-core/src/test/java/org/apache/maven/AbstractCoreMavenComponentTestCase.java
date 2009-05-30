package org.apache.maven;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.FileUtils;

public abstract class AbstractCoreMavenComponentTestCase
    extends PlexusTestCase
{
    @Requirement
    protected RepositorySystem repositorySystem;

    @Requirement
    protected org.apache.maven.project.ProjectBuilder projectBuilder;

    protected void setUp()
        throws Exception
    {
        repositorySystem = lookup( RepositorySystem.class );
        projectBuilder = lookup( org.apache.maven.project.ProjectBuilder.class );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        repositorySystem = null;
        projectBuilder = null;
        super.tearDown();
    }

    abstract protected String getProjectsDirectory();

    protected File getProject( String name )
        throws Exception
    {
        File source = new File( new File( getBasedir(), getProjectsDirectory() ), name );
        File target = new File( new File( getBasedir(), "target" ), name );
        if ( !target.exists() )
        {
            FileUtils.copyDirectoryStructure( source, target );
        }
        return new File( target, "pom.xml" );
    }

    /**
     * We need to customize the standard Plexus container with the plugin discovery listener which
     * is what looks for the META-INF/maven/plugin.xml resources that enter the system when a Maven
     * plugin is loaded.
     * 
     * We also need to customize the Plexus container with a standard plugin discovery listener
     * which is the MavenPluginCollector. When a Maven plugin is discovered the MavenPluginCollector
     * collects the plugin descriptors which are found.
     */
    protected void customizeContainerConfiguration( ContainerConfiguration containerConfiguration )
    {
        containerConfiguration.addComponentDiscoverer( PluginManager.class );
        containerConfiguration.addComponentDiscoveryListener( PluginManager.class );
    }

    protected MavenExecutionRequest createMavenExecutionRequest( File pom )
        throws Exception
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setPom( pom ).setProjectPresent( true )
            .setPluginGroups( Arrays.asList( new String[] { "org.apache.maven.plugins" } ) )
            .setLocalRepository( getLocalRepository() )
            .setRemoteRepositories( getRemoteRepositories() )
            .setGoals( Arrays.asList( new String[] { "package" } ) )
            .setProperties( new Properties() );

        return request;
    }

    // layer the creation of a project builder configuration with a request, but this will need to be
    // a Maven subclass because we don't want to couple maven to the project builder which we need to
    // separate.
    protected MavenSession createMavenSession( File pom )
        throws Exception        
    {
        return createMavenSession( pom, new Properties() );
    }
    
    protected MavenSession createMavenSession( File pom, Properties executionProperties )
        throws Exception
    {
        MavenExecutionRequest request = createMavenExecutionRequest( pom );

        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest()
            .setLocalRepository( request.getLocalRepository() )
            .setRemoteRepositories( request.getRemoteRepositories() )
            .setExecutionProperties( executionProperties );

        MavenProject project = null;

        if ( pom != null )
        {
            project = projectBuilder.build( pom, configuration );
        }
        else
        {
            project = createStubMavenProject();
        }

        MavenSession session = new MavenSession( getContainer(), request, new DefaultMavenExecutionResult(), project );

        return session;
    }

    protected MavenProject createStubMavenProject()
    {
        Model model = new Model();
        model.setGroupId( "org.apache.maven.test" );
        model.setArtifactId( "maven-test" );
        model.setVersion( "1.0" );
        return new MavenProject( model );
    }
    
    protected List<ArtifactRepository> getRemoteRepositories() 
        throws InvalidRepositoryException
    {
        return Arrays.asList( repositorySystem.createDefaultRemoteRepository() );
    }
        
    protected ArtifactRepository getLocalRepository() 
        throws InvalidRepositoryException
    {        
        return repositorySystem.createDefaultLocalRepository();        
    }
    
    protected class ProjectBuilder
    {
        private MavenProject project;
        
        public ProjectBuilder( String groupId, String artifactId, String version )
        {
            Model model = new Model();
            model.setModelVersion( "4.0.0" );
            model.setGroupId( groupId );
            model.setArtifactId( artifactId );
            model.setVersion( version );  
            model.setBuild(  new Build() );
            project = new MavenProject( model );            
        }
        
        public ProjectBuilder setGroupId( String groupId )
        {
            project.setGroupId( groupId );
            return this;
        }

        public ProjectBuilder setArtifactId( String artifactId )
        {
            project.setArtifactId( artifactId );
            return this;
        }
        
        public ProjectBuilder setVersion( String version )
        {
            project.setVersion( version );
            return this;
        }
        
        // Dependencies
        //
        public ProjectBuilder addDependency( String groupId, String artifactId, String version, String scope )
        {
            return addDependency( groupId, artifactId, version, scope, (Exclusion)null );
        }
        
        public ProjectBuilder addDependency( String groupId, String artifactId, String version, String scope, Exclusion exclusion )
        {
            return addDependency( groupId, artifactId, version, scope, null, exclusion );            
        }

        public ProjectBuilder addDependency( String groupId, String artifactId, String version, String scope, String systemPath )
        {
            return addDependency( groupId, artifactId, version, scope, systemPath, null );         
        }
        
        public ProjectBuilder addDependency( String groupId, String artifactId, String version, String scope, String systemPath, Exclusion exclusion )
        {
            Dependency d = new Dependency();
            d.setGroupId( groupId );
            d.setArtifactId( artifactId );
            d.setVersion( version );
            d.setScope( scope );
            
            if ( systemPath != null && scope.equals(  Artifact.SCOPE_SYSTEM ) )
            {
                d.setSystemPath( systemPath );
            }
            
            if ( exclusion != null )
            {
                d.addExclusion( exclusion );
            }
            
            project.getDependencies().add( d );
            
            return this;
        }
        
        // Plugins
        //
        public ProjectBuilder addPlugin( Plugin plugin )
        {
            project.getBuildPlugins().add( plugin );            
            return this;
        }
        
        public MavenProject get()
        {
            return project;
        }        
    }    
    
    protected class PluginBuilder
    {
        private Plugin plugin;
        
        public PluginBuilder( String groupId, String artifactId, String version )
        {
            plugin = new Plugin();
            plugin.setGroupId( groupId );
            plugin.setArtifactId( artifactId );
            plugin.setVersion( version );                         
        }
                
        // Dependencies
        //
        public PluginBuilder addDependency( String groupId, String artifactId, String version, String scope, Exclusion exclusion )
        {
            return addDependency( groupId, artifactId, version, scope, exclusion );            
        }

        public PluginBuilder addDependency( String groupId, String artifactId, String version, String scope, String systemPath )
        {
            return addDependency( groupId, artifactId, version, scope, systemPath, null );         
        }
        
        public PluginBuilder addDependency( String groupId, String artifactId, String version, String scope, String systemPath, Exclusion exclusion )
        {
            Dependency d = new Dependency();
            d.setGroupId( groupId );
            d.setArtifactId( artifactId );
            d.setVersion( version );
            d.setScope( scope );
            
            if ( systemPath != null && scope.equals(  Artifact.SCOPE_SYSTEM ) )
            {
                d.setSystemPath( systemPath );
            }
            
            if ( exclusion != null )
            {
                d.addExclusion( exclusion );
            }
            
            plugin.getDependencies().add( d );
            
            return this;
        }
                
        public Plugin get()
        {
            return plugin;
        }        
    }        
}
