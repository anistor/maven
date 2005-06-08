package org.apache.maven.project.inheritance;

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

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Scm;
import org.apache.maven.model.Site;
import org.apache.maven.project.ModelUtils;
import org.codehaus.plexus.util.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id: DefaultModelInheritanceAssembler.java,v 1.4 2004/08/23 20:24:54
 *          jdcasey Exp $
 * @todo generate this with modello to keep it in sync with changes in the model.
 */
public class DefaultModelInheritanceAssembler
    implements ModelInheritanceAssembler
{
    public void assembleModelInheritance( Model child, Model parent )
    {
        // cannot inherit from null parent.
        if ( parent == null )
        {
            return;
        }
        
        // Group id
        if ( child.getGroupId() == null )
        {
            child.setGroupId( parent.getGroupId() );
        }

        // version
        // TODO: I think according to the latest design docs, we don't want to inherit version at all
        if ( child.getVersion() == null )
        {
            // The parent version may have resolved to something different, so we take what we asked for...
            // instead of - child.setVersion( parent.getVersion() );

            if ( child.getParent() != null )
            {
                child.setVersion( child.getParent().getVersion() );
            }
        }

        // inceptionYear
        if ( child.getInceptionYear() == null )
        {
            child.setInceptionYear( parent.getInceptionYear() );
        }

        // url
        if ( child.getUrl() == null )
        {
            if ( parent.getUrl() != null )
            {
                child.setUrl( appendPath( parent.getUrl(), child.getArtifactId() ) );
            }
            else
            {
                child.setUrl( parent.getUrl() );
            }
        }

        // ----------------------------------------------------------------------
        // Distribution
        // ----------------------------------------------------------------------

        assembleDistributionInheritence( child, parent );

        // issueManagement
        if ( child.getIssueManagement() == null )
        {
            child.setIssueManagement( parent.getIssueManagement() );
        }

        // description
        if ( child.getDescription() == null )
        {
            child.setDescription( parent.getDescription() );
        }

        // Organization
        if ( child.getOrganization() == null )
        {
            child.setOrganization( parent.getOrganization() );
        }

        // Scm
        assembleScmInheritance( child, parent );

        // ciManagement
        if ( child.getCiManagement() == null )
        {
            child.setCiManagement( parent.getCiManagement() );
        }

        // developers
        if ( child.getDevelopers().size() == 0 )
        {
            child.setDevelopers( parent.getDevelopers() );
        }

        // developers
        if ( child.getContributors().size() == 0 )
        {
            child.setContributors( parent.getContributors() );
        }

        // mailingLists
        if ( child.getMailingLists().size() == 0 )
        {
            child.setMailingLists( parent.getMailingLists() );
        }

        // Build
        assembleBuildInheritance( child, parent.getBuild() );

        assembleModelBaseInheritance( child, parent );
    }

    public void mergeProfileWithModel( Model model, Profile profile )
    {
        assembleModelBaseInheritance( model, profile );

        assembleBuildBaseInheritance( model.getBuild(), profile.getBuild() );
    }

    private void assembleModelBaseInheritance( ModelBase child, ModelBase parent )
    {
        // Dependencies :: aggregate
        List dependencies = parent.getDependencies();

        for ( Iterator iterator = dependencies.iterator(); iterator.hasNext(); )
        {
            Dependency dependency = (Dependency) iterator.next();

            child.addDependency( dependency );

        }

        // Repositories :: aggregate
        List parentRepositories = parent.getRepositories();

        List childRepositories = child.getRepositories();

        for ( Iterator iterator = parentRepositories.iterator(); iterator.hasNext(); )
        {
            Repository repository = (Repository) iterator.next();

            if ( !childRepositories.contains( repository ) )
            {
                child.addRepository( repository );
            }
        }

        // Mojo Repositories :: aggregate
        List parentPluginRepositories = parent.getPluginRepositories();
        List childPluginRepositories = child.getPluginRepositories();

        for ( Iterator iterator = parentPluginRepositories.iterator(); iterator.hasNext(); )
        {
            Repository repository = (Repository) iterator.next();

            if ( !childPluginRepositories.contains( repository ) )
            {
                child.addPluginRepository( repository );
            }
        }

        // Reports :: aggregate
        if ( child.getReports() != null && parent.getReports() != null )
        {
            if ( child.getReports().getOutputDirectory() == null )
            {
                child.getReports().setOutputDirectory( parent.getReports().getOutputDirectory() );
            }

            List parentReports = parent.getReports().getPlugins();

            List childReports = child.getReports().getPlugins();

            for ( Iterator iterator = parentReports.iterator(); iterator.hasNext(); )
            {
                Plugin plugin = (Plugin) iterator.next();

                if ( !childReports.contains( plugin ) )
                {
                    child.getReports().addPlugin( plugin );
                }
            }
        }

        assembleDependencyManagementInheritance( child, parent );
        
        // Evaluation Properties :: aggregate
        child.addEvalProperties( parent.getEvalProperties() );
    }

    private void assembleDependencyManagementInheritance( ModelBase child, ModelBase parent )
    {
        DependencyManagement parentDepMgmt = parent.getDependencyManagement();

        DependencyManagement childDepMgmt = child.getDependencyManagement();

        if ( parentDepMgmt != null )
        {
            if ( childDepMgmt == null )
            {
                child.setDependencyManagement( parentDepMgmt );
            }
            else
            {
                List childDeps = childDepMgmt.getDependencies();

                Map mappedChildDeps = new TreeMap();
                for ( Iterator it = childDeps.iterator(); it.hasNext(); )
                {
                    Dependency dep = (Dependency) it.next();
                    mappedChildDeps.put( dep.getManagementKey(), dep );
                }

                for ( Iterator it = parentDepMgmt.getDependencies().iterator(); it.hasNext(); )
                {
                    Dependency dep = (Dependency) it.next();
                    if ( !mappedChildDeps.containsKey( dep.getManagementKey() ) )
                    {
                        childDepMgmt.addDependency( dep );
                    }
                }
            }
        }
    }

    private void assembleBuildInheritance( Model child, Build parentBuild )
    {
        // cannot inherit from null parent...
        if ( parentBuild == null )
        {
            return;
        }
        
        Build childBuild = child.getBuild();

        if ( parentBuild != null )
        {
            if ( childBuild == null )
            {
                childBuild = new Build();
                child.setBuild( childBuild );
            }
            // The build has been set but we want to step in here and fill in
            // values
            // that have not been set by the child.

            if ( childBuild.getDirectory() == null )
            {
                childBuild.setDirectory( parentBuild.getDirectory() );
            }

            if ( childBuild.getSourceDirectory() == null )
            {
                childBuild.setSourceDirectory( parentBuild.getSourceDirectory() );
            }

            if ( childBuild.getScriptSourceDirectory() == null )
            {
                childBuild.setScriptSourceDirectory( parentBuild.getScriptSourceDirectory() );
            }

            if ( childBuild.getTestSourceDirectory() == null )
            {
                childBuild.setTestSourceDirectory( parentBuild.getTestSourceDirectory() );
            }

            if ( childBuild.getOutputDirectory() == null )
            {
                childBuild.setOutputDirectory( parentBuild.getOutputDirectory() );
            }

            if ( childBuild.getTestOutputDirectory() == null )
            {
                childBuild.setTestOutputDirectory( parentBuild.getTestOutputDirectory() );
            }

            assembleBuildBaseInheritance( childBuild, parentBuild );
        }
    }

    private void assembleBuildBaseInheritance( BuildBase childBuild, BuildBase parentBuild )
    {
        // if the parent build is null, obviously we cannot inherit from it...
        if ( parentBuild == null )
        {
            return;
        }

        if ( childBuild.getDefaultGoal() == null )
        {
            childBuild.setDefaultGoal( parentBuild.getDefaultGoal() );
        }

        if ( childBuild.getFinalName() == null )
        {
            childBuild.setFinalName( parentBuild.getFinalName() );
        }

        List resources = childBuild.getResources();
        if ( resources == null || resources.isEmpty() )
        {
            childBuild.setResources( parentBuild.getResources() );
        }

        resources = childBuild.getTestResources();
        if ( resources == null || resources.isEmpty() )
        {
            childBuild.setTestResources( parentBuild.getTestResources() );
        }

        // Plugins are aggregated if Plugin.inherit != false
        ModelUtils.mergePluginLists( childBuild, parentBuild, true );

        // Plugin management :: aggregate
        if ( childBuild != null && parentBuild != null )
        {
            ModelUtils.mergePluginLists( childBuild.getPluginManagement(), parentBuild.getPluginManagement(), false );
        }
    }

    private void assembleScmInheritance( Model child, Model parent )
    {
        if ( parent.getScm() != null )
        {
            Scm parentScm = parent.getScm();

            Scm childScm = child.getScm();

            if ( childScm == null )
            {
                childScm = new Scm();

                child.setScm( childScm );
            }

            if ( StringUtils.isEmpty( childScm.getConnection() ) && !StringUtils.isEmpty( parentScm.getConnection() ) )
            {
                childScm.setConnection( appendPath( parentScm.getConnection(), child.getArtifactId() ) );
            }

            if ( StringUtils.isEmpty( childScm.getDeveloperConnection() )
                && !StringUtils.isEmpty( parentScm.getDeveloperConnection() ) )
            {
                childScm
                    .setDeveloperConnection( appendPath( parentScm.getDeveloperConnection(), child.getArtifactId() ) );
            }

            if ( StringUtils.isEmpty( childScm.getUrl() ) && !StringUtils.isEmpty( parentScm.getUrl() ) )
            {
                childScm.setUrl( appendPath( parentScm.getUrl(), child.getArtifactId() ) );
            }
        }
    }

    private void assembleDistributionInheritence( Model child, Model parent )
    {
        if ( parent.getDistributionManagement() != null )
        {
            DistributionManagement parentDistMgmt = parent.getDistributionManagement();

            DistributionManagement childDistMgmt = child.getDistributionManagement();

            if ( childDistMgmt == null )
            {
                childDistMgmt = new DistributionManagement();

                child.setDistributionManagement( childDistMgmt );
            }

            if ( childDistMgmt.getSite() == null )
            {
                if ( parentDistMgmt.getSite() != null )
                {
                    Site site = new Site();

                    childDistMgmt.setSite( site );

                    site.setId( parentDistMgmt.getSite().getId() );

                    site.setName( parentDistMgmt.getSite().getName() );

                    site.setUrl( parentDistMgmt.getSite().getUrl() );

                    if ( site.getUrl() != null )
                    {
                        site.setUrl( appendPath( site.getUrl(), child.getArtifactId() ) );
                    }
                }
            }

            if ( childDistMgmt.getRepository() == null )
            {
                if ( parentDistMgmt.getRepository() != null )
                {
                    Repository repository = new Repository();

                    childDistMgmt.setRepository( repository );

                    repository.setId( parentDistMgmt.getRepository().getId() );

                    repository.setName( parentDistMgmt.getRepository().getName() );

                    repository.setUrl( parentDistMgmt.getRepository().getUrl() );
                }
            }
        }
    }

    private static String appendPath( String url, String path )
    {
        if ( url.endsWith( "/" ) )
        {
            return url + path;
        }
        else
        {
            return url + "/" + path;
        }
    }
}
