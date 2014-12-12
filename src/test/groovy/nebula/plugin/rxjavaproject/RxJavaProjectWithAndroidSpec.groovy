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
