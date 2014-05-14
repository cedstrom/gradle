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

package org.gradle.plugin.use

import org.gradle.api.Project
import org.gradle.api.specs.AndSpec
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.plugin.use.resolve.portal.PluginPortalTestServer
import org.gradle.test.fixtures.plugin.PluginBuilder
import org.junit.Rule

class PluginUseClassVisibilityIntegrationSpec extends AbstractIntegrationSpec {

    public static final String PLUGIN_ID = "org.myplugin"
    public static final String VERSION = "1.0"
    public static final String GROUP = "my"
    public static final String ARTIFACT = "plugin"
    public static final String USE = "plugins { id '$PLUGIN_ID' version '$VERSION' }"

    def pluginBuilder = new PluginBuilder(file(ARTIFACT))

    @Rule
    PluginPortalTestServer portal = new PluginPortalTestServer(executer, mavenRepo)

    def setup() {
        executer.requireGradleHome() // need accurate classloading
        portal.start()
    }

    def "plugin is available via `plugins` container"() {
        publishPlugin()

        buildScript """
            $USE

            task verify << {
                def foundByClass = false
                plugins.withType(pluginClass) { foundByClass = true }
                assert foundByClass
            }
        """

        expect:
        succeeds("verify")
    }

    def "plugin class isn't visible to build script"() {
        publishPlugin()

        buildScript """
            $USE

            task verify << {
                try {
                    getClass().getClassLoader().loadClass("org.gradle.test.TestPlugin")
                    throw new AssertionError("plugin class *is* visible to build script")
                } catch (ClassNotFoundException expected) {}
            }
        """

        expect:
        succeeds("verify")
    }

    def "plugin can access Gradle API classes"() {
        publishPlugin("assert project instanceof ${Project.name}; new ${AndSpec.name}()")

        buildScript USE

        expect:
        succeeds("tasks")
    }

    def "plugin cannot access core Gradle plugin classes"() {
        publishPlugin("getClass().getClassLoader().loadClass('org.gradle.api.plugins.JavaPlugin')")

        buildScript USE

        expect:
        fails("tasks")
    }

    void publishPlugin() {
        publishPlugin("project.ext.pluginApplied = true; project.ext.pluginClass = getClass()")
    }

    void publishPlugin(String impl) {
        portal.expectPluginQuery(PLUGIN_ID, VERSION, GROUP, ARTIFACT, VERSION)
        def module = portal.m2repo.module(GROUP, ARTIFACT, VERSION)
        module.allowAll()

        pluginBuilder.addPlugin(impl, PLUGIN_ID)
        pluginBuilder.publishTo(executer, module.artifactFile)
    }

}
