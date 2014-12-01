/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.play.tasks

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.test.fixtures.archive.JarTestFixture


class ProcessJavaScriptIntegrationTest extends AbstractIntegrationSpec {
    def setup() {
        buildFile << """
            plugins {
                id 'play-application'
                id 'play-javascript'
            }

            repositories{
                jcenter()
                maven{
                    name = "typesafe-maven-release"
                    url = "https://repo.typesafe.com/typesafe/maven-releases"
                }
            }
        """
    }

    def "processes default javascript source set as part of play application build"() {
        given:
        file("app/test.js") << "alert('this is a test!');"

        when:
        succeeds "assemble"

        then:
        executedAndNotSkipped(
                ":processPlayBinaryPlayJavaScriptSources",
                ":createPlayBinaryJar",
                ":playBinary")
        file("build/playBinary/javascript/test.js").exists()
        jar("build/jars/play/playBinary.jar").containsDescendants(
                "test.js"
        )

        // Up-to-date works
        when:
        succeeds "assemble"

        then:
        skipped(":processPlayBinaryPlayJavaScriptSources",
                ":createPlayBinaryJar",
                ":playBinary")

        // Detects missing output
        when:
        file("build/playBinary/javascript/test.js").delete()
        file("build/jars/play/playBinary.jar").delete()
        succeeds "assemble"

        then:
        executedAndNotSkipped(
                ":processPlayBinaryPlayJavaScriptSources",
                ":createPlayBinaryJar",
                ":playBinary")
        file("build/playBinary/javascript/test.js").exists()

        // Detects changed input
        when:
        file("app/test.js") << "alert('this is a change!');"
        succeeds "assemble"

        then:
        executedAndNotSkipped(":processPlayBinaryPlayJavaScriptSources",
                 ":createPlayBinaryJar",
                 ":playBinary")
    }

    def "processes multiple javascript source sets as part of play application build" () {
        given:
        file("app/test1.js") << "alert('this is a test!');"
        file("extra/javascripts/test2.js") << "alert('this is a test!');"
        file("src/play/anotherJavaScript/a/b/c/test3.js") << "alert('this is a test!');"

        buildFile << """
            model {
                components {
                    play {
                        sources {
                            extraJavaScript(JavaScriptSourceSet) {
                                source.srcDir "extra"
                            }
                            anotherJavaScript(JavaScriptSourceSet)
                        }
                    }
                }
            }
        """

        when:
        succeeds "assemble"

        then:
        executedAndNotSkipped(
                ":processPlayBinaryPlayJavaScriptSources",
                ":processPlayBinaryPlayExtraJavaScript",
                ":processPlayBinaryPlayAnotherJavaScript",
                ":createPlayBinaryJar",
                ":playBinary")
        file("build/playBinary/javascript/test1.js").exists()
        file("build/playBinary/javascript/javascripts/test2.js").exists()
        file("build/playBinary/javascript/a/b/c/test3.js").exists()
        jar("build/jars/play/playBinary.jar").containsDescendants(
                "test1.js",
                "javascripts/test2.js",
                "a/b/c/test3.js"
        )

        when:
        succeeds "assemble"

        then:
        skipped(":processPlayBinaryPlayJavaScriptSources",
                ":processPlayBinaryPlayExtraJavaScript",
                ":processPlayBinaryPlayAnotherJavaScript",
                ":createPlayBinaryJar",
                ":playBinary")
    }

    JarTestFixture jar(String fileName) {
        new JarTestFixture(file(fileName))
    }
}
