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

import com.google.common.io.Files
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import spock.lang.Ignore

class RxJavaReleasePluginMultiLauncherSpec extends RxJavaIntegrationSpec {

    Grgit grgit
    Grgit originGit

    def setup() {
        // Place to push to
        def origin = new File(projectDir.parent, "${projectDir.name}.git")
        origin.mkdirs()

        ['build.gradle', 'settings.gradle'].each {
            Files.move(new File(projectDir, it), new File(origin, it))
        }
        // Should be empty now.

        originGit = Grgit.init(dir: origin)
        originGit.add(patterns: ["build.gradle", 'settings.gradle', '.gitignore'] as Set)
        originGit.commit(message: 'Initial checkout')

        // Clone into our real project directory
        grgit = Grgit.clone(dir: projectDir, uri: origin.absolutePath)

        new File(projectDir, '.gitignore') << """
            .gradle-test-kit
            .gradle
            build/
            """.stripIndent()

        buildFile << """
            ext.dryRun = true
            group = 'test'
            allprojects {
                ${applyPlugin(RxjavaProjectPlugin)}
            }
            prepare.doFirst {
                println version.toString()
            }
            """.stripIndent()

        def subBuildFile = """
            apply plugin: 'java'
        """.stripIndent()

        // Sub A
        createSubProject('SubA', subBuildFile)
        writeHelloWorld('SubA/', 'reactivey')

        // Sub B
        createSubProject('SubB', subBuildFile)
        writeHelloWorld('SubB/', 'reactivez')

        grgit.add(patterns: ['build.gradle', 'settings.gradle', '.gitignore'] as Set)
        grgit.add(patterns: ['SubA/build.gradle', 'SubA/src/main/java/reactivey/HelloWorld.java'] as Set)
        grgit.add(patterns: ['SubB/build.gradle', 'SubB/src/main/java/reactivez/HelloWorld.java'] as Set)
        grgit.commit(message: 'Setup')
        grgit.push()
    }

    def cleanup() {
        if (grgit) grgit.close()
        if (originGit) originGit.close()
    }

    def 'perform release'() {
        when:
        def results = runTasksSuccessfully('final')

        then:
        results.wasExecuted(':SubA:build')
        results.wasExecuted(':SubB:build')
        !results.wasExecuted(':artifactoryUpload')
        results.wasExecuted(':bintrayUpload')
        !results.wasUpToDate(':SubA:bintrayUpload')
        !results.wasUpToDate(':SubB:bintrayUpload')

        //Grgit originGit = Grgit.open(origin)
        def tags = originGit.tag.list()
        Tag tag001 = tags.find { Tag tag -> tag.name == 'v0.1.0' }
        tag001
        tag001.shortMessage == 'Release of 0.1.0'
        tag001.fullMessage.startsWith('Release of 0.1.0')
        tag001.fullMessage.contains(": Setup")
        tag001.fullMessage.contains(": Initial checkout")

        when:
        writeHelloWorld('SubB/', 'test')
        grgit.add(patterns: ['SubB/src/main/java/test/HelloWorld.java'] as Set)
        grgit.commit(message: 'Adding Test Hello World')

        runTasksSuccessfully('final')

        then:
        def tags2 = originGit.tag.list()
        def tag002 = tags2.find { Tag tag -> tag.name == 'v0.2.0'}
        tag002
        tag002.fullMessage  =~ /Release of 0\.2\.0\s+- [0-9a-fA-F]+: Adding Test Hello World/
    }

}
