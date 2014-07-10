package nebula.plugin.rxjavaproject

import nebula.test.ProjectSpec
import org.gradle.api.plugins.JavaPlugin

class RxjavaLicensePluginSpec extends ProjectSpec {
    def 'lazily save file'() {
        when:
        project.plugins.apply(JavaPlugin)
        project.plugins.apply(RxjavaLicensePlugin)

        then:
        def HEADER = new File(projectDir, 'build/license/HEADER')
        !HEADER.exists()

        when:
        def headerTask = project.tasks.getByName('writeLicenseHeader')
        headerTask.getActions().each { it.execute(headerTask) }

        then:
        HEADER.exists()
    }
}
