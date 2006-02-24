package org.apache.maven.cli;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.maven.Maven;
import org.apache.maven.SettingsConfigurationException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.monitor.event.DefaultEventDispatcher;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.RuntimeInfo;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * @author jason van zyl
 * @version $Id$
 * @noinspection UseOfSystemOutOrSystemErr,ACCESS_STATIC_VIA_INSTANCE
 */
public class MavenCli
{
    private static Embedder embedder;

    /**
     * @noinspection ConfusingMainMethod
     */
    public static int main( String[] args, ClassWorld classWorld )
    {
        // ----------------------------------------------------------------------
        // Setup the command line parser
        // ----------------------------------------------------------------------

        CLIManager cliManager = new CLIManager();

        CommandLine commandLine;
        try
        {
            commandLine = cliManager.parse( args );
        }
        catch ( ParseException e )
        {
            System.err.println( "Unable to parse command line options: " + e.getMessage() );
            cliManager.displayHelp();
            return 1;
        }

        // TODO: maybe classworlds could handle this requirement...
        if ( System.getProperty( "java.class.version", "44.0" ).compareTo( "48.0" ) < 0 )
        {
            System.err.println( "Sorry, but JDK 1.4 or above is required to execute Maven" );
            System.err.println(
                "You appear to be using Java version: " + System.getProperty( "java.version", "<unknown>" ) );

            return 1;
        }

        boolean debug = commandLine.hasOption( CLIManager.DEBUG );

        boolean showErrors = debug || commandLine.hasOption( CLIManager.ERRORS );

        if ( showErrors )
        {
            System.out.println( "+ Error stacktraces are turned on." );
        }

        // ----------------------------------------------------------------------
        // Process particular command line options
        // ----------------------------------------------------------------------

        if ( commandLine.hasOption( CLIManager.HELP ) )
        {
            cliManager.displayHelp();
            return 0;
        }

        if ( commandLine.hasOption( CLIManager.VERSION ) )
        {
            showVersion();

            return 0;
        }
        else if ( debug )
        {
            showVersion();
        }

        // ----------------------------------------------------------------------
        // Now that we have everything that we need we will fire up plexus and
        // bring the maven component to life for use.
        // ----------------------------------------------------------------------

        embedder = new Embedder();

        try
        {
            embedder.start( classWorld );
        }
        catch ( PlexusContainerException e )
        {
            showFatalError( "Unable to start the embedded plexus container", e, showErrors );

            return 1;
        }

        // ----------------------------------------------------------------------
        // The execution properties need to be created before the settings
        // are constructed.
        // ----------------------------------------------------------------------

        Properties executionProperties = getExecutionProperties( commandLine );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        String userSettingsPath = null;

        if ( commandLine.hasOption( CLIManager.ALTERNATE_USER_SETTINGS ) )
        {
            userSettingsPath = commandLine.getOptionValue( CLIManager.ALTERNATE_USER_SETTINGS );
        }

        boolean interactive = true;

        if ( commandLine.hasOption( CLIManager.BATCH_MODE ) )
        {
            interactive = false;
        }

        boolean usePluginRegistry = true;

        if ( commandLine.hasOption( CLIManager.SUPPRESS_PLUGIN_REGISTRY ) )
        {
            usePluginRegistry = false;
        }

        Boolean pluginUpdateOverride = Boolean.FALSE;

        if ( commandLine.hasOption( CLIManager.FORCE_PLUGIN_UPDATES ) ||
            commandLine.hasOption( CLIManager.FORCE_PLUGIN_UPDATES2 ) )
        {
            pluginUpdateOverride = Boolean.TRUE;
        }
        else if ( commandLine.hasOption( CLIManager.SUPPRESS_PLUGIN_UPDATES ) )
        {
            pluginUpdateOverride = Boolean.FALSE;
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        try
        {
            List goals = commandLine.getArgList();

            boolean recursive = true;

            String failureType = null;

            if ( commandLine.hasOption( CLIManager.NON_RECURSIVE ) )
            {
                recursive = false;
            }

            if ( commandLine.hasOption( CLIManager.FAIL_FAST ) )
            {
                failureType = ReactorManager.FAIL_FAST;
            }
            else if ( commandLine.hasOption( CLIManager.FAIL_AT_END ) )
            {
                failureType = ReactorManager.FAIL_AT_END;
            }
            else if ( commandLine.hasOption( CLIManager.FAIL_NEVER ) )
            {
                failureType = ReactorManager.FAIL_NEVER;
            }

            boolean offline = false;

            if ( commandLine.hasOption( CLIManager.OFFLINE ) )
            {
                offline = true;
            }

            boolean updateSnapshots = false;

            if ( commandLine.hasOption( CLIManager.UPDATE_SNAPSHOTS ) )
            {
                updateSnapshots = true;
            }

            String globalChecksumPolicy = null;

            if ( commandLine.hasOption( CLIManager.CHECKSUM_FAILURE_POLICY ) )
            {
                // todo; log
                System.out.println( "+ Enabling strict checksum verification on all artifact downloads." );

                globalChecksumPolicy = ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL;
            }
            else if ( commandLine.hasOption( CLIManager.CHECKSUM_WARNING_POLICY ) )
            {
                // todo: log
                System.out.println( "+ Disabling strict checksum verification on all artifact downloads." );

                globalChecksumPolicy = ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN;
            }

            File baseDirectory = new File( System.getProperty( "user.dir" ) );

            // ----------------------------------------------------------------------
            // Profile Activation
            // ----------------------------------------------------------------------

            List activeProfiles = new ArrayList();

            List inactiveProfiles = new ArrayList();

            if ( commandLine.hasOption( CLIManager.ACTIVATE_PROFILES ) )
            {
                String profilesLine = commandLine.getOptionValue( CLIManager.ACTIVATE_PROFILES );

                StringTokenizer profileTokens = new StringTokenizer( profilesLine, "," );

                while ( profileTokens.hasMoreTokens() )
                {
                    String profileAction = profileTokens.nextToken().trim();

                    if ( profileAction.startsWith( "-" ) )
                    {
                        activeProfiles.add( profileAction.substring( 1 ) );
                    }
                    else if ( profileAction.startsWith( "+" ) )
                    {
                        inactiveProfiles.add( profileAction.substring( 1 ) );
                    }
                    else
                    {
                        // TODO: deprecate this eventually!
                        activeProfiles.add( profileAction );
                    }
                }
            }

            TransferListener transferListener;

            if ( interactive )
            {
                transferListener = new ConsoleDownloadMonitor();
            }
            else
            {
                transferListener = new BatchModeDownloadMonitor();
            }

            boolean reactorActive = false;

            if ( commandLine.hasOption( CLIManager.REACTOR ) )
            {
                reactorActive = true;
            }

            String alternatePomFile = null;

            if ( commandLine.hasOption( CLIManager.ALTERNATE_POM_FILE ) )
            {
                alternatePomFile = commandLine.getOptionValue( CLIManager.ALTERNATE_POM_FILE );
            }

            // ----------------------------------------------------------------------
            // From here we are CLI free
            // ----------------------------------------------------------------------

            //  1. LoggerManager: can get from the container
            //  2. debug: use to set the threshold on the logger manager
            //  3. Settings
            //     -> localRepository
            //     -> interactiveMode
            //     -> usePluginRegistry
            //     -> offline
            //     -> proxies
            //     -> servers
            //     -> mirrors
            //     -> profiles
            //     -> activeProfiles
            //     -> pluginGroups
            //  4. ProfileManager
            //     -> active profiles
            //     -> inactive profiles
            //  5. EventDispatcher
            //  6. baseDirectory
            //  7. goals
            //  8. executionProperties
            //  9. failureType: fail fast, fail at end, fail never
            // 10. globalChecksumPolicy: fail, warn
            // 11. showErrors (this is really CLI is but used inside Maven internals
            // 12. recursive
            // 13. offline
            // 14. updateSnapshots
            // 15. reactorActive
            // 16. transferListener: in the CLI this is batch or console
            // 17. interactive

            // We have a general problem with plexus components that are singletons in that they use
            // the same logger for their lifespan. This is not good in that many requests may be fired
            // off and the singleton plexus component will continue to funnel their output to the same
            // logger. We need to be able to swap the logger.

            LoggerManager loggerManager = (LoggerManager) embedder.lookup( LoggerManager.ROLE );

            if ( debug )
            {
                loggerManager.setThreshold( Logger.LEVEL_DEBUG );
            }

            Settings settings = buildSettings( userSettingsPath, interactive, usePluginRegistry, pluginUpdateOverride );

            ProfileManager profileManager = new DefaultProfileManager( embedder.getContainer() );

            profileManager.explicitlyActivate( activeProfiles );

            profileManager.explicitlyDeactivate( inactiveProfiles );

            EventDispatcher eventDispatcher = new DefaultEventDispatcher();

            MavenExecutionRequest request = createRequest( baseDirectory,
                                                           goals,
                                                           settings,
                                                           eventDispatcher,
                                                           loggerManager,
                                                           profileManager,
                                                           executionProperties,
                                                           failureType,
                                                           globalChecksumPolicy,
                                                           showErrors,
                                                           recursive,
                                                           offline,
                                                           updateSnapshots
            );

            request.setReactorActive( reactorActive );

            request.setPomFile( alternatePomFile );

            WagonManager wagonManager = (WagonManager) embedder.lookup( WagonManager.ROLE );

            // this seems redundant having the transferListener be
            wagonManager.setDownloadMonitor( transferListener );

            wagonManager.setInteractive( interactive );

            Maven maven = (Maven) embedder.lookup( Maven.ROLE );

            maven.execute( request );
        }
        catch ( SettingsConfigurationException e )
        {
            showError( "Error reading settings.xml: " + e.getMessage(), e, showErrors );

            return 1;
        }
        catch ( ComponentLookupException e )
        {
            showFatalError( "Unable to configure the Maven application", e, showErrors );

            return 1;
        }
        catch ( MavenExecutionException e )
        {
            return 1;
        }

        return 0;
    }

    private static void showFatalError( String message, Exception e, boolean show )
    {
        System.err.println( "FATAL ERROR: " + message );
        if ( show )
        {
            System.err.println( "Error stacktrace:" );

            e.printStackTrace();
        }
        else
        {
            System.err.println( "For more information, run with the -e flag" );
        }
    }

    private static void showError( String message, Exception e, boolean show )
    {
        System.err.println( message );
        if ( show )
        {
            System.err.println( "Error stacktrace:" );

            e.printStackTrace();
        }
    }

    private static void showVersion()
    {
        InputStream resourceAsStream;
        try
        {
            Properties properties = new Properties();
            resourceAsStream = MavenCli.class.getClassLoader().getResourceAsStream(
                "META-INF/maven/org.apache.maven/maven-core/pom.properties" );
            properties.load( resourceAsStream );

            if ( properties.getProperty( "builtOn" ) != null )
            {
                System.out.println( "Maven version: " + properties.getProperty( "version", "unknown" )
                    + " built on " + properties.getProperty( "builtOn" ) );
            }
            else
            {
                System.out.println( "Maven version: " + properties.getProperty( "version", "unknown" ) );
            }
        }
        catch ( IOException e )
        {
            System.err.println( "Unable determine version from JAR file: " + e.getMessage() );
        }
    }

    // ----------------------------------------------------------------------
    // System properties handling
    // ----------------------------------------------------------------------

    private static Properties getExecutionProperties( CommandLine commandLine )
    {
        Properties executionProperties = new Properties();

        // ----------------------------------------------------------------------
        // Options that are set on the command line become system properties
        // and therefore are set in the session properties. System properties
        // are most dominant.
        // ----------------------------------------------------------------------

        if ( commandLine.hasOption( CLIManager.SET_SYSTEM_PROPERTY ) )
        {
            String[] defStrs = commandLine.getOptionValues( CLIManager.SET_SYSTEM_PROPERTY );

            for ( int i = 0; i < defStrs.length; ++i )
            {
                setCliProperty( defStrs[i], executionProperties );
            }
        }

        executionProperties.putAll( System.getProperties() );

        return executionProperties;
    }

    private static void setCliProperty( String property, Properties executionProperties )
    {
        String name;

        String value;

        int i = property.indexOf( "=" );

        if ( i <= 0 )
        {
            name = property.trim();

            value = "true";
        }
        else
        {
            name = property.substring( 0, i ).trim();

            value = property.substring( i + 1 ).trim();
        }

        executionProperties.setProperty( name, value );

        // ----------------------------------------------------------------------
        // I'm leaving the setting of system properties here as not to break
        // the SystemPropertyProfileActivator. This won't harm embedding. jvz.
        // ----------------------------------------------------------------------

        System.setProperty( name, value );
    }

    // ----------------------------------------------------------------------
    // Command line manager
    // ----------------------------------------------------------------------

    static class CLIManager
    {
        public static final char ALTERNATE_POM_FILE = 'f';

        public static final char BATCH_MODE = 'B';

        public static final char SET_SYSTEM_PROPERTY = 'D';

        public static final char OFFLINE = 'o';

        public static final char REACTOR = 'r';

        public static final char DEBUG = 'X';

        public static final char ERRORS = 'e';

        public static final char HELP = 'h';

        public static final char VERSION = 'v';

        private Options options;

        public static final char NON_RECURSIVE = 'N';

        public static final char UPDATE_SNAPSHOTS = 'U';

        public static final char ACTIVATE_PROFILES = 'P';

        public static final String FORCE_PLUGIN_UPDATES = "cpu";

        public static final String FORCE_PLUGIN_UPDATES2 = "up";

        public static final String SUPPRESS_PLUGIN_UPDATES = "npu";

        public static final String SUPPRESS_PLUGIN_REGISTRY = "npr";

        public static final char CHECKSUM_FAILURE_POLICY = 'C';

        public static final char CHECKSUM_WARNING_POLICY = 'c';

        private static final char ALTERNATE_USER_SETTINGS = 's';

        private static final String FAIL_FAST = "ff";

        private static final String FAIL_AT_END = "fae";

        private static final String FAIL_NEVER = "fn";

        public CLIManager()
        {
            options = new Options();

            options.addOption( OptionBuilder.withLongOpt( "file" ).hasArg().withDescription(
                "Force the use of an alternate POM file." ).create( ALTERNATE_POM_FILE ) );

            options.addOption(
                OptionBuilder.withLongOpt( "define" ).hasArg().withDescription( "Define a system property" ).create(
                    SET_SYSTEM_PROPERTY ) );
            options.addOption(
                OptionBuilder.withLongOpt( "offline" ).withDescription( "Work offline" ).create( OFFLINE ) );
            options.addOption(
                OptionBuilder.withLongOpt( "help" ).withDescription( "Display help information" ).create( HELP ) );
            options.addOption(
                OptionBuilder.withLongOpt( "version" ).withDescription( "Display version information" ).create(
                    VERSION ) );
            options.addOption(
                OptionBuilder.withLongOpt( "debug" ).withDescription( "Produce execution debug output" ).create(
                    DEBUG ) );
            options.addOption(
                OptionBuilder.withLongOpt( "errors" ).withDescription( "Produce execution error messages" ).create(
                    ERRORS ) );
            options.addOption( OptionBuilder.withLongOpt( "reactor" ).withDescription(
                "Execute goals for project found in the reactor" ).create( REACTOR ) );
            options.addOption( OptionBuilder.withLongOpt( "non-recursive" ).withDescription(
                "Do not recurse into sub-projects" ).create( NON_RECURSIVE ) );
            options.addOption( OptionBuilder.withLongOpt( "update-snapshots" ).withDescription(
                "Update all snapshots regardless of repository policies" ).create( UPDATE_SNAPSHOTS ) );
            options.addOption( OptionBuilder.withLongOpt( "activate-profiles" ).withDescription(
                "Comma-delimited list of profiles to activate" ).hasArg().create( ACTIVATE_PROFILES ) );

            options.addOption( OptionBuilder.withLongOpt( "batch-mode" ).withDescription(
                "Run in non-interactive (batch) mode" ).create( BATCH_MODE ) );

            options.addOption( OptionBuilder.withLongOpt( "check-plugin-updates" ).withDescription(
                "Force upToDate check for any relevant registered plugins" ).create( FORCE_PLUGIN_UPDATES ) );
            options.addOption( OptionBuilder.withLongOpt( "update-plugins" ).withDescription(
                "Synonym for " + FORCE_PLUGIN_UPDATES ).create( FORCE_PLUGIN_UPDATES2 ) );
            options.addOption( OptionBuilder.withLongOpt( "no-plugin-updates" ).withDescription(
                "Suppress upToDate check for any relevant registered plugins" ).create( SUPPRESS_PLUGIN_UPDATES ) );

            options.addOption( OptionBuilder.withLongOpt( "no-plugin-registry" ).withDescription(
                "Don't use ~/.m2/plugin-registry.xml for plugin versions" ).create( SUPPRESS_PLUGIN_REGISTRY ) );

            options.addOption( OptionBuilder.withLongOpt( "strict-checksums" ).withDescription(
                "Fail the build if checksums don't match" ).create( CHECKSUM_FAILURE_POLICY ) );
            options.addOption(
                OptionBuilder.withLongOpt( "lax-checksums" ).withDescription( "Warn if checksums don't match" ).create(
                    CHECKSUM_WARNING_POLICY ) );

            options.addOption( OptionBuilder.withLongOpt( "settings" )
                .withDescription( "Alternate path for the user settings file" ).hasArg()
                .create( ALTERNATE_USER_SETTINGS ) );

            options.addOption( OptionBuilder.withLongOpt( "fail-fast" ).withDescription(
                "Stop at first failure in reactorized builds" ).create( FAIL_FAST ) );

            options.addOption( OptionBuilder.withLongOpt( "fail-at-end" ).withDescription(
                "Only fail the build afterwards; allow all non-impacted builds to continue" ).create( FAIL_AT_END ) );

            options.addOption( OptionBuilder.withLongOpt( "fail-never" ).withDescription(
                "NEVER fail the build, regardless of project result" ).create( FAIL_NEVER ) );
        }

        public CommandLine parse( String[] args )
            throws ParseException
        {
            CommandLineParser parser = new GnuParser();
            return parser.parse( options, args );
        }

        public void displayHelp()
        {
            System.out.println();

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "mvn [options] [<goal(s)>] [<phase(s)>]", "\nOptions:", options, "\n" );
        }
    }

