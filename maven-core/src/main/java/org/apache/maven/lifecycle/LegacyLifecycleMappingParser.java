package org.apache.maven.lifecycle;

import org.apache.maven.lifecycle.binding.LegacyLifecycle;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.model.BuildBinding;
import org.apache.maven.lifecycle.model.CleanBinding;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.model.Phase;
import org.apache.maven.lifecycle.model.SiteBinding;

import java.util.Collections;
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
            LegacyLifecycle lifecycle = (LegacyLifecycle) it.next();
            
            if ( "clean".equals( lifecycle.getId() ) )
            {
                bindings.setCleanBinding( parseCleanBindings( lifecycle.getDefaultPhases(), Collections.EMPTY_LIST ) );
            }
            else if ( "site".equals( lifecycle.getId() ) )
            {
                bindings.setSiteBinding( parseSiteBindings( lifecycle.getDefaultPhases(), Collections.EMPTY_LIST ) );
            }
            else if ( "default".equals( lifecycle.getId() ) )
            {
                bindings.setBuildBinding( parseBuildBindings( lifecycle.getDefaultPhases(), Collections.EMPTY_LIST ) );
            }
            else
            {
                throw new LifecycleSpecificationException( "Unrecognized lifecycle: " + lifecycle.getId() );
            }
        }
        
        LifecycleUtils.setOrigin( bindings, "Maven default lifecycle" );

        return bindings;
    }

    public static LifecycleBindings parseMappings( LifecycleMapping mapping, String packaging )
        throws LifecycleSpecificationException
    {
        LifecycleBindings bindings = new LifecycleBindings();
        bindings.setPackaging( packaging );
        
        bindings.setCleanBinding( parseCleanBindings( mapping.getPhases( "clean" ), mapping.getOptionalMojos( "clean" ) ) );
        bindings.setBuildBinding( parseBuildBindings( mapping.getPhases( "default" ), mapping.getOptionalMojos( "default" ) ) );
        bindings.setSiteBinding( parseSiteBindings( mapping.getPhases( "site" ), mapping.getOptionalMojos( "site" ) ) );
        
        LifecycleUtils.setOrigin( bindings, "Maven " + packaging + " lifecycle" );

        return bindings;
    }
    
    private static BuildBinding parseBuildBindings( Map phases, List optionalKeys )
        throws LifecycleSpecificationException
    {
        BuildBinding binding = new BuildBinding();
        
        if ( phases != null )
        {
            binding.setValidate( parsePhaseBindings( (String) phases.get( "validate" ), optionalKeys ) );
            binding.setInitialize( parsePhaseBindings( (String) phases.get( "initialize" ), optionalKeys ) );
            binding.setGenerateSources( parsePhaseBindings( (String) phases.get( "generate-sources" ), optionalKeys ) );
            binding.setProcessSources( parsePhaseBindings( (String) phases.get( "process-sources" ), optionalKeys ) );
            binding.setGenerateResources( parsePhaseBindings( (String) phases.get( "generate-resources" ), optionalKeys ) );
            binding.setProcessResources( parsePhaseBindings( (String) phases.get( "process-resources" ), optionalKeys ) );
            binding.setCompile( parsePhaseBindings( (String) phases.get( "compile" ), optionalKeys ) );
            binding.setProcessClasses( parsePhaseBindings( (String) phases.get( "process-classes" ), optionalKeys ) );
            binding.setGenerateTestSources( parsePhaseBindings( (String) phases.get( "generate-test-sources" ), optionalKeys ) );
            binding.setProcessTestSources( parsePhaseBindings( (String) phases.get( "process-test-sources" ), optionalKeys ) );
            binding.setGenerateTestResources( parsePhaseBindings( (String) phases.get( "generate-test-resources" ), optionalKeys ) );
            binding.setProcessTestResources( parsePhaseBindings( (String) phases.get( "process-test-resources" ), optionalKeys ) );
            binding.setTestCompile( parsePhaseBindings( (String) phases.get( "test-compile" ), optionalKeys ) );
            binding.setProcessTestClasses( parsePhaseBindings( (String) phases.get( "process-test-classes" ), optionalKeys ) );
            binding.setTest( parsePhaseBindings( (String) phases.get( "test" ), optionalKeys ) );
            binding.setPreparePackage( parsePhaseBindings( (String) phases.get( "prepare-package" ), optionalKeys ) );
            binding.setCreatePackage( parsePhaseBindings( (String) phases.get( "package" ), optionalKeys ) );
            binding.setPreIntegrationTest( parsePhaseBindings( (String) phases.get( "pre-integration-test" ), optionalKeys ) );
            binding.setIntegrationTest( parsePhaseBindings( (String) phases.get( "integration-test" ), optionalKeys ) );
            binding.setPostIntegrationTest( parsePhaseBindings( (String) phases.get( "post-integration-test" ), optionalKeys ) );
            binding.setVerify( parsePhaseBindings( (String) phases.get( "verify" ), optionalKeys ) );
            binding.setInstall( parsePhaseBindings( (String) phases.get( "install" ), optionalKeys ) );
            binding.setDeploy( parsePhaseBindings( (String) phases.get( "deploy" ), optionalKeys ) );
        }

        return binding;
    }

    private static CleanBinding parseCleanBindings( Map phaseMappings, List optionalKeys )
        throws LifecycleSpecificationException
    {
        CleanBinding binding = new CleanBinding();

        if ( phaseMappings != null )
        {
            binding.setPreClean( parsePhaseBindings( (String) phaseMappings.get( "pre-clean" ), optionalKeys ) );
            binding.setClean( parsePhaseBindings( (String) phaseMappings.get( "clean" ), optionalKeys ) );
            binding.setPostClean( parsePhaseBindings( (String) phaseMappings.get( "post-clean" ), optionalKeys ) );
        }

        return binding;
    }

    private static Phase parsePhaseBindings( String bindingList, List optionalKeys )
        throws LifecycleSpecificationException
    {
        Phase phase = new Phase();

        if ( bindingList != null )
        {
            for ( StringTokenizer tok = new StringTokenizer( bindingList, "," ); tok.hasMoreTokens(); )
            {
                String rawBinding = tok.nextToken().trim();

                MojoBinding binding = MojoBindingUtils.parseMojoBinding( rawBinding, false );
                if ( optionalKeys.contains( rawBinding ) )
                {
                    binding.setOptional( true );
                }

                if ( binding == null )
                {
                    continue;
                }

                phase.addBinding( binding );
            }
        }

        return phase;
    }

    private static SiteBinding parseSiteBindings( Map phases, List optionalKeys )
        throws LifecycleSpecificationException
    {
        SiteBinding binding = new SiteBinding();

        if ( phases != null )
        {
            binding.setPreSite( parsePhaseBindings( (String) phases.get( "pre-site" ), optionalKeys ) );
            binding.setSite( parsePhaseBindings( (String) phases.get( "site" ), optionalKeys ) );
            binding.setPostSite( parsePhaseBindings( (String) phases.get( "post-site" ), optionalKeys ) );
            binding.setSiteDeploy( parsePhaseBindings( (String) phases.get( "site-deploy" ), optionalKeys ) );
        }

        return binding;
    }

    private LegacyLifecycleMappingParser()
    {
    }

}
