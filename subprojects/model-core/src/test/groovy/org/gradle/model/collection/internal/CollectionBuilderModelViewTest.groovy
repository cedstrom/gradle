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

package org.gradle.model.collection.internal

import org.gradle.internal.reflect.DirectInstantiator
import org.gradle.model.ModelViewClosedException
import org.gradle.model.collection.CollectionBuilder
import org.gradle.model.internal.core.CollectionBuilderModelView
import org.gradle.model.internal.core.rule.describe.SimpleModelRuleDescriptor
import org.gradle.model.internal.type.ModelType
import spock.lang.Specification

class CollectionBuilderModelViewTest extends Specification {

    def "cannot create items after view is closed"() {
        def builder = Mock(CollectionBuilder)
        def view = new CollectionBuilderModelView(new DirectInstantiator(), ModelType.of(CollectionBuilder), builder, new SimpleModelRuleDescriptor("foo"))
        def instance = view.instance

        when:
        instance.create("foo")

        then:
        1 * builder.create("foo")

        when:
        view.close()

        and:
        instance.create("foo")

        then:
        thrown ModelViewClosedException

        // assume other methods are implemented in the same way
    }
}
