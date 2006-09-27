package org.apache.maven;

/**
 * @author Milos Kleint
 * @author Jason van Zyl
 */
public interface Toolchain
{
    String getId();

    String getName();

    String getVendor();

    String getSpecification();

    Map getProperties();

    Map getSystemProperties();

    //JAVA
    /**
     * Returns a ClassPath, which represents bootstrap libraries for the
     * runtime environment. The Bootstrap libraries include libraries in
     * JRE's extension directory, if there are any.
     *
     * @return ClassPath representing the bootstrap libs
     */
    List getBootstrapLibraries();

    //JAVA
    /**
     * Returns libraries recognized by default by the platform. Usually
     * it corresponds to contents of CLASSPATH environment variable.
     */
    List getStandardLibraries();

    /**
     * Gets the platform tool executable.
     *
     * @param toolName the tool platform independent tool name.
     * @return file representing the tool executable, or null if the tool can not be found
     */
    String findTool( String toolName );

    //JAVA
    /**
     * Returns the locations of the source of platform
     * or empty collection when the location is not set or is invalid
     *
     * @return ClassPath never returns null
     */
    List getSourceFolders();

    //JAVA
    /**
     * Returns the locations of the Javadoc for this platform
     * or empty collection if the location is not set or invalid
     *
     * @return List&lt;URL&gt never returns null
     */
    List getJavadocFolders();
}
