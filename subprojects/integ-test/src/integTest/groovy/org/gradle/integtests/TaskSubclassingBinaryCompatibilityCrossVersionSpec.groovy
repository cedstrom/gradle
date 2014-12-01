/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.integtests

import org.gradle.api.DefaultTask
import org.gradle.api.internal.ConventionTask
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CodeNarc
import org.gradle.api.plugins.quality.FindBugs
import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.War
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.integtests.fixtures.CrossVersionIntegrationSpec
import org.gradle.integtests.fixtures.TargetVersions
import org.gradle.plugins.ear.Ear
import org.gradle.util.GradleVersion

/**
 * Tests that task classes compiled against earlier versions of Gradle are still compatible.
 */
@TargetVersions('0.9-rc-3+')
class TaskSubclassingBinaryCompatibilityCrossVersionSpec extends CrossVersionIntegrationSpec {
    def "can use task subclass compiled using previous Gradle version"() {
        given:
        def taskClasses = [
                DefaultTask,
                SourceTask,
                ConventionTask,
                Copy,
                Sync,
                Zip,
                Jar,
                Tar,
                War,
                Ear,
                Test,
                ScalaCompile,
                GroovyCompile,
//                JavaCompile,
                CodeNarc,
                FindBugs,
                Checkstyle,
                Pmd
        ]

        if (previous.version < GradleVersion.version("1.0-milestone-4")) {
            taskClasses.remove(Ear)
        }
        if (previous.version < GradleVersion.version("1.0-milestone-8")) {
            taskClasses.remove(FindBugs)
            taskClasses.remove(Pmd)
        }
        if (previous.version < GradleVersion.version("1.1")) {
            // Breaking changes were made to Test between 1.0 and 1.1
            taskClasses.remove(Test)
        }


        Map<String, String> subclasses = taskClasses.collectEntries { ["custom" + it.simpleName, it.name] }

        file("producer/build.gradle") << """
            apply plugin: 'groovy'
            dependencies {
                ${previous.version < GradleVersion.version("1.4-rc-1") ? "groovy" : "compile" } localGroovy()
                compile gradleApi()
            }
        """

        file("producer/src/main/groovy/SomePlugin.groovy") << """
            import org.gradle.api.Plugin
            import org.gradle.api.Project

            class SomePlugin implements Plugin<Project> {
                void apply(Project p) { """ <<
                subclasses.collect { "p.tasks.create('${it.key}', ${it.key})" }.join("\n") << """
                }
            }
            """ <<

                subclasses.collect {
                    def className = it.key
                    """class ${className} extends ${it.value} {
    ${className}() {
        // GRADLE-3185
        project.logger.lifecycle('task created')
    }
}"""
                }.join("\n")

        buildFile << """
buildscript {
    dependencies { classpath fileTree(dir: "producer/build/libs", include: '*.jar') }
}

apply plugin: SomePlugin
"""

        expect:
        version previous withTasks 'assemble' inDirectory(file("producer")) run()
        version current withTasks 'tasks' run()
    }
}
