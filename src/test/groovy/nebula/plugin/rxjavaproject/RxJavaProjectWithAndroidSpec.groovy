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

import nebula.test.IntegrationSpec
import org.ajoberstar.grgit.Grgit

class RxJavaProjectWithAndroidSpec extends IntegrationSpec {
    def setup() {
        // We require to be a git repo
        def originGit = Grgit.init(dir: projectDir)
        originGit.add(patterns: ['build.gradle', '.gitignore'] as Set)
        originGit.commit(message: 'Initial checkout')
        originGit.close()

        buildFile << """\
            buildscript {
                repositories { jcenter() }
                dependencies {
                    classpath 'com.android.tools.build:gradle:1.+'
                }
            }

            repositories {
                jcenter()
            }

            ${applyPlugin(RxjavaProjectPlugin)}
            apply plugin: 'com.android.application'

            android {
                compileSdkVersion 20
                buildToolsVersion "20"

                defaultConfig {
                    minSdkVersion 14
                    targetSdkVersion 20
                    versionCode 1
                    versionName "1.0"
                }
                buildTypes {
                    release {
                        minifyEnabled false
                    }
                }
            }
        """.stripIndent()
    }

    def 'android plugin can be applied'() {

        expect:
        runTasksSuccessfully('androidDependencies')

    }
}
