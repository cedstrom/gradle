/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.plugin.internal;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.internal.plugins.PluginRegistry;
import org.gradle.api.plugins.PluginAware;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.classpath.ClassPath;
import org.gradle.internal.classpath.DefaultClassPath;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.plugin.PluginHandler;
import org.gradle.plugin.resolve.PluginResolution;
import org.gradle.plugin.resolve.internal.PluginRegistryPluginResolver;

import java.io.File;
import java.net.URLClassLoader;
import java.util.Set;

public class DefaultPluginHandlerFactory implements PluginHandlerFactory {

    private final PluginRegistry pluginRegistry;
    private final Instantiator instantiator;

    public DefaultPluginHandlerFactory(PluginRegistry pluginRegistry, Instantiator instantiator) {
        this.pluginRegistry = pluginRegistry;
        this.instantiator = instantiator;
    }

    public PluginHandler createPluginHandler(final Object target) {
        if (target instanceof PluginAware) {
            PluginHandler pluginHandler = new DefaultPluginHandler((PluginAware) target, instantiator, new Action<PluginResolution>() {
                public void execute(PluginResolution pluginResolution) {
                    Set<File> classpathFiles = pluginResolution.resolveClasspath();
                    ClassPath classPath = new DefaultClassPath(classpathFiles);
                    ClassLoader classLoader = new URLClassLoader(classPath.getAsURLArray(), this.getClass().getClassLoader());
                    Class<?> aClass;
                    try {
                        aClass = classLoader.loadClass(pluginResolution.getClassName());
                    } catch (ClassNotFoundException e) {
                        throw UncheckedException.throwAsUncheckedException(e);
                    }
                    ((PluginAware) target).getPlugins().apply((Class<? extends Plugin>) aClass);
                }
            });
            pluginHandler.getResolvers().add(new PluginRegistryPluginResolver(pluginRegistry));
            return pluginHandler;
        } else {
            return new NonPluggableTargetPluginHandler(target);
        }
    }
}