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

package org.gradle.platform.base.internal;

import com.google.common.collect.Lists;
import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer;
import org.gradle.api.specs.Spec;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.platform.base.Platform;
import org.gradle.platform.base.PlatformContainer;
import org.gradle.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class DefaultPlatformContainer extends DefaultPolymorphicDomainObjectContainer<Platform> implements PlatformContainer {

    private List<Platform> searchOrder = new ArrayList<Platform>();

    public DefaultPlatformContainer(Instantiator instantiator) {
        super(Platform.class, instantiator);
        whenObjectAdded(new Action<Platform>() {
            public void execute(Platform platform) {
                searchOrder.add(platform);
            }
        });
        whenObjectRemoved(new Action<Platform>() {
            public void execute(Platform platform) {
                searchOrder.remove(platform);
            }
        });
    }

    public <T extends Platform> List<T> chooseFromTargets(Class<T> type, List<String> targets) {
        NamedDomainObjectSet<T> allWithType = withType(type);

        List<T> matching = Lists.newArrayList();
        final List<String> notFound = Lists.newArrayList(targets);
        CollectionUtils.filter(allWithType, matching, new Spec<T>() {
            public boolean isSatisfiedBy(T element) {
                return notFound.remove(element.getName());
            }
        });

        if (notFound.size() == 1) {
            throw new InvalidUserDataException(String.format("Invalid %s: %s", type.getSimpleName(), notFound.get(0)));
        } else if (notFound.size() > 1) {
            throw new InvalidUserDataException(String.format("Invalid %ss: %s", type.getSimpleName(), notFound));
        }
        return matching;
    }

}