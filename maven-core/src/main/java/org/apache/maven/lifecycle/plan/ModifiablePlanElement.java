package org.apache.maven.lifecycle.plan;


public interface ModifiablePlanElement
{

    void addModifier( BuildPlanModifier planModifier );

    boolean hasModifiers();
    
}
