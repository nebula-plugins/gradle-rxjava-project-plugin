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
                    classpath 'com.android.tools.build:gradle:2.3.0'
                }
            }

            repositories {
                jcenter()
            }

            ${applyPlugin(RxjavaProjectPlugin)}
            apply plugin: 'com.android.application'

            android {
                compileSdkVersion 25
                buildToolsVersion '25'

                defaultConfig {
                    minSdkVersion 14
                    targetSdkVersion 25
                    versionCode 1
                    versionName '1.0'
                }
                buildTypes {
                    release {
                        minifyEnabled false
                    }
                }
            }
        """.stripIndent()

        file("src/main/AndroidManifest.xml") << """\
        <?xml version="1.0" encoding="UTF-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
            package="com.example.android.basiccontactables"
            android:versionCode="1"
            android:versionName="1.0" >

            <uses-permission android:name="android.permission.READ_CONTACTS"/>
            <!-- Min/target SDK versions (<uses-sdk>) managed by build.gradle -->
            <permission android:name="android"></permission>

            <application
                android:allowBackup="true"
                android:icon="@drawable/ic_launcher"
                android:label="@string/app_name"
                android:theme="@style/Theme.Sample" >
                <activity
                    android:name="com.example.android.basiccontactables.MainActivity"
                    android:label="@string/app_name"
                    android:launchMode="singleTop">
                    <meta-data
                        android:name="android.app.searchable"
                        android:resource="@xml/searchable" />
                    <intent-filter>
                        <action android:name="android.intent.action.SEARCH" />
                    </intent-filter>
                    <intent-filter>
                        <action android:name="android.intent.action.MAIN" />
                        <category android:name="android.intent.category.LAUNCHER" />
                    </intent-filter>
                </activity>
            </application>
        </manifest>
        """.stripIndent()
    }

    def 'android plugin can be applied'() {
        when:
        def result = runTasksSuccessfully('dependencies')

        then:
        result.standardOutput.contains('android')
    }
}
