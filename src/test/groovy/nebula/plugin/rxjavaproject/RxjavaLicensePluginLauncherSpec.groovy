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

    def 'find missing licenses'() {
        writeHelloWorld("nebula")
        buildFile << """
            apply plugin: 'java'
            ${applyPlugin(RxjavaLicensePlugin)}
        """.stripIndent()

        when:
        def results = runTasksSuccessfully('licenseMain') // we don't fail the build

        then:
        results.standardOutput.contains('Missing header in: src/main/java/nebula/HelloWorld.java')
        // results.standardError.contains("License violations were found")
    }

    def 'fix license header'() {
        writeHelloWorld("nebula")
        File f = new File(projectDir, 'src/main/java/nebula/HelloWorld.java')
        buildFile << """
            apply plugin: 'java'
            ${applyPlugin(RxjavaLicensePlugin)}
        """.stripIndent()

        when:
        runTasksSuccessfully('licenseFormatMain')
        runTasksSuccessfully('licenseMain')

        then:
        f.text.contains("Copyright")
    }

}
