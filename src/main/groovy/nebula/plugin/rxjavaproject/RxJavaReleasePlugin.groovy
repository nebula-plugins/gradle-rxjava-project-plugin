package nebula.plugin.rxjavaproject

import nebula.plugin.publishing.sign.NebulaSignPlugin
import org.ajoberstar.gradle.git.release.GrgitReleasePlugin
import org.ajoberstar.gradle.git.release.GrgitReleasePluginExtension
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.exception.GrgitException
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.text.SimpleDateFormat

class RxJavaReleasePlugin  implements Plugin<Project> {

    Project project
    Grgit grgit

    def FORMAT = new SimpleDateFormat('yyMMddHHmmss')

    @Override
    void apply(Project project) {
        this.project = project

        project.plugins.apply(GrgitReleasePlugin)
        def extension = project.extensions.getByType(GrgitReleasePluginExtension)

        grgit = Grgit.open(project.rootProject.projectDir)

        extension.grgit = grgit
        extension.version.untaggedStages = ['dev'] as SortedSet
        extension.version.taggedStages = ['rc'] as SortedSet
        extension.version.useBuildMetadataForStage = { it == 'dev' }

        // Create snapshot, candidate, and release
        // Use literal tasks name from command line.
        def cliTasks = project.gradle.startParameter.taskNames
        def hasSnapshot = cliTasks.contains('snapshot')
        def hasCandidate = cliTasks.contains('candidate')
        def hasRelease = cliTasks.contains('release')
        if ([hasSnapshot, hasCandidate, hasRelease].count { it } > 2) {
            throw new GradleException("Only snapshot, candidate, or release can be specified.")
        }
        def snapshotTask = project.task(dependsOn: 'release', 'snapshot')
        def candidateTask = project.task(dependsOn: 'release', 'candidate')

        if (hasCandidate) {
            project.ext['release.stage'] = 'rc'
            extension.releaseTasks = ['build', 'bintrayUpload']
        } else if (hasRelease) {
            project.ext['release.stage'] = 'final'
            extension.releaseTasks = ['build', 'bintrayUpload']
        } else {
            project.ext['release.stage'] = 'dev'
            extension.releaseTasks = ['build', 'artifactoryPublish']
            extension.version.createBuildMetadata = {
                if (grgit.branch.current.name == 'master') {
                    "SNAPSHOT"
                } else {
                    // Feature branches
                    def branchPath = grgit.branch.current.name
                    def branchName = branchPath.substring(branchPath.indexOf('/')+1)
                    "${branchName}-SNAPSHOT" // grgit.head().abbreviatedId
//                    def current = FORMAT.format(new Date())
//                    "${branchName}.${current}-SNAPSHOT"
                }
            }
        }
        project.plugins.withType(NebulaSignPlugin) {
            project.tasks.getByName('bintrayUpload').dependsOn('preparePublish')
            project.tasks.getByName('artifactoryPublish').dependsOn('preparePublish')
        }
        project.tasks.matching { it.name == 'bintrayUpload' || it.name == 'artifactoryPublish'}.all {
            it.mustRunAfter('build')
        }

        extension.generateTagMessage = { version -> // default is "Release of ${version}"
            StringBuilder builder = new StringBuilder()
            builder.append('Release of ')
            builder.append(version)
            boolean firstCommit = true
            try {
                grgit.log {
                    range "v${version.nearest.normal.toString()}^{commit}", 'HEAD'
                }.inject(builder) { bldr, commit ->
                    if (firstCommit) {
                        builder.append('\n\n')
                        firstCommit = false
                    }
                    bldr.append('- ')
                    bldr.append(commit.shortMessage)
                    bldr.append('\n')
                }
            } catch(GrgitException ge) {
                // Unable to get a log, it might just be our first time
            }
            builder.toString()
        }
    }
}
