package org.apache.maven.lifecycle.plan;

import java.util.List;

public interface ModifiablePlanElement
{

    void addModifier( LifecyclePlanModifier planModifier );

    List getModifiers();
    
}
