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

/**
 * Establish JMH
 */
class RxjavaPerformancePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        // Facets
        project.plugins.apply(JavaBasePlugin)
        def facetPlugin = (NebulaFacetPlugin) project.plugins.apply(NebulaFacetPlugin)
        facetPlugin.extension.create('perf')

        project.plugins.apply(ShadowPlugin) // Applies JavaPlugin :-(

        // Requires JavaPlugin, to get the ShadowJavaPlugin
        project.plugins.apply(JavaPlugin)

        project.plugins.withType(JavaBasePlugin) {
            JavaPluginConvention convention = project.convention.getPlugin(JavaPluginConvention)

            ShadowJar shadowJar = project.tasks.getByName(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME)
            shadowJar.from(convention.sourceSets.perf.output)
            shadowJar.from(project.configurations.perfRuntime)
            shadowJar.classifier = 'benchmarks'
            shadowJar.manifest {
                attributes("Main-Class": "org.openjdk.jmh.Main")
            }

            project.configurations {
                // Configuration to hold jar and dependencies
                benchmarks
            }
            // Make sure this is built with "build"
            project.artifacts.add('archives', shadowJar)
            CustomComponentPlugin.addArtifact(project, 'shadow', shadowJar, 'jar', project.configurations.perfRuntime)
        }

    }
}
