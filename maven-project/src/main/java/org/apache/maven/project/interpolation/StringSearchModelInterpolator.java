package org.apache.maven.project.interpolation;

import org.apache.maven.project.path.PathTranslator;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;

public class StringSearchModelInterpolator
    extends AbstractStringBasedModelInterpolator
{

    public StringSearchModelInterpolator()
    {
    }

    public StringSearchModelInterpolator( PathTranslator pathTranslator )
    {
        super( pathTranslator );
    }

    protected Interpolator createInterpolator()
    {
        return new StringSearchInterpolator();
    }

}
