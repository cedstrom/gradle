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

package org.gradle.language.scala.internal.toolchain;

import org.gradle.api.JavaVersion;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.project.IsolatedAntBuilder;
import org.gradle.api.internal.tasks.compile.JavaCompilerFactory;
import org.gradle.api.internal.tasks.compile.daemon.CompilerDaemonManager;
import org.gradle.language.scala.platform.ScalaPlatform;
import org.gradle.platform.base.internal.toolchain.ToolProvider;

public class DefaultScalaToolChain implements ScalaToolChainInternal {
    private ProjectFinder projectFinder;
    private CompilerDaemonManager compilerDaemonManager;
    private JavaCompilerFactory javaCompilerFactory;
    private final IsolatedAntBuilder isolatedAntBuilder;
    private final ConfigurationContainer configurationContainer;
    private final DependencyHandler dependencyHandler;
    private final JavaVersion javaVersion;

    public DefaultScalaToolChain(ProjectFinder projectFinder, CompilerDaemonManager compilerDaemonManager, JavaCompilerFactory javaCompilerFactory, IsolatedAntBuilder isolatedAntBuilder, ConfigurationContainer configurationContainer, DependencyHandler dependencyHandler) {
        this.projectFinder = projectFinder;
        this.compilerDaemonManager = compilerDaemonManager;
        this.javaCompilerFactory = javaCompilerFactory;
        this.isolatedAntBuilder = isolatedAntBuilder;
        this.configurationContainer = configurationContainer;
        this.dependencyHandler = dependencyHandler;
        this.javaVersion = JavaVersion.current();
    }

    public String getName() {
        return String.format("Scala Toolchain");
    }

    public String getDisplayName() {
        return String.format("Scala Toolchain (JDK %s (%s))", javaVersion.getMajorVersion(), javaVersion);
    }

    public ToolProvider select(ScalaPlatform targetPlatform) {
        return new ScalaToolProvider(projectFinder, compilerDaemonManager, javaCompilerFactory, isolatedAntBuilder, dependencyHandler, configurationContainer, targetPlatform.getScalaVersion());
    }
}
