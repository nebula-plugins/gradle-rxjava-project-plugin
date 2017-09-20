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

import org.ajoberstar.grgit.Grgit

class RxjavaProjectPluginMultiSpec extends RxJavaIntegrationSpec {

    Grgit originGit

    def snapshotVer = '0.1.0-SNAPSHOT'

    def setup() {
        def subBuildFile = """
            ${applyPlugin(RxjavaProjectPlugin)}
            apply plugin: 'java'
            license {
                ignoreFailures = true
            }
        """.stripIndent()

        // Sub A
        createSubProject('SubA', subBuildFile)
        writeHelloWorld('SubA/', 'reactivey')

        // Sub B
        createSubProject('SubB', subBuildFile)
        writeHelloWorld('SubB/', 'reactivez')

        createFile('gradle.properties') << 'version=1.0.0-SNAPSHOT'
        buildFile << """
            ${applyPlugin(RxjavaProjectPlugin)}
        """.stripIndent()

        new File(projectDir, '.gitignore') << """
            .gradle-test-kit
            .gradle
            build/
            """.stripIndent()

        originGit = Grgit.init(dir: projectDir)
        originGit.add(patterns: ["build.gradle", 'settings.gradle', '.gitignore'] as Set)
        originGit.commit(message: 'Initial checkout')
    }

    def cleanup() {
        if(originGit) {
            originGit.close()
        }
    }

    def 'stand it all up'() {
        setup:
        // Shadow plugin is using SimpleWorkResult and is deprecated in 4.2
        System.setProperty('ignoreDeprecations', 'true')

        when:
        def result = runTasksSuccessfully('build')

        then:
        //new File(projectDir)
        fileExists("SubA/build/libs/SubA-${snapshotVer}.jar")
        fileExists("SubB/build/libs/SubB-${snapshotVer}.jar")

        cleanup:
        System.clearProperty('ignoreDeprecations')
    }
}
