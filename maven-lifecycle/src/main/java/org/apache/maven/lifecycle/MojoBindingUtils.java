package org.apache.maven.lifecycle;

import org.apache.maven.lifecycle.model.MojoBinding;

import java.util.StringTokenizer;

public final class MojoBindingUtils
{

    private MojoBindingUtils()
    {
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

    public static String toString( MojoBinding mojoBinding )
    {
        if ( mojoBinding instanceof PrefixedMojoBinding )
        {
            return ((PrefixedMojoBinding) mojoBinding).getPrefix() + ":" + mojoBinding.getGoal();
        }
        else
        {
            return mojoBinding.getGroupId() + ":" + mojoBinding.getArtifactId() + ":"
                + ( mojoBinding.getVersion() == null ? "" : mojoBinding.getVersion() + ":" ) + mojoBinding.getGoal();
        }
    }

}
