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

import nebula.core.ProjectType
import nebula.plugin.release.NetflixOssStrategies
import nebula.plugin.release.ReleasePlugin
import org.ajoberstar.gradle.git.release.base.ReleasePluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class RxJavaReleasePlugin  implements Plugin<Project> {
    Project project

    @Override
    void apply(Project project) {
        this.project = project

        project.plugins.apply(ReleasePlugin)

        ProjectType projectType = new ProjectType(project)
        if (projectType.isRootProject) {
            ReleasePluginExtension releaseExtension = project.extensions.findByType(ReleasePluginExtension)
            releaseExtension.with {
                defaultVersionStrategy = NetflixOssStrategies.SNAPSHOT
            }
        }
    }
}
