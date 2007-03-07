package org.apache.maven.lifecycle.parser;

import org.apache.maven.lifecycle.BuildBinding;
import org.apache.maven.lifecycle.CleanBinding;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleBindings;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.MojoBinding;
import org.apache.maven.lifecycle.Phase;
import org.apache.maven.lifecycle.SiteBinding;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public final class LegacyLifecycleMappingParser
{

    public static LifecycleBindings parseDefaultMappings( List lifecycles )
        throws LifecycleSpecificationException
    {
        LifecycleBindings bindings = new LifecycleBindings();
        
        bindings.setOrigin( "Maven core" );
        bindings.setPackaging( "unmatched" );
        
        for ( Iterator it = lifecycles.iterator(); it.hasNext(); )
        {
            Lifecycle lifecycle = (Lifecycle) it.next();
            
            if ( "clean".equals( lifecycle.getId() ) )
            {
                bindings.setCleanBinding( parseCleanBindings( lifecycle.getDefaultPhases() ) );
            }
            else if ( "site".equals( lifecycle.getId() ) )
            {
                bindings.setSiteBinding( parseSiteBindings( lifecycle.getDefaultPhases() ) );
            }
            else if ( "default".equals( lifecycle.getId() ) )
            {
                bindings.setBuildBinding( parseBuildBindings( lifecycle.getDefaultPhases() ) );
            }
            else
            {
                throw new LifecycleSpecificationException( "Unrecognized lifecycle: " + lifecycle.getId() );
            }
        }

        return bindings;
    }

    public static LifecycleBindings parseMappings( LifecycleMapping mapping, String packaging )
        throws LifecycleSpecificationException
    {
        LifecycleBindings bindings = new LifecycleBindings();
        bindings.setOrigin( "Maven core" );
        bindings.setPackaging( packaging );

        bindings.setCleanBinding( parseCleanBindings( mapping.getPhases( "clean" ) ) );
        bindings.setBuildBinding( parseBuildBindings( mapping.getPhases( "default" ) ) );
        bindings.setSiteBinding( parseSiteBindings( mapping.getPhases( "site" ) ) );

        return bindings;
    }

    public static MojoBinding parseMojoBinding( String bindingSpec, boolean allowPrefixReference )
        throws LifecycleSpecificationException
    {
        StringTokenizer tok = new StringTokenizer( bindingSpec, ":" );
        int numTokens = tok.countTokens();

        MojoBinding binding = null;

        if ( numTokens == 2 )
        {
            if ( !allowPrefixReference )
            {
                String msg = "Mapped-prefix lookup of mojos are only supported from direct invocation. "
                    + "Please use specification of the form groupId:artifactId[:version]:goal instead.";

                throw new LifecycleSpecificationException( msg );
            }

            binding = new PrefixedMojoBinding();

            ( (PrefixedMojoBinding) binding ).setPrefix( tok.nextToken() );
            binding.setGoal( tok.nextToken() );

        }
        else if ( numTokens == 3 || numTokens == 4 )
        {
            binding = new MojoBinding();

            binding.setGroupId( tok.nextToken() );
            binding.setArtifactId( tok.nextToken() );

            if ( numTokens == 4 )
            {
                binding.setVersion( tok.nextToken() );
            }

            binding.setGoal( tok.nextToken() );
        }
        else
        {
            String message = "Invalid task '" + bindingSpec + "': you must specify a valid lifecycle phase, or"
                + " a goal in the format plugin:goal or pluginGroupId:pluginArtifactId:pluginVersion:goal";

            throw new LifecycleSpecificationException( message );
        }

        return binding;
    }

    private static BuildBinding parseBuildBindings( Map phases )
        throws LifecycleSpecificationException
    {
        BuildBinding binding = new BuildBinding();

        binding.setValidate( parsePhaseBindings( (String) phases.get( "validate" ) ) );
        binding.setInitialize( parsePhaseBindings( (String) phases.get( "initialize" ) ) );
        binding.setGenerateSources( parsePhaseBindings( (String) phases.get( "generate-sources" ) ) );
        binding.setProcessSources( parsePhaseBindings( (String) phases.get( "process-sources" ) ) );
        binding.setGenerateResources( parsePhaseBindings( (String) phases.get( "generate-resources" ) ) );
        binding.setProcessResources( parsePhaseBindings( (String) phases.get( "process-resources" ) ) );
        binding.setCompile( parsePhaseBindings( (String) phases.get( "compile" ) ) );
        binding.setProcessClasses( parsePhaseBindings( (String) phases.get( "process-classes" ) ) );
        binding.setGenerateTestSources( parsePhaseBindings( (String) phases.get( "generate-test-sources" ) ) );
        binding.setProcessTestSources( parsePhaseBindings( (String) phases.get( "process-test-sources" ) ) );
        binding.setGenerateTestResources( parsePhaseBindings( (String) phases.get( "generate-test-resources" ) ) );
        binding.setProcessTestResources( parsePhaseBindings( (String) phases.get( "process-test-resources" ) ) );
        binding.setTestCompile( parsePhaseBindings( (String) phases.get( "test-compile" ) ) );
        binding.setProcessTestClasses( parsePhaseBindings( (String) phases.get( "process-test-classes" ) ) );
        binding.setTest( parsePhaseBindings( (String) phases.get( "test" ) ) );
        binding.setPreparePackage( parsePhaseBindings( (String) phases.get( "prepare-package" ) ) );
        binding.setCreatePackage( parsePhaseBindings( (String) phases.get( "package" ) ) );
        binding.setPreIntegrationTest( parsePhaseBindings( (String) phases.get( "pre-integration-test" ) ) );
        binding.setIntegrationTest( parsePhaseBindings( (String) phases.get( "integration-test" ) ) );
        binding.setPostIntegrationTest( parsePhaseBindings( (String) phases.get( "post-integration-test" ) ) );
        binding.setVerify( parsePhaseBindings( (String) phases.get( "verify" ) ) );
        binding.setInstall( parsePhaseBindings( (String) phases.get( "install" ) ) );
        binding.setDeploy( parsePhaseBindings( (String) phases.get( "deploy" ) ) );

        return binding;
    }

    private static CleanBinding parseCleanBindings( Map phaseMappings )
        throws LifecycleSpecificationException
    {
        CleanBinding binding = new CleanBinding();

        binding.setPreClean( parsePhaseBindings( (String) phaseMappings.get( "pre-clean" ) ) );
        binding.setClean( parsePhaseBindings( (String) phaseMappings.get( "clean" ) ) );
        binding.setPostClean( parsePhaseBindings( (String) phaseMappings.get( "post-clean" ) ) );

        return binding;
    }

    private static Phase parsePhaseBindings( String bindingList )
        throws LifecycleSpecificationException
    {
        Phase phase = new Phase();

        if ( bindingList != null )
        {
            for ( StringTokenizer tok = new StringTokenizer( bindingList, "," ); tok.hasMoreTokens(); )
            {
                String rawBinding = tok.nextToken().trim();

                MojoBinding binding = parseMojoBinding( rawBinding, false );

                if ( binding == null )
                {
                    continue;
                }

                phase.addBinding( binding );
            }
        }

        return phase;
    }

    private static SiteBinding parseSiteBindings( Map phases )
        throws LifecycleSpecificationException
    {
        SiteBinding binding = new SiteBinding();

        binding.setPreSite( parsePhaseBindings( (String) phases.get( "pre-site" ) ) );
        binding.setSite( parsePhaseBindings( (String) phases.get( "site" ) ) );
        binding.setPostSite( parsePhaseBindings( (String) phases.get( "post-site" ) ) );

        return binding;
    }

    private LegacyLifecycleMappingParser()
    {
    }

}
