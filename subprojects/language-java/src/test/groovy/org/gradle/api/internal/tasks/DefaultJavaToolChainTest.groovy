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

package org.gradle.api.internal.tasks
import org.gradle.api.JavaVersion
import org.gradle.api.internal.tasks.compile.JavaCompileSpec
import org.gradle.api.internal.tasks.compile.JavaCompilerFactory
import org.gradle.api.tasks.compile.CompileOptions
import org.gradle.api.tasks.javadoc.internal.JavadocGenerator
import org.gradle.api.tasks.javadoc.internal.JavadocSpec
import org.gradle.jvm.platform.JavaPlatform
import org.gradle.jvm.platform.internal.DefaultJavaPlatform
import org.gradle.language.base.internal.compile.Compiler
import org.gradle.process.internal.ExecActionFactory
import org.gradle.util.TreeVisitor
import spock.lang.Specification

class DefaultJavaToolChainTest extends Specification {
    def javaCompilerFactory = Stub(JavaCompilerFactory)
    def execActionFactory = Stub(ExecActionFactory)
    def toolChain = new DefaultJavaToolChain(javaCompilerFactory, execActionFactory)
    def JavaVersion currentJvm = JavaVersion.current()
    def currentPlatform = platform(currentJvm)

    def "has reasonable string representation"() {
        expect:
        toolChain.name == "JDK${currentJvm}"
        toolChain.displayName == "JDK ${currentJvm.majorVersion} (${currentJvm})"
        toolChain.toString() == toolChain.displayName
    }

    def "creates compiler for JavaCompileSpec"() {
        def options = Stub(CompileOptions)
        def spec = Stub(JavaCompileSpec) {
            getCompileOptions() >> options
        }
        def compiler = Stub(Compiler)

        given:
        javaCompilerFactory.create(options) >> compiler

        expect:
        toolChain.select(currentPlatform).newCompiler(spec) == compiler
    }

    def "creates compiler for JavadocSpec"() {
        expect:
        toolChain.select(currentPlatform).newCompiler(Stub(JavadocSpec)) instanceof JavadocGenerator
    }

    def "creates available tool provider for earlier platform"() {
        def earlierPlatform = platform(JavaVersion.VERSION_1_5)

        when:
        def toolProvider = toolChain.select(earlierPlatform)

        then:
        toolProvider.available

        when:
        TreeVisitor<String> visitor = Mock()
        toolProvider.explain(visitor)

        then:
        0 * _
    }

    def "creates unavailable tool provider for incompatible platform"() {
        def futurePlatform = platform(JavaVersion.VERSION_1_9)
        TreeVisitor<String> visitor = Mock()

        when:
        def toolProvider = toolChain.select(futurePlatform)

        then:
        !toolProvider.available

        when:
        toolProvider.explain(visitor)

        then:
        1 * visitor.node("Could not target platform: '${futurePlatform}' using tool chain: '${toolChain}'.")
        0 * _
    }

    private static JavaPlatform platform(JavaVersion javaVersion) {
        return new DefaultJavaPlatform(javaVersion)
    }
}
