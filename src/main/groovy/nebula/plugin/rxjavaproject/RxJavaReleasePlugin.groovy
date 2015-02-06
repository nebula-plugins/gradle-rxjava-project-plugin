package nebula.plugin.rxjavaproject

import nebula.plugin.release.NetflixOssStrategies
import nebula.plugin.release.OverrideStrategies
import nebula.plugin.release.ReleasePlugin
import org.ajoberstar.gradle.git.release.base.ReleasePluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph

class RxJavaReleasePlugin  implements Plugin<Project> {
    static final String TRAVIS_CI = 'release.travisci'
    Project project

    @Override
    void apply(Project project) {
        this.project = project

        project.plugins.apply(ReleasePlugin)

        ReleasePluginExtension releaseExtension = project.extensions.findByType(ReleasePluginExtension)
        releaseExtension.with {
            defaultVersionStrategy = NetflixOssStrategies.SNAPSHOT
        }

        // Wire tasks
        project.tasks.matching { it.name == 'bintrayUpload' || it.name == 'artifactoryPublish'}.all { Task task ->
            task.mustRunAfter('build')
            project.rootProject.tasks.release.dependsOn(task)
        }

        project.tasks.matching { it.name == 'bintrayUpload' }.all { Task task ->
            project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
                task.onlyIf {
                    graph.hasTask(':final') || graph.hasTask(':candidate')
                }
            }
        }

        project.tasks.matching { it.name == 'artifactoryPublish'}.all { Task task ->
            project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
                task.onlyIf {
                    graph.hasTask(":snapshot")
                }
            }
        }

        if (project.hasProperty(TRAVIS_CI) && project.property(TRAVIS_CI).toBoolean()) {
            project.tasks.release.deleteAllActions() // remove tagging op on travisci
            project.tasks.prepare.deleteAllActions()
        }
    }
}