    // ----------------------------------------------------------------------
    // Methods that are now decoupled from the CLI, we want to push these
    // into DefaultMaven and use them in the embedder as well.
    // ----------------------------------------------------------------------

    private static MavenExecutionRequest createRequest( File baseDirectory,
                                                        List goals,
                                                        Settings settings,
                                                        EventDispatcher eventDispatcher,
                                                        LoggerManager loggerManager,
                                                        ProfileManager profileManager,
                                                        Properties executionProperties,
                                                        String failureType,
                                                        String globalChecksumPolicy,
                                                        boolean showErrors,
                                                        boolean recursive,
                                                        boolean offline,
                                                        boolean updateSnapshots
    )
        throws ComponentLookupException
    {
        MavenExecutionRequest request;

        ArtifactRepository localRepository = createLocalRepository( embedder, settings, offline, updateSnapshots, globalChecksumPolicy );

        request = new DefaultMavenExecutionRequest( localRepository,
                                                    settings,
                                                    eventDispatcher,
                                                    goals,
                                                    baseDirectory.getAbsolutePath(),
                                                    profileManager,
                                                    executionProperties,
                                                    showErrors );

        Logger logger = loggerManager.getLoggerForComponent( Mojo.ROLE );

        request.addEventMonitor( new DefaultEventMonitor( logger ) );

        if ( !recursive )
        {
            request.setRecursive( false );
        }

        request.setFailureBehavior( failureType );

        return request;
    }

