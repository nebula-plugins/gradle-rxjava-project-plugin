package nebula.plugin.rxjavaproject

import nebula.core.GradleHelper
import nebula.plugin.responsible.FacetDefinition
import nebula.plugin.responsible.NebulaFacetPlugin
import nl.javadude.gradle.plugins.license.License
import nl.javadude.gradle.plugins.license.LicenseExtension
import nl.javadude.gradle.plugins.license.LicensePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin

/**
 * Leverage license plugin to show missing headers, and inject license into the POM
 */
class RxjavaLicensePlugin  implements Plugin<Project> {

    Project project
    File header

    @Override
    void apply(Project project) {
        this.project = project

        project.plugins.apply(LicensePlugin)
        def licenseExtension = project.extensions.getByType(LicenseExtension)
        licenseExtension.skipExistingHeaders = true
        licenseExtension.strictCheck = false
        licenseExtension.ext.year = Calendar.getInstance().get(Calendar.YEAR)

        header = defineHeaderFile()
        licenseExtension.header = header

        // Limit to just main sourceSet
        project.plugins.withType(JavaBasePlugin) {
            // This is actually too late, because of a bug in the license plugin
            licenseExtension.sourceSets = [project.sourceSets.main]
        }

        // Hack to work around above bug
        project.plugins.withType(NebulaFacetPlugin) {NebulaFacetPlugin facetPlugin ->
            facetPlugin.extension.all { FacetDefinition facet ->
                License licenseCheckTask = project.tasks.getByName("license${facet.name.capitalize()}")
                licenseCheckTask.ignoreFailures = true
            }
        }

        def writeTask = project.task('writeLicenseHeader') {
            description 'Write license header for License tasks'
            onlyIf {
                def licenseTasks = project.gradle.taskGraph.getAllTasks().findAll { it instanceof License }
                return licenseTasks.any { ((License) it).getHeader() == header }
            }
            doFirst {
                header.parentFile.mkdirs()
                copyHeaderFile()
            }
        }
        project.tasks.withType(License) {
            it.dependsOn(writeTask)
        }
    }

    File defineHeaderFile() {
        File tmpDir = new GradleHelper(project).getTempDir('license')
        File to = new File(tmpDir, 'HEADER')
        return to
    }

    def copyHeaderFile() {
        return ClasspathHelper.copyResource('reactivex/codequality/HEADER', header)
    }

}
