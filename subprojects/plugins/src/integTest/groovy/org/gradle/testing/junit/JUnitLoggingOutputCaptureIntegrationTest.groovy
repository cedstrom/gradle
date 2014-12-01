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

package org.gradle.testing.junit

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.HtmlTestExecutionResult
import org.gradle.integtests.fixtures.JUnitXmlTestExecutionResult
import org.gradle.testing.fixture.JUnitCoverage
import org.gradle.util.TextUtil
import spock.lang.Ignore

import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.is

class JUnitLoggingOutputCaptureIntegrationTest extends AbstractIntegrationSpec {
    def setup() {
        buildFile << """
            apply plugin: "java"
            repositories { mavenCentral() }
            dependencies { testCompile 'junit:junit:$JUnitCoverage.NEWEST' }
            test {
                reports.junitXml.outputPerTestCase = true
                onOutput { test, event -> print "\$test -> \$event.message" }
            }
        """
    }

    def "captures logging output events"() {
        file("src/test/java/OkTest.java") << """
public class OkTest {
    static {
        System.out.println("class loaded");
    }

    public OkTest() {
        System.out.println("test constructed");
    }

    @org.junit.BeforeClass public static void init() {
        System.out.println("before class out");
        System.err.println("before class err");
    }

    @org.junit.AfterClass public static void end() {
        System.out.println("after class out");
        System.err.println("after class err");
    }

    @org.junit.Before
    public void before() {
        System.out.println("before out");
        System.err.println("before err");
    }

    @org.junit.After
    public void after() {
        System.out.println("after out");
        System.err.println("after err");
    }

    @org.junit.Test
    public void ok() {
        System.out.print("test out: \u03b1</html>");
        System.out.println();
        System.err.println("test err");
    }

    @org.junit.Test
    public void anotherOk() {
        System.out.println("ok out");
        System.err.println("ok err");
    }
}
"""
        when: run "test"

        then:
        result.output.contains(TextUtil.toPlatformLineSeparators("""test class OkTest -> class loaded
test class OkTest -> before class out
test class OkTest -> before class err
test class OkTest -> test constructed
test anotherOk(OkTest) -> before out
test anotherOk(OkTest) -> before err
test anotherOk(OkTest) -> ok out
test anotherOk(OkTest) -> ok err
test anotherOk(OkTest) -> after out
test anotherOk(OkTest) -> after err
test class OkTest -> test constructed
test ok(OkTest) -> before out
test ok(OkTest) -> before err
test ok(OkTest) -> test out: \u03b1</html>
test ok(OkTest) -> test err
test ok(OkTest) -> after out
test ok(OkTest) -> after err
test class OkTest -> after class out
test class OkTest -> after class err
"""))

        // This test covers current behaviour, not necessarily desired behaviour

        def xmlReport = new JUnitXmlTestExecutionResult(testDirectory)
        def classResult = xmlReport.testClass("OkTest")
        classResult.assertTestCaseStdout("ok", is("""before out
test out: \u03b1</html>
after out
"""))
        classResult.assertTestCaseStderr("ok", is("""before err
test err
after err
"""))
        classResult.assertStdout(is("""class loaded
before class out
test constructed
test constructed
after class out
"""))
        classResult.assertStderr(is("""before class err
after class err
"""))

        def htmlReport = new HtmlTestExecutionResult(testDirectory)
        def classReport = htmlReport.testClass("OkTest")
        classReport.assertStdout(is("""class loaded
before class out
test constructed
before out
ok out
after out
test constructed
before out
test out: \u03b1</html>
after out
after class out
"""))
        classReport.assertStderr(is("""before class err
before err
ok err
after err
before err
test err
after err
after class err
"""))
    }

    def "captures output from logging frameworks"() {
        buildFile << """
dependencies { testCompile "org.slf4j:slf4j-simple:1.7.7", "org.slf4j:slf4j-api:1.7.7" }
"""
        file("src/test/java/FooTest.java") << """

            public class FooTest {
                private final static org.slf4j.Logger SLF4J = org.slf4j.LoggerFactory.getLogger(FooTest.class);
                private final static java.util.logging.Logger JUL = java.util.logging.Logger.getLogger(FooTest.class.getName());

                @org.junit.Test
                public void foo() {
                  SLF4J.info("slf4j info");
                  JUL.info("jul info");
                  JUL.warning("jul warning");
                }
            }
        """

        when: run("test")

        then:
        result.output.contains("test foo(FooTest) -> [Test worker] INFO FooTest - slf4j info")
        result.output.contains("test foo(FooTest) -> INFO: jul info")
        result.output.contains("test foo(FooTest) -> WARNING: jul warning")

        def testResult = new JUnitXmlTestExecutionResult(testDirectory)
        def classResult = testResult.testClass("FooTest")
        classResult.assertTestCaseStderr("foo", containsString("[Test worker] INFO FooTest - slf4j info"))
        classResult.assertTestCaseStderr("foo", containsString("INFO: jul info"))
        classResult.assertTestCaseStderr("foo", containsString("WARNING: jul warning"))
    }

    def "test can generate output from multiple threads"() {
        file("src/test/java/OkTest.java") << """
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;

public class OkTest {
    @org.junit.Test
    public void ok() throws Exception {
        // logging from multiple threads
        List<Thread> threads  = new ArrayList<Thread>();
        for (int i = 0; i < 5; i++) {
            Thread thread = new Thread("thread " + i) {
                @Override
                public void run() {
                    System.out.print("stdout from "); // print a partial line
                    System.err.println("stderr from " + getName());
                    System.out.println(getName());
                    Logger.getLogger("test-logger").info("info from " + getName());
                }
            };
            thread.start();
            threads.add(thread);
        }
        for(Thread thread: threads) {
            thread.join();
        }
    }
}
"""

        when:
        run("test")

        then:
        def testResult = new JUnitXmlTestExecutionResult(testDirectory)
        def classResult = testResult.testClass("OkTest")

        5.times { n ->
            result.output.contains("test ok(OkTest) -> stdout from thread $n")
            result.output.contains("test ok(OkTest) -> stderr from thread $n")
            result.output.contains("test ok(OkTest) -> INFO: info from thread $n")

            classResult.assertTestCaseStdout("ok", containsString("stdout from thread $n"))
            classResult.assertTestCaseStderr("ok", containsString("stderr from thread $n"))
            classResult.assertTestCaseStderr("ok", containsString("INFO: info from thread $n"))
        }
    }

    @Ignore
    def "output does not require trailing end-of-line separator"() {
        expect: false
    }
}