    private static ArtifactRepository createLocalRepository( Embedder embedder,
                                                             Settings settings,
                                                             boolean offline,
                                                             boolean updateSnapshots,
                                                             String globalChecksumPolicy )
        throws ComponentLookupException
    {
        // TODO: release
        // TODO: something in plexus to show all active hooks?
        ArtifactRepositoryLayout repositoryLayout =
            (ArtifactRepositoryLayout) embedder.lookup( ArtifactRepositoryLayout.ROLE, "default" );

        ArtifactRepositoryFactory artifactRepositoryFactory =
            (ArtifactRepositoryFactory) embedder.lookup( ArtifactRepositoryFactory.ROLE );

        String url = settings.getLocalRepository();

        if ( !url.startsWith( "file:" ) )
        {
            url = "file://" + url;
        }

        ArtifactRepository localRepository = new DefaultArtifactRepository( "local", url, repositoryLayout );

        boolean snapshotPolicySet = false;

        if ( offline )
        {
            settings.setOffline( true );

            snapshotPolicySet = true;
        }

        if ( !snapshotPolicySet && updateSnapshots )
        {
            artifactRepositoryFactory.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        }

        artifactRepositoryFactory.setGlobalChecksumPolicy( globalChecksumPolicy );

        return localRepository;
    }

