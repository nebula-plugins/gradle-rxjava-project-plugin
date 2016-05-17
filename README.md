gradle-rxjava-project-plugin
==============
![Support Status](https://img.shields.io/badge/Nebula-internal-lightgray.svg)
[![Build Status](https://travis-ci.org/nebula-plugins/gradle-rxjava-project-plugin.svg?branch=master)](https://travis-ci.org/nebula-plugins/gradle-rxjava-project-plugin)
[![Coverage Status](https://coveralls.io/repos/nebula-plugins/gradle-rxjava-project-plugin/badge.svg?branch=master&service=github)](https://coveralls.io/github/nebula-plugins/gradle-rxjava-project-plugin?branch=master)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/nebula-plugins/gradle-rxjava-project-plugin?utm_source=badgeutm_medium=badgeutm_campaign=pr-badge)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/gradle-rxjava-project-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)


This plugin is to support projects in the ReactiveX org (and it isn't meant to be used elsewhere). It is at its essence
just a combination of other plugins that are common to all ReactiveX projects, with some additional configuration. The 
primary responsibilities to:

  * Provide release process
  * Configure publishing
  * Recommend license headers
  * Create a performance module for testing with jmh
  * Configure modules to be as OSGI modules
  * Set defaults for javadoc formatting
  
This project could be used as an example of how a "project plugin" could work. A "project plugin" is a Gradle that 
provides consistency across many projects, e.g. in a Github org or an enterprise.

# Plugins Used

For reference, these are Gradle-related modules used:

  * com.netflix.nebula:nebula-project-plugin
  * com.netflix.nebula:nebula-bintray-plugin' wraps gradle-bintray-plugin with different defaults and adding OJO support and multi-module support.
  * com.netflix.nebula:nebula-publishing-plugin for producing a jar, source jar, javadoc jar with metadata about how it was produced.
  * com.github.jengelman.gradle.plugins:shadow for generating a binary for the performances tests to run from.
  * nl.javadude.gradle.plugins:license-gradle-plugin for license recommendations
  * org.ajoberstar:gradle-git:1.3.0 for release process.

# Applying the Plugin

To include, add the following to your build.gradle

    plugins {
      id 'nebula.rxjava-project' version '3.0.0'
    }
    
    subprojects { // if you have subprojects
      apply plugin: 'nebula.rxjava-project'
    }

or if older than gradle 2.1

    buildscript {
      repositories { jcenter() }
      dependencies { classpath 'com.netflix.nebula:gradle-rxjava-project-plugin:3.+' }
    }

    allprojects {
        apply plugin: 'nebula.rxjava-project'
    }

# Parameters

* `-Prelease.travisci=true` - this will disable tagging

# Variants

## License Check

By default the license check is on. To turn it off:

    license {
        ignoreFailures = true
    }

Gradle Compatibility Tested
---------------------------

Built with Oracle JDK7
Tested with Oracle JDK8

| Gradle Version | Works |
| :------------: | :---: |
| 2.2.1          | yes   |
| 2.3            | yes   |
| 2.4            | yes   |
| 2.5            | yes   |
| 2.6            | yes   |
| 2.7            | yes   |

LICENSE
=======

Copyright 2014-2015 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
