package nebula.plugin.rxjavaproject

import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayUploadTask
import nebula.plugin.bintray.BintrayPlugin
import nebula.plugin.info.scm.ScmInfoExtension
import org.gradle.BuildAdapter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Upload
import org.jfrog.gradle.plugin.artifactory.task.BuildInfoBaseTask

class RxJavaPublishingPlugin  implements Plugin<Project> {

    Project project

    @Override
    void apply(Project project) {
        this.project = project

        boolean dryRun = project.hasProperty('dryRun') && project['dryRun'] as Boolean
        def disable = {
            it.enabled = !dryRun
        }
        project.tasks.withType(BintrayUploadTask, disable)
        project.tasks.withType(Upload, disable)
        project.tasks.withType(BuildInfoBaseTask, disable)

        project.plugins.apply(BintrayPlugin) // I wish, we would break this apart below so that we can customize it.
        project.plugins.apply(PublishingPlugin)

        project.tasks.getByName('verifyReleaseStatus').actions.clear()
        project.tasks.getByName('verifySnapshotStatus').actions.clear()

        // Ripping apart NebulaBintrayPublishingPlugin
//        def bintrayUpload = new NebulaBintrayPublishingPlugin().addBintray(project)
//        project.plugins.withType(NebulaBaseMavenPublishingPlugin) { NebulaBaseMavenPublishingPlugin mavenPublishingPlugin ->
//            mavenPublishingPlugin.withMavenPublication { MavenPublicationInternal mavenPublication ->
//                // Ensure everything is built before uploading
//                bintrayUpload.dependsOn(mavenPublication.publishableFiles)
//            }
//        }

        // Configuring for us
        BintrayExtension bintray = project.extensions.getByType(BintrayExtension)
        bintray.pkg.repo = 'RxJava'
        bintray.pkg.userOrg = 'reactivex'
        bintray.pkg.licenses = ['Apache-2.0'] //TBD
        bintray.pkg.labels = ['rxjava', 'reactivex']

        BintrayUploadTask bintrayUpload = (BintrayUploadTask) project.tasks.find { it instanceof BintrayUploadTask }
        bintrayUpload.doFirst {
            ScmInfoExtension scmInfo = project.extensions.findByType(ScmInfoExtension)
            // We have to change the task directly, since they already copied from the extension in an afterEvaluate

            if (scmInfo) {
                // Assuming scmInfo.origin is something like git@github.com:reactivex/rxjava-core.git
                bintrayUpload.packageName = calculateRepoFromOrigin(scmInfo.origin) ?: project.rootProject.name

                def url = calculateUrlFromOrigin(scmInfo.origin)
                bintrayUpload.packageWebsiteUrl = url
                bintrayUpload.packageIssueTrackerUrl = "${url}/issues"
                bintrayUpload.packageVcsUrl = "${url}.git"
            }
        }

        // Undo BintrayPlugin. We have to use a BuildListener to be after the bintrayPlugin's buildlistener.
        // I have no idea why they need to depend on a maven local install
        project.gradle.addBuildListener(new BuildAdapter() {
            @Override
            void projectsEvaluated(Gradle gradle) {
                project.tasks.matching {
                    it.name == "publishMavenNebulaPublicationToMavenLocal"
                }.all {
                    bintrayUpload.dependsOn.remove(it)
                }
            }
        })

//        project.plugins.apply(NebulaBintraySyncPublishingPlugin)
//
//        // Instead of NebulaOJOPublishingPlugin
//        def ojo = new NebulaOJOPublishingPlugin()
//        ojo.project = project
//        ojo.applyArtifactory()

    }

    static GIT_PATTERN = /((git|ssh|https?):(\/\/))?(\w+@)?([\w\.]+)([\:\\/])([\w\.@\:\/\-~]+)(\.git)(\/)?/

    /**
     * Convert git syntax of git@github.com:reactivex/rxjava-core.git to https://github.com/reactivex/rxjava-core
     * @param origin
     */
    static String calculateUrlFromOrigin(String origin) {
        def m = origin =~ GIT_PATTERN
        return "https://${m[0][5]}/${m[0][7]}"
    }

    static String calculateRepoFromOrigin(String origin) {
        def m = origin =~ GIT_PATTERN
        String path = m[0][7]
        path.tokenize('/').last()
    }
}