/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nebula.plugin.rxjavaproject

import nebula.core.GradleHelper
import nebula.plugin.contacts.ContactsPlugin
import nebula.plugin.dependencylock.DependencyLockPlugin
import nebula.plugin.info.InfoPlugin
import nebula.plugin.publishing.NebulaJavadocJarPlugin
import nebula.plugin.publishing.NebulaPublishingPlugin
import nebula.plugin.publishing.NebulaSourceJarPlugin
import nebula.plugin.publishing.NebulaTestJarPlugin
import nebula.plugin.publishing.sign.NebulaSignPlugin
import nebula.plugin.responsible.FixJavaPlugin
import nebula.plugin.responsible.NebulaFacetPlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.idea.IdeaPlugin

class RxjavaProjectPlugin implements Plugin<Project> {

    Project project

    @Override
    void apply(Project project) {
        this.project = project

        project.plugins.apply(FixJavaPlugin)

        // Repositories
        project.repositories.jcenter()

        // Publishing
        project.plugins.apply(NebulaPublishingPlugin)
        project.plugins.apply(NebulaSignPlugin)
        project.plugins.apply(NebulaJavadocJarPlugin)
        project.plugins.apply(NebulaSourceJarPlugin)
        project.plugins.apply(NebulaTestJarPlugin)

        // Info
        project.plugins.apply(InfoPlugin)

        // Contacts
        ContactsPlugin contactsPlugin = project.plugins.apply(ContactsPlugin)
        project.contacts {
            'benjchristensen@netflix.com' {
                github 'benjchristensen'
                moniker 'Ben Christensen'
            }
        }

        // Dependency Locking
        project.plugins.apply(DependencyLockPlugin)

        // IDE Support
        project.plugins.apply EclipsePlugin
        project.plugins.apply IdeaPlugin

        // Facets
        def facetPlugin = (NebulaFacetPlugin) project.plugins.apply(NebulaFacetPlugin)
        facetPlugin.extension.create('examples')  {
            parentSourceSet = 'main'
        }

        // Default Group
        def gradleHelper = new GradleHelper(project)
        gradleHelper.addDefaultGroup('com.netflix.rxjava') // TODO This will have to change to reactivex

        // ReactiveX specific plugins
        project.plugins.apply RxjavaPerformancePlugin
        project.plugins.apply RxjavaOsgiPlugin

        // Set Default java versions
        project.plugins.withType(JavaPlugin) { JavaPlugin javaPlugin ->
            JavaPluginConvention convention = project.convention.getPlugin(JavaPluginConvention)
            convention.sourceCompatibility = JavaVersion.VERSION_1_6
            convention.targetCompatibility = JavaVersion.VERSION_1_6
        }

        // TODO Publish javadoc back to Github for hosting
        project.tasks.withType(Javadoc) {
            failOnError = false
            // we do not want the org.rx.operations package include
            exclude '**/operations/**'

            options {
                // TODO Publish Doclet to global location
                // doclet = "org.benjchristensen.doclet.DocletExclude"
                // docletpath = [rootProject.file('./gradle/doclet-exclude.jar')]

                // TODO Embed stylesheet into .jar
                // stylesheetFile = rootProject.file('./gradle/javadocStyleSheet.css')

                // TODO See why this was initially added.
                // it.classpath = sourceSets.main.compileClasspath
                windowTitle = "RxJava Javadoc ${project.version}"
            }
            options.addStringOption('top').value = '<h2 class="title" style="padding-top:40px">RxJava</h2>'
        }

        project.tasks.withType(Test) { Test testTask ->
            testTask.testLogging.exceptionFormat = 'full'
            testTask.testLogging.events "started"
            testTask.testLogging.displayGranularity = 2
        }
    }
}
