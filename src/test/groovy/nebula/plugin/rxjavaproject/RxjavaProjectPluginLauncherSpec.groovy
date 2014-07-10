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
import spock.lang.Ignore

import java.util.jar.Attributes
import java.util.jar.JarFile

class RxjavaProjectPluginLauncherSpec extends IntegrationSpec {


    def setup() {
        useToolingApi = false
        writeHelloWorld('reactivex')
        createFile('src/examples/java/Example.java') << 'public class Example {}'
        createFile('src/perf/java/Perf.java') << 'public class Perf {}'

        createFile('gradle.properties') << 'version=1.0.0-SNAPSHOT'
        buildFile << """
            ${applyPlugin(RxjavaProjectPlugin)}
            apply plugin: 'java'
        """.stripIndent()
    }

    def 'stand it all up'() {
        when:
        def result = runTasksSuccessfully('build')

        then:
        fileExists('build/classes/main/reactivex/HelloWorld.class')
        fileExists('build/libs/stand-it-all-up-1.0.0-SNAPSHOT-javadoc.jar')
        fileExists('build/libs/stand-it-all-up-1.0.0-SNAPSHOT-sources.jar')
        fileExists('build/libs/stand-it-all-up-1.0.0-SNAPSHOT-tests.jar')
        fileExists('build/libs/stand-it-all-up-1.0.0-SNAPSHOT.jar')

        def manifest = getManifest('build/libs/stand-it-all-up-1.0.0-SNAPSHOT.jar')
        manifest['Module-Email'] == 'benjchristensen@netflix.com'

        result.wasExecuted(':compileExamplesJava')
        fileExists('build/classes/examples/Example.class')

        result.wasExecuted(':compilePerfJava')
        fileExists('build/classes/perf/Perf.class')
        fileExists('build/libs/stand-it-all-up-1.0.0-SNAPSHOT-benchmarks.jar')

        result.wasExecuted(':javadoc')
        fileExists('build/docs/javadoc/index.html')
        new File(projectDir, 'build/docs/javadoc/index.html').text.contains('<title>RxJava Javadoc 1.0.0-SNAPSHOT</title>')
        def jmhManifest = getManifest('build/libs/stand-it-all-up-1.0.0-SNAPSHOT-benchmarks.jar')
        jmhManifest['Main-Class'] == 'org.openjdk.jmh.Main'
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
