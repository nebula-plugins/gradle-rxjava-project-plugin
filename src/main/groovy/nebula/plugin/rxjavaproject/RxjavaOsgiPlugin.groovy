/*
 * Copyright 2014-2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nebula.plugin.rxjavaproject

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.osgi.OsgiManifest
import org.gradle.api.plugins.osgi.OsgiPlugin
import org.gradle.api.tasks.bundling.Jar

/**
 * Apply OSGI specific fields
 */
class RxjavaOsgiPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(OsgiPlugin)

        // TODO Use scm info to determine DocURL

        project.tasks.matching { it.name == 'jar' }.all { Jar jarTask ->
            project.plugins.withType(JavaPlugin) {
                // OsgiPlugin will only set the manifest to a OsgiManifest if the JavaPlugin is applied
                jarTask.manifest {
                    // Add some static typing to these calls
                    OsgiManifest manifest = (OsgiManifest) delegate

                    manifest.name = project.name
                    manifest.instruction 'Bundle-Vendor', 'ReactiveX'
                    manifest.instruction 'Bundle-DocURL', 'https://github.com/ReactiveX/RxJava'

                    // Legacy from what we had test in the compile classpath
                    manifest.instruction 'Import-Package', '!org.junit,!junit.framework,!org.mockito.*,*'

                    // rxjava-core needs this:
                    // instruction 'Eclipse-ExtensibleAPI', 'true'

                    // Everyone by rxjava-core needs this to be added to the rxjava-core bundle:
                    //instruction 'Fragment-Host', 'com.netflix.rxjava.core'
                    // TODO Find way to add this to all, but exclude rxjava-core
                }
            }
        }
    }
}
