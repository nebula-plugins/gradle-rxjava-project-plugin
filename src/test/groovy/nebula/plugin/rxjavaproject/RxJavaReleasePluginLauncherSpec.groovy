package nebula.plugin.rxjavaproject

import com.google.common.io.Files
import nebula.plugin.publishing.maven.NebulaMavenPublishingPlugin
import nebula.test.IntegrationSpec
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import org.ajoberstar.grgit.operation.BranchAddOp
import org.ajoberstar.grgit.operation.BranchChangeOp
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.lib.StoredConfig
import org.gradle.api.plugins.JavaPlugin
import spock.lang.Ignore

class RxJavaReleasePluginLauncherSpec extends IntegrationSpec {

    Grgit grgit
    Grgit originGit

    def setup() {
        useToolingApi = false

        // Place to push to
        def origin = new File(projectDir.parent, "${projectDir.name}.git")
        origin.mkdirs()

        ['build.gradle', 'settings.gradle'].each {
            Files.move(new File(projectDir, it), new File(origin, it))
        }
        // Should be empty now.

        originGit = Grgit.init(dir: origin)
        originGit.add(patterns: ["build.gradle", 'settings.gradle', '.gitignore'] as Set)
        originGit.commit(message: 'Initial checkout')

        grgit = Grgit.clone(dir: projectDir, uri: origin.absolutePath)

        new File(projectDir, '.gitignore') << """
            .gradle-test-kit
            .gradle
            build/
            """.stripIndent()

        buildFile << """
            ext.dryRun = true
            group = 'test'
            ${applyPlugin(NebulaMavenPublishingPlugin)}
            ${applyPlugin(RxJavaPublishingPlugin)}
            ${applyPlugin(RxJavaReleasePlugin)}
            ${applyPlugin(JavaPlugin)}

            task printVersion << {
                println "Version is \${version}"
            }
            """.stripIndent()

        grgit.add(patterns: ['build.gradle', '.gitignore'] as Set)
        grgit.commit(message: 'Setup')
        grgit.push()
    }

    def cleanup() {
        if (grgit) grgit.close()
        if (originGit) originGit.close()
    }

    def 'perform release'() {
        when:
        def results = runTasksSuccessfully('release')

        then:
        results.wasExecuted(':build')
        !results.wasExecuted(':artifactoryUpload')
        !results.wasUpToDate(':bintrayUpload')

        //Grgit originGit = Grgit.open(origin)
        def tags = originGit.tag.list()
        Tag tag001 = tags.find { Tag tag -> tag.name == 'v0.0.1' }
        tag001
        tag001.fullMessage == 'Release of 0.0.1'

        when:
        writeHelloWorld('test')
        grgit.add(patterns: ['src/main/java/test/HelloWorld.java'] as Set)
        grgit.commit(message: 'Adding Hello World')

        runTasksSuccessfully('release')

        then:
        def tags2 = originGit.tag.list()
        def tag002 = tags2.find { Tag tag -> tag.name == 'v0.0.2'}
        tag002
        tag002.fullMessage == 'Release of 0.0.2\n\n- Adding Hello World\n'
    }

    def 'perform candidate'() {
        when:
        def results = runTasksSuccessfully('candidate')

        then:
        def tags = originGit.tag.list()
        tags.find { Tag tag -> tag.name == 'v0.0.1-rc.1' }

        when:
        writeHelloWorld('test')
        grgit.add(patterns: ['src/main/java/test/HelloWorld.java'] as Set)
        grgit.commit(message: 'Adding Hello World')

        runTasksSuccessfully('candidate')

        then:
        def tags2 = originGit.tag.list()
        tags2.find { Tag tag -> tag.name == 'v0.0.1-rc.2'}
    }

    @Ignore("Can't get -SNAPSHOT in the string")
    def 'perform snapshots'() {
        when:
        def results = runTasksSuccessfully('candidate')

        then:
        def tags = originGit.tag.list()
        tags.collect { it.name }.any {it == 'v0.0.1-rc.1'}

        when:
        writeHelloWorld('test')
        grgit.add(patterns: ['src/main/java/test/HelloWorld.java'] as Set)
        grgit.commit(message: 'Adding Hello World')

        def result = runTasksSuccessfully('snapshot', 'printVersion')

        then:
        result.standardOutput.contains("Version is v0.0.1-dev.3-SNAPSHOT")
    }

    def 'perform snapshots from branch'() {

        when:
        def results = runTasksSuccessfully('candidate')

        then:
        def tags = originGit.tag.list()
        tags.collect { it.name }.any {it == 'v0.0.1-rc.1'}

        when:
        writeHelloWorld('test')

        grgit.branch.add(name: 'feature/myfeature', startPoint: 'master', mode: BranchAddOp.Mode.TRACK)
        grgit.checkout(branch: 'feature/myfeature')

        grgit.add(patterns: ['src/main/java/test/HelloWorld.java'] as Set)
        grgit.commit(message: 'Adding Hello World')

        def result = runTasksSuccessfully('snapshot', 'printVersion')

        then:
        result.standardOutput =~ /0\.0\.1-dev\.3\+myfeature-SNAPSHOT/ // Not a fan of this, I want to remove "dev.3+"
    }

    // TODO Test failure cases.
}
