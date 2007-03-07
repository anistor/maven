package org.apache.maven.lifecycle;

public interface LifecycleBindingLoader
{
    
    String ROLE = LifecycleBindingLoader.class.getName();

    LifecycleBindings getBindings()
        throws LifecycleLoaderException, LifecycleSpecificationException;

}
