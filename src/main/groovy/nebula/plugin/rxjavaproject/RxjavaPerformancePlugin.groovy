package nebula.plugin.rxjavaproject

import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin
import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import nebula.plugin.publishing.component.CustomComponentPlugin
import nebula.plugin.responsible.NebulaFacetPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.JavaExec

/**
 * Establish JMH
 */
class RxjavaPerformancePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        // Facets
        project.plugins.apply(JavaPlugin)

        project.plugins.apply(ShadowPlugin) // Applies JavaPlugin :-(

        // Requires JavaPlugin, to get the ShadowJavaPlugin
        project.plugins.apply(JavaPlugin)

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
            CustomComponentPlugin.addArtifact(project, 'shadow', shadowJar, 'jar', project.configurations.perfRuntime)
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
                args(jmh.split(' '))
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
