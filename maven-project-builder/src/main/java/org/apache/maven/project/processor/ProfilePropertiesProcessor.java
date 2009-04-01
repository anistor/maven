package org.apache.maven.project.processor;

import java.util.Properties;

import org.apache.maven.model.Model;

public class ProfilePropertiesProcessor    
    extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target, c = (Model) child, p = (Model) parent;

        Properties properties = new Properties();
                        
        if ( c.getProperties() != null )
        {
            properties.putAll( c.getProperties() );
        }
        
        if ( p != null && p.getProperties() != null )
        {
            properties.putAll( p.getProperties() );
        }
        
        if ( !properties.isEmpty() )
        {
            if(t.getProperties().isEmpty())
            {
                t.setProperties( properties );   
            }
            else
            {
            	add(properties, t.getProperties());
                //t.getProperties().putAll( properties );
            }       
        }
    }
    
    /**
     * Add source properties to target if the property does not exist: parent over child
     * 
     * @param source
     * @param target
     */
    private static void add(Properties source, Properties target)
    {
    	for(Object key : source.keySet())
    	{
    		if(!target.containsKey(key))
    		{  			
    			target.put(key, source.get(key));
    		}
    	}
    }

}
