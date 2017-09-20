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
import nebula.plugin.publishing.maven.MavenPublishPlugin
import nebula.test.IntegrationSpec
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import org.ajoberstar.grgit.operation.BranchAddOp
import org.gradle.api.plugins.JavaPlugin

class RxJavaReleasePluginLauncherSpec extends IntegrationSpec {

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

        grgit = Grgit.clone(dir: projectDir, uri: origin.absolutePath)

        new File(projectDir, '.gitignore') << """
            .gradle-test-kit
            .gradle
            build/
            """.stripIndent()

        buildFile << """
            ext.dryRun = true
            group = 'test'
            ${applyPlugin(MavenPublishPlugin)}
            ${applyPlugin(RxJavaPublishingPlugin)}
            ${applyPlugin(RxJavaReleasePlugin)}
            ${applyPlugin(JavaPlugin)}

            task printVersion {
                doFirst {
                    logger.lifecycle "Version is \${version}"
                }
            }
            """.stripIndent()

        grgit.add(patterns: ['build.gradle', '.gitignore'] as Set)
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
        results.wasExecuted(':build')
        !results.wasExecuted(':artifactoryUpload')
        !results.wasUpToDate(':bintrayUpload')

        def tags = originGit.tag.list()
        Tag tag001 = tags.find { Tag tag -> tag.name == 'v0.1.0' }
        tag001
        tag001.fullMessage.startsWith 'Release of 0.1.0'

        when:
        writeHelloWorld('test')
        grgit.add(patterns: ['src/main/java/test/HelloWorld.java'] as Set)
        grgit.commit(message: 'Adding Hello World')

        runTasksSuccessfully('final')

        then:
        def tags2 = originGit.tag.list()
        def tag002 = tags2.find { Tag tag -> tag.name == 'v0.2.0'}
        tag002
        tag002.fullMessage.startsWith 'Release of 0.2.0\n\n- '
    }

    def 'perform candidate'() {
        when:
        def taskResult = runTasksSuccessfully('tasks', '--all')

        then:
        !(taskResult.standardOutput =~ /bintrayUpload .* \[.*publishMavenNebulaPublicationToMavenLocal\.*]/)

        when:
        def results = runTasksSuccessfully('candidate')

        then:
        def tags = originGit.tag.list()
        tags.find { Tag tag -> tag.name == 'v0.1.0-rc.1' }

        when:
        writeHelloWorld('test')
        grgit.add(patterns: ['src/main/java/test/HelloWorld.java'] as Set)
        grgit.commit(message: 'Adding Hello World')

        runTasksSuccessfully('candidate')

        then:
        def tags2 = originGit.tag.list()
        tags2.find { Tag tag -> tag.name == 'v0.1.0-rc.2'}
    }

    def 'perform snapshots'() {
        when:
        def results = runTasksSuccessfully('candidate')

        then:
        def tags = originGit.tag.list()
        tags.collect { it.name }.any {it == 'v0.1.0-rc.1'}

        when:
        writeHelloWorld('test')
        grgit.add(patterns: ['src/main/java/test/HelloWorld.java'] as Set)
        grgit.commit(message: 'Adding Hello World')

        def result = runTasksSuccessfully('printVersion')

        then:
        result.standardOutput.contains("Version is 0.1.0-SNAPSHOT")
    }

    def 'perform dev snapshots from branch'() {

        when:
        writeHelloWorld('test')
        grgit.add(patterns: ['src/main/java/test/HelloWorld.java'] as Set)
        grgit.commit(message: 'Adding Hello World')

        def result = runTasksSuccessfully('devSnapshot', 'printVersion')
        then:
        result.standardOutput =~ /Version is 0\.1\.0-dev\./

        when:
        grgit.branch.add(name: 'feature/myfeature', startPoint: 'master', mode: BranchAddOp.Mode.TRACK)
        grgit.checkout(branch: 'feature/myfeature')

        grgit.add(patterns: ['src/main/java/test/HelloWorld.java'] as Set)
        grgit.commit(message: 'Adding Hello World')

        def result2 = runTasksSuccessfully('devSnapshot', 'printVersion')

        then:
        result2.standardOutput =~ /Version is 0\.1\.0-dev\..*\+myfeature/
    }

    def 'travisci model using lastTag'() {
        when:
        writeHelloWorld('test')
        grgit.add(patterns: ['src/main/java/test/HelloWorld.java'] as Set)
        grgit.commit(message: 'Adding Hello World')
        grgit.tag.add(name: 'v0.3.0')

        def results = runTasksSuccessfully('-Prelease.useLastTag=true', 'final', 'printVersion')

        then:
        results.wasExecuted(':build')
        !results.wasExecuted(':artifactoryUpload')
        !results.wasUpToDate(':bintrayUpload')
        results.standardOutput.contains("Version is 0.3.0")
    }

    def 'travisci snapshot model'() {
        when:
        writeHelloWorld('test')
        grgit.add(patterns: ['src/main/java/test/HelloWorld.java'] as Set)
        grgit.commit(message: 'Adding Hello World')
        grgit.tag.add(name: 'v0.3.0')

        def results = runTasksSuccessfully('-Prelease.travisci=true', 'snapshot', 'printVersion')

        then:
        results.wasExecuted(':build')
        results.standardOutput.contains("Version is 0.4.0-SNAPSHOT")
    }
}
