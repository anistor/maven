package org.apache.maven.lifecycle;

import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.model.BuildBinding;
import org.apache.maven.lifecycle.model.CleanBinding;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.model.Phase;
import org.apache.maven.lifecycle.model.SiteBinding;

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
        
        LifecycleUtils.setOrigin( bindings, "Maven core" );

        return bindings;
    }

    public static LifecycleBindings parseMappings( LifecycleMapping mapping, String packaging )
        throws LifecycleSpecificationException
    {
        LifecycleBindings bindings = new LifecycleBindings();
        bindings.setPackaging( packaging );

        bindings.setCleanBinding( parseCleanBindings( mapping.getPhases( "clean" ) ) );
        bindings.setBuildBinding( parseBuildBindings( mapping.getPhases( "default" ) ) );
        bindings.setSiteBinding( parseSiteBindings( mapping.getPhases( "site" ) ) );
        
        LifecycleUtils.setOrigin( bindings, "Maven core" );

        return bindings;
    }
    
    private static BuildBinding parseBuildBindings( Map phases )
        throws LifecycleSpecificationException
    {
        BuildBinding binding = new BuildBinding();
        
        if ( phases != null )
        {
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
        }

        return binding;
    }

    private static CleanBinding parseCleanBindings( Map phaseMappings )
        throws LifecycleSpecificationException
    {
        CleanBinding binding = new CleanBinding();

        if ( phaseMappings != null )
        {
            binding.setPreClean( parsePhaseBindings( (String) phaseMappings.get( "pre-clean" ) ) );
            binding.setClean( parsePhaseBindings( (String) phaseMappings.get( "clean" ) ) );
            binding.setPostClean( parsePhaseBindings( (String) phaseMappings.get( "post-clean" ) ) );
        }

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

                MojoBinding binding = MojoBindingParser.parseMojoBinding( rawBinding, false );

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

        if ( phases != null )
        {
            binding.setPreSite( parsePhaseBindings( (String) phases.get( "pre-site" ) ) );
            binding.setSite( parsePhaseBindings( (String) phases.get( "site" ) ) );
            binding.setPostSite( parsePhaseBindings( (String) phases.get( "post-site" ) ) );
            binding.setSiteDeploy( parsePhaseBindings( (String) phases.get( "site-deploy" ) ) );
        }

        return binding;
    }

    private LegacyLifecycleMappingParser()
    {
    }

}