    private static Settings buildSettings( String userSettingsPath, boolean interactive, boolean usePluginRegistry, Boolean pluginUpdateOverride )
        throws ComponentLookupException, SettingsConfigurationException
    {
        Settings settings = null;

        MavenSettingsBuilder settingsBuilder = (MavenSettingsBuilder) embedder.lookup( MavenSettingsBuilder.ROLE );

        try
        {
            if ( userSettingsPath != null )
            {
                File userSettingsFile = new File( userSettingsPath );

                if ( userSettingsFile.exists() && !userSettingsFile.isDirectory() )
                {
                    settings = settingsBuilder.buildSettings( userSettingsFile );
                }
                else
                {
                    System.out.println( "WARNING: Alternate user settings file: " + userSettingsPath +
                        " is invalid. Using default path." );
                }
            }

            if ( settings == null )
            {
                settings = settingsBuilder.buildSettings();
            }
        }
        catch ( IOException e )
        {
            throw new SettingsConfigurationException( "Error reading settings file", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new SettingsConfigurationException( e.getMessage(), e.getDetail(), e.getLineNumber(),
                                                      e.getColumnNumber() );
        }

        settings.setInteractiveMode( interactive );

        settings.setUsePluginRegistry( usePluginRegistry );

        RuntimeInfo runtimeInfo = new RuntimeInfo( settings );

        runtimeInfo.setPluginUpdateOverride( pluginUpdateOverride );

        settings.setRuntimeInfo( runtimeInfo );

        return settings;
    }
}
