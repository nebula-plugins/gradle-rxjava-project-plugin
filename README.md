gradle-rxjava-project-plugin
==============

Template for RxJava projects

# Applying the Plugin

To include, add the following to your build.gradle

    buildscript {
      repositories { jcenter() }
      dependencies { classpath 'com.netflix.nebula:gradle-rxjava-project-plugin:1.12.+' }
    }

    allprojects {
        apply plugin: 'rxjava-project'
    }

# Variants

## License Check

By default the license check is on. To turn it off:

    license {
        ignoreFailures = true
    }

