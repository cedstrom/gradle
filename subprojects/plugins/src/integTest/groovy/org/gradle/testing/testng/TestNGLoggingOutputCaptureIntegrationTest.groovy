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

package org.gradle.testing.testng

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.HtmlTestExecutionResult
import org.gradle.integtests.fixtures.JUnitXmlTestExecutionResult
import org.gradle.testing.fixture.TestNGCoverage
import org.gradle.util.TextUtil

import static org.hamcrest.Matchers.is

class TestNGLoggingOutputCaptureIntegrationTest extends AbstractIntegrationSpec {

    def setup() {
        buildFile << """
            apply plugin: "java"
            repositories { jcenter() }
            dependencies { testCompile "org.testng:testng:$TestNGCoverage.NEWEST" }
            test {
                useTestNG()
                reports.junitXml.outputPerTestCase = true
                onOutput { test, event -> print "\$test -> \$event.message" }
            }
        """

        file("src/test/java/FooTest.java") << """import org.testng.annotations.*;
            public class FooTest {
                static { System.out.println("static out"); System.err.println("static err"); }

                public FooTest() {
                    System.out.println("constructor out"); System.err.println("constructor err");
                }

                @BeforeClass public static void beforeClass() {
                    System.out.println("beforeClass out"); System.err.println("beforeClass err");
                }

                @BeforeTest public void beforeTest() {
                    System.out.println("beforeTest out"); System.err.println("beforeTest err");
                }

                @Test public void m1() {
                    System.out.print("m1: ");
                    System.out.print("\u03b1</html>");
                    System.out.println();
                    System.err.println("m1 err");
                }

                @Test public void m2() {
                    System.out.println("m2 out"); System.err.println("m2 err");
                }

                @AfterTest public void afterTest() {
                    System.out.println("afterTest out"); System.err.println("afterTest err");
                }

                @AfterClass public static void afterClass() {
                    System.out.println("afterClass out"); System.err.println("afterClass err");
                }
            }
        """
    }

    def "attaches events to correct test descriptors of a suite"() {
        buildFile << "test.useTestNG { suites 'suite.xml' }"

        file("suite.xml") << """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="AwesomeSuite">
  <test name='The Foo Test'><classes><class name='FooTest'/></classes></test>
</suite>"""

        when: run "test"

        then:
        result.output.contains(TextUtil.toPlatformLineSeparators("""process 'Gradle Test Executor 1' -> static out
process 'Gradle Test Executor 1' -> static err
process 'Gradle Test Executor 1' -> constructor out
process 'Gradle Test Executor 1' -> constructor err
test 'The Foo Test' -> beforeTest out
test 'The Foo Test' -> beforeTest err
test 'The Foo Test' -> beforeClass out
test 'The Foo Test' -> beforeClass err
test method m1(FooTest) -> m1: \u03b1</html>
test method m1(FooTest) -> m1 err
test method m2(FooTest) -> m2 out
test method m2(FooTest) -> m2 err
test 'The Foo Test' -> afterClass out
test 'The Foo Test' -> afterClass err
test 'The Foo Test' -> afterTest out
test 'The Foo Test' -> afterTest err
"""))

        /**
         * This test documents the current behavior. It's not right, we're missing a lot of output in the report.
         */

        def xmlReport = new JUnitXmlTestExecutionResult(testDirectory)
        def classResult = xmlReport.testClass("FooTest")

        classResult.assertTestCaseStderr("m1", is("m1 err\n"))
        classResult.assertTestCaseStderr("m2", is("m2 err\n"))
        classResult.assertTestCaseStdout("m1", is("m1: \u03b1</html>\n"))
        classResult.assertTestCaseStdout("m2", is("m2 out\n"))
        classResult.assertStderr(is(""))
        classResult.assertStdout(is(""))

        def htmlReport = new HtmlTestExecutionResult(testDirectory)
        def classReport = htmlReport.testClass("FooTest")
        classReport.assertStdout(is("m1: \u03b1</html>\nm2 out\n"))
        classReport.assertStderr(is("m1 err\nm2 err\n"))
    }

    def "attaches output events to correct test descriptors"() {
        when: run "test"

        then:
        result.output.contains(TextUtil.toPlatformLineSeparators("""process 'Gradle Test Executor 1' -> static out
process 'Gradle Test Executor 1' -> static err
process 'Gradle Test Executor 1' -> constructor out
process 'Gradle Test Executor 1' -> constructor err
test 'Gradle test' -> beforeTest out
test 'Gradle test' -> beforeTest err
test 'Gradle test' -> beforeClass out
test 'Gradle test' -> beforeClass err
test method m1(FooTest) -> m1: \u03b1</html>
test method m1(FooTest) -> m1 err
test method m2(FooTest) -> m2 out
test method m2(FooTest) -> m2 err
test 'Gradle test' -> afterClass out
test 'Gradle test' -> afterClass err
test 'Gradle test' -> afterTest out
test 'Gradle test' -> afterTest err
"""))

        def xmlReport = new JUnitXmlTestExecutionResult(testDirectory)
        def classResult = xmlReport.testClass("FooTest")


        /**
         * This test documents the current behavior. It's not right, we're missing a lot of output in the report.
         */

        classResult.assertTestCaseStderr("m1", is("m1 err\n"))
        classResult.assertTestCaseStderr("m2", is("m2 err\n"))
        classResult.assertTestCaseStdout("m1", is("m1: \u03b1</html>\n"))
        classResult.assertTestCaseStdout("m2", is("m2 out\n"))
        classResult.assertStderr(is(""))
        classResult.assertStdout(is(""))

        def htmlReport = new HtmlTestExecutionResult(testDirectory)
        def classReport = htmlReport.testClass("FooTest")
        classReport.assertStdout(is("m1: \u03b1</html>\nm2 out\n"))
        classReport.assertStderr(is("m1 err\nm2 err\n"))
    }
}
