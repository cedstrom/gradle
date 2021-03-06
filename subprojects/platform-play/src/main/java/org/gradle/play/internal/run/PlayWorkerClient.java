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

package org.gradle.play.internal.run;

import org.gradle.internal.UncheckedException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

public class PlayWorkerClient implements PlayRunWorkerClientProtocol {

    private final BlockingQueue<PlayAppLifecycleUpdate> startEvent = new SynchronousQueue<PlayAppLifecycleUpdate>();
    private final BlockingQueue<PlayAppLifecycleUpdate> stopEvent = new SynchronousQueue<PlayAppLifecycleUpdate>();

    public void update(PlayAppLifecycleUpdate update) {
        try {
            PlayAppStatus status = update.getStatus();
            if(status == PlayAppStatus.RUNNING || status == PlayAppStatus.FAILED){
                startEvent.put(update);
            }else{
                stopEvent.put(update);
            }
        } catch (InterruptedException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

    public PlayAppLifecycleUpdate waitForRunning() {
        try {
            return startEvent.take();
        } catch (InterruptedException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

    public PlayAppLifecycleUpdate waitForStop() {
        try {
            return stopEvent.take();
        } catch (InterruptedException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }
}
