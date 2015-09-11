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

import nebula.test.IntegrationSpec
import org.ajoberstar.grgit.Grgit
import spock.lang.Ignore

import java.util.jar.Attributes
import java.util.jar.JarFile

class RxjavaProjectPluginLauncherSpec extends IntegrationSpec {

    Grgit originGit

    def setup() {
        writeHelloWorld('reactivex')
        createFile('src/examples/java/Example.java') << 'public class Example {}'
        createFile('src/perf/java/Perf.java') << 'public class Perf {}'

        buildFile << """
            ${applyPlugin(RxjavaProjectPlugin)}
            apply plugin: 'java'
            license {
                ignoreFailures = true
            }
            publishing {
                repositories {
                    maven {
                        name 'test'
                        url 'build/testmaven'
                    }
                }
            }
        """.stripIndent()

        new File(projectDir, '.gitignore') << """
            .gradle-test-kit
            .gradle
            build/
            """.stripIndent()

        originGit = Grgit.init(dir: projectDir)
        originGit.add(patterns: ["build.gradle", 'settings.gradle', '.gitignore', 'src'] as Set)
        originGit.commit(message: 'Initial checkout')
        originGit.remote.add(name: 'origin', url: 'https://test.git.host/project/repo.git')
    }

    def cleanup() {
        if(originGit) {
            originGit.close()
        }
    }

    def 'stand it all up'() {
        when:
        def result = runTasksSuccessfully('build', 'publishNebulaPublicationToTestRepository')

        then:
        fileExists('build/classes/main/reactivex/HelloWorld.class')
        // 0.1.0-dev.1.uncommitted+59d316c
        def snapshotVer = "0.1.0-SNAPSHOT"
        fileExists("build/libs/stand-it-all-up-${snapshotVer}-javadoc.jar")
        fileExists("build/libs/stand-it-all-up-${snapshotVer}-sources.jar")
        fileExists("build/libs/stand-it-all-up-${snapshotVer}.jar")

        def manifest = getManifest("build/libs/stand-it-all-up-${snapshotVer}.jar")
        manifest['Module-Email'] == 'benjchristensen@netflix.com'

        result.wasExecuted(':compileExamplesJava')
        fileExists('build/classes/examples/Example.class')

        result.wasExecuted(':compilePerfJava')
        fileExists('build/classes/perf/Perf.class')
        fileExists("build/libs/stand-it-all-up-${snapshotVer}-benchmarks.jar")

        result.wasExecuted(':javadoc')
        fileExists('build/docs/javadoc/index.html')
        new File(projectDir, 'build/docs/javadoc/index.html').text.contains("<title>RxJava Javadoc ${snapshotVer}</title>")
        def jmhManifest = getManifest("build/libs/stand-it-all-up-${snapshotVer}-benchmarks.jar")
        jmhManifest['Main-Class'] == 'org.openjdk.jmh.Main'
    }

    def 'dev build with changes'() {
        setup:
        new File(projectDir, 'gradle.properties') << "counter=1"
        // Don't commit this file, we want a local untracked changed

        when:
        runTasksSuccessfully('build')

        then:
        fileExists("build/libs/dev-build-with-changes-0.1.0-SNAPSHOT.jar")
    }

    @Ignore
    def Map<String,String> getManifest(String jarPath) {
        def jmhJar = new JarFile(new File(projectDir, jarPath))
        Attributes attrs = jmhJar.manifest.mainAttributes
        attrs.keySet().collectEntries { Object key ->
            return [key.toString(), attrs.getValue(key)]
        }
    }
}
