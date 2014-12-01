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

package org.gradle.execution.taskgraph

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.initialization.BuildCancellationToken
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.gradle.util.TestUtil.createRootProject

class DefaultTaskExecutionPlanParallelTaskHandlingTest extends Specification {

    DefaultTaskExecutionPlan executionPlan = new DefaultTaskExecutionPlan(Stub(BuildCancellationToken))
    DefaultProject root = createRootProject();

    List<TaskInfo> startedTasks = []
    List<Thread> blockedThreads = []

    void cleanup() {
        completeAllStartedTasks()
        allBlockedThreadsFinish()
    }

    private void addToGraphAndPopulate(Task... tasks) {
        executionPlan.addToTaskGraph(Arrays.asList(tasks))
        executionPlan.determineExecutionPlan()
    }

    void startTasks(int num) {
        num.times { startedTasks << executionPlan.getTaskToExecute() }
    }

    void noMoreTasksCurrentlyAvailableForExecution() {
        blockedThreads << blockedThread { executionPlan.taskComplete(executionPlan.getTaskToExecute()) }
    }

    void completeAllStartedTasks() {
        startedTasks.each { executionPlan.taskComplete(it) }
        startedTasks.clear()
    }

    void allBlockedThreadsFinish() {
        blockedThreads*.join()
        blockedThreads.clear()
    }

    @ParallelizableTask
    static class Parallel extends DefaultTask {}

    static class ParallelChild extends Parallel {}

    Thread blockedThread(Runnable target) {
        def conditions = new PollingConditions(timeout: 3, delay: 0.01)
        def thread = new Thread(target)

        thread.start()
        conditions.eventually {
            assert thread.state == Thread.State.WAITING
        }
        thread
    }

    void requestedTasksBecomeAvailableForExecution() {
        allBlockedThreadsFinish()
    }

    def "two dependent parallelizable tasks are not executed in parallel"() {
        given:
        Task a = root.task("a", type: Parallel)
        Task b = root.task("b", type: Parallel).dependsOn(a)

        when:
        addToGraphAndPopulate(b)
        startTasks(1)

        then:
        noMoreTasksCurrentlyAvailableForExecution()
    }

    def "two parallelizable tasks with must run after ordering are not executed in parallel"() {
        given:
        Task a = root.task("a", type: Parallel)
        Task b = root.task("b", type: Parallel).mustRunAfter(a)

        when:
        addToGraphAndPopulate(a, b)
        startTasks(1)

        then:
        noMoreTasksCurrentlyAvailableForExecution()
    }

    def "task that extend a parallelizable task are not parallelizable by default"() {
        given:
        Task a = root.task("a", type: ParallelChild)
        Task b = root.task("b", type: ParallelChild)

        when:
        addToGraphAndPopulate(a, b)
        startTasks(1)

        then:
        noMoreTasksCurrentlyAvailableForExecution()
    }

    def "task is not available for execution until all of its dependencies that are executed in parallel complete"() {
        given:
        Task a = root.task("a", type: Parallel)
        Task b = root.task("b", type: Parallel)
        Task c = root.task("c", type: Parallel).dependsOn(a, b)

        when:
        addToGraphAndPopulate(c)
        startTasks(2)

        then:
        noMoreTasksCurrentlyAvailableForExecution()

        when:
        completeAllStartedTasks()

        then:
        requestedTasksBecomeAvailableForExecution()
    }

    def "a parallelizable task with custom actions is not run in parallel"() {
        given:
        Task a = root.task("a", type: Parallel)
        Task b = root.task("b", type: Parallel).doLast {}

        when:
        addToGraphAndPopulate(a, b)
        startTasks(1)

        then:
        noMoreTasksCurrentlyAvailableForExecution()
    }

    def "DefaultTask is parallelizable"() {
        given:
        Task a = root.task("a")
        Task b = root.task("b")

        when:
        addToGraphAndPopulate(a, b)

        then:
        startTasks(2)
    }
}
