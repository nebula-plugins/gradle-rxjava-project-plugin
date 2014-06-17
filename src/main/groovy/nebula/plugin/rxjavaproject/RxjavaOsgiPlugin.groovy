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
            jarTask.manifest { // Should be OsgiManifest
                name = project.name
                instruction 'Bundle-Vendor', 'ReactiveX'
                instruction 'Bundle-DocURL', 'https://github.com/Netflix/RxJava'
                instruction 'Import-Package', '!org.junit,!junit.framework,!org.mockito.*,*'
                instruction 'Eclipse-ExtensibleAPI', 'true'
            }
        }
    }
}
