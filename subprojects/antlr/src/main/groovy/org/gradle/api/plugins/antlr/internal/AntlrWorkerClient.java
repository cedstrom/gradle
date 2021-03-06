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

package org.gradle.api.plugins.antlr.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AntlrWorkerClient implements AntlrWorkerClientProtocol {

    private static final Logger LOGGER = LoggerFactory.getLogger(AntlrWorkerClient.class);

    private AntlrResult result;

    public void executed(AntlrResult result) {
        this.result = result;
    }

    public AntlrResult getResult() {
        return result;
    }
}