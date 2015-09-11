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

import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin
import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import nebula.plugin.publishing.maven.MavenPublishPlugin
import nebula.plugin.responsible.NebulaFacetPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.JavaExec

/**
 * Establish JMH
 */
class RxjavaPerformancePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        // The Shadow plugin really likes working directly with the JavaPlugin. It recent versions it reacts well to the
        // JavaPlugin, but it's probably best for us to avoid all the performance code if we're not doing the full blown
        // Shadow plugin.

        project.plugins.withType(JavaPlugin) {
            project.plugins.apply(ShadowPlugin)

            def facetPlugin = (NebulaFacetPlugin) project.plugins.apply(NebulaFacetPlugin)
            facetPlugin.extension.create('perf')

            project.dependencies {
                perfCompile 'org.openjdk.jmh:jmh-core:0.9'
                perfCompile 'org.openjdk.jmh:jmh-generator-annprocess:0.9'
            }

            project.plugins.withType(JavaBasePlugin) {
                JavaPluginConvention convention = project.convention.getPlugin(JavaPluginConvention)

                ShadowJar shadowJar = project.tasks.getByName(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME)
                shadowJar.from(convention.sourceSets.perf.output)
                shadowJar.from(project.configurations.perfRuntime)
                shadowJar.classifier = 'benchmarks'

                // Not using applyManifest since it'll inherit the BND/OSGI cruft
                shadowJar.doFirst {
                    shadowJar.manifest.attributes.put("Main-Class", "org.openjdk.jmh.Main")
                }

                project.configurations {
                    // Configuration to hold jar and dependencies
                    benchmarks
                }
                // Make sure this is built with "build"
                project.artifacts.add('archives', shadowJar)
                project.plugins.withType(MavenPublishPlugin) {
                    project.publishing {
                        publications {
                            nebula(MavenPublication) {
                                artifact project.tasks.shadowJar
                            }
                        }
                    }
                }
            }

            /**
             * By default: Run without arguments this will execute all benchmarks that are found (can take a long time).
             *
             * Optionally pass arguments for custom execution. Example:
             *
             *  ../gradlew benchmarks '-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*OperatorSerializePerf.*'
             *
             * To see all options:
             *
             *  ../gradlew benchmarks '-Pjmh=-h'
             */
            project.task(type: JavaExec, 'benchmarks') {
                main = 'org.openjdk.jmh.Main'
                classpath = project.sourceSets.perf.runtimeClasspath
                maxHeapSize = "512m"
                jvmArgs '-XX:+UnlockCommercialFeatures'
                jvmArgs '-XX:+FlightRecorder'

                if (project.hasProperty('jmh')) {
                    args(project.jmh.split(' '))
                } else {
                    //args '-h' // help output
                    args '-f' // fork
                    args '1'
                    args '-wi' // warmup iterations
                    args '5'
                    args '-i' // test iterations
                    args '5'
                    args '-r' // time per execution in seconds
                    args '5'
                    //args '-prof' // profilers
                    //args 'HS_GC' // HotSpot (tm) memory manager (GC) profiling via implementation-specific MBeans
                    //args 'HS_RT' // HotSpot (tm) runtime profiling via implementation-specific MBeans
                    //args 'HS_THR' // HotSpot (tm) threading subsystem via implementation-specific MBeans
                    //args 'HS_COMP' // HotSpot (tm) JIT compiler profiling via implementation-specific MBeans
                    //args 'HS_CL' // HotSpot (tm) classloader profiling via implementation-specific MBeans
                    //args 'STACK' // Simple and naive Java stack profiler
                }
            }
        }
    }
}
