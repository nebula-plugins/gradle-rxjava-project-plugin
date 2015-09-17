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

        project.plugins.apply(BintrayPlugin)
        project.plugins.apply(PublishingPlugin)

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