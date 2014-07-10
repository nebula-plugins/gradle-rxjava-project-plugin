package nebula.plugin.rxjavaproject

import nebula.test.IntegrationSpec

class RxjavaLicensePluginLauncherSpec extends IntegrationSpec {
    def 'lazily save file'() {
        buildFile << """
            apply plugin: 'java'
            ${applyPlugin(RxjavaLicensePlugin)}
        """.stripIndent()

        when:
        def results = runTasksSuccessfully('license')

        then:
        results.wasExecuted(':writeLicenseHeader')
        new File(projectDir, 'build/license/HEADER').exists()
    }

    def 'avoid saving file'() {
        file('LICENSE').text = "Free for all!!"
        buildFile << """
            apply plugin: 'java'
            ${applyPlugin(RxjavaLicensePlugin)}
            license {
                header file('LICENSE')
            }
        """.stripIndent()

        when:
        def results = runTasksSuccessfully('license')

        then:
        results.wasExecuted(':writeLicenseHeader')
        !results.wasUpToDate(':writeLicenseHeader')
        !new File(projectDir, 'build/license/HEADER').exists()
    }
}
