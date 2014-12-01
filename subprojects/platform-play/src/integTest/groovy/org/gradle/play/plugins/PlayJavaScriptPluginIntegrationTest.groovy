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

package org.gradle.play.plugins

import org.gradle.integtests.fixtures.WellBehavedPluginTest
import org.gradle.util.TextUtil

class PlayJavaScriptPluginIntegrationTest extends WellBehavedPluginTest {

    @Override
    String getPluginName() {
        return 'play-javascript'
    }

    def setup() {
        buildFile << """
            plugins {
                id 'play-application'
                id 'play-javascript'
            }
        """
    }

    def "javascript source set appears in component listing"() {
        when:
        succeeds "components"

        then:
        output.contains(TextUtil.toPlatformLineSeparators("""
    JavaScript source 'play:javaScriptSources'
        app
"""))
    }

    def "creates and configures process task when source exists"() {
        buildFile << """
            task checkTasks {
                doLast {
                    def javaScriptProcessTasks = tasks.withType(Copy).matching { it.name == "processPlayBinaryPlayJavaScriptSources" }
                    assert javaScriptProcessTasks.size() == 1
                }
            }
        """

        when:
        file("app/test.js") << "test"

        then:
        succeeds "checkTasks"
    }

    def "does not create process task when source does not exist"() {
        buildFile << """
            task checkTasks {
                doLast {
                    def javaScriptProcessTasks = tasks.withType(Copy).matching { it.name == "processPlayBinaryPlayJavaScriptSources" }
                    assert javaScriptProcessTasks.size() == 0
                }
            }
        """

        expect:
        succeeds "checkTasks"
    }
}
