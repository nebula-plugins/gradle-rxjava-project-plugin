package nebula.plugin.rxjavaproject

import nebula.core.GradleHelper
import nl.javadude.gradle.plugins.license.License
import nl.javadude.gradle.plugins.license.LicenseExtension
import nl.javadude.gradle.plugins.license.LicensePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

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
        licenseExtension.ignoreFailures = true // TODO Maybe this should be changed
        licenseExtension.ext.year = Calendar.getInstance().get(Calendar.YEAR)

        header = defineHeaderFile()
        licenseExtension.header = header

        def writeTask = project.task('writeLicenseHeader') {
            description 'Write license header for License tasks'
            onlyIf {
                def licenseTasks = project.gradle.taskGraph.getAllTasks().findAll { it instanceof License }
                return licenseTasks.any { ((License) it).getHeader() == header }
            }
            doFirst {
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
