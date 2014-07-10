package nebula.plugin.rxjavaproject

import org.gradle.api.Plugin
import org.gradle.api.Project
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
            jarTask.manifest { // Will be OsgiManifest

                name = project.name
                instruction 'Bundle-Vendor', 'ReactiveX'
                instruction 'Bundle-DocURL', 'https://github.com/ReactiveX/RxJava'

                // Legacy from what we had test in the compile classpath
                instruction 'Import-Package', '!org.junit,!junit.framework,!org.mockito.*,*'

                // rxjava-core needs this:
                // instruction 'Eclipse-ExtensibleAPI', 'true'

                // Everyone by rxjava-core needs this to be added to the rxjava-core bundle:
                //instruction 'Fragment-Host', 'com.netflix.rxjava.core'
                // TODO Find way to add this to all, but exclude rxjava-core
            }
        }
    }
}
