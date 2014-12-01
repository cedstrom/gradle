This specification outlines the work that is required to use Gradle to build applications that use the [Play framework](http://www.playframework.com).

# Use cases

There are 3 main use cases:

- A developer builds a Play application.
- A developer runs a Play application during development.
- A deployer runs a Play application. That is, a Play application is packaged up as a distribution which can be run in a production environment.

# Out of scope

The following features are currently out of scope for this spec, but certainly make sense for later work:

- Building a Play application for multiple Scala versions. For now, the build for a given Play application will target a single Scala version.
  It will be possible to declare which version of Scala to build for.
- Using continuous mode with JVMs older than Java 7. For now, this will work only with Java 7 and later will be supported. It will be possible to build and run for Java 6.
- Any specific IDE integration, beyond Gradle's current general purpose IDE integration for Java and Scala.
- Any specific testing support, beyond Gradle's current support for testing Java and Scala projects.
- Any specific support for publishing and resolving Play applications, beyond Gradle's current general purpose capabilities.
- Any specific support for authoring plugins, beyond Gradle's current support.
- Installing the Play tools on the build machine.
- Migrating or importing SBT settings for a Play project.

# Performance

Performance should be comparable to SBT:

- Building and starting an application.
- Reload after a change.
- Executing tests for an application.

# Milestone 1

For the first milestone, a developer will be able to define, build and run a simple play application that consists
of routes and templates, Java and Scala sources, as well as Javascript, CSS, CoffeeScript and LESSCSS sources.

In this milestone we are not dealing with:
- Automatically rebuilding the play application when sources change
- 3rd party dependencies for Java, Scala or Javascript. (This may change).

The goal is to get something working pretty quickly, and work to improve the configurability and discoverability over time.

## Feature: Developer builds, tests and runs the Play Application created by “activator new”

### Story: Build author declares and builds a Play application component

Add a `play-application` plugin that provides Play application component:

    plugins {
        id 'play-application'
    }

    model {
        components {
            myapp(PlayApplicationSpec) 
        }
    }

- ~~Running `gradle assemble` builds an empty Jar file.~~
- ~~Running `gradle components` shows some basic details about the Play application. ~~
- In this story, no source files are supported.

#### Test cases

- ~~component report shows PlayApplicationSpec with~~ 
- ~~version info about~~
      - ~~play  (declared in the plugin)~~
      - ~~java  (picked current version for now)~~
- ~~assemble creates an empty jar file~~

### Story: Developer builds a default generated Play application

- Using hard-coded:
    - Locations of java/scala files
    - Locations of routes file
    - Locations of templates files
    - Dependencies of the play ("com.typesafe.play:play_2.11:2.3.5")
    - Dependency of template compiler ("com.typesafe.play:twirl-compiler_2.11:1.0.2)
    - Dependency of routes compiler
- resolve different play dependencies from
    - typesafe maven release repository (https://repo.typesafe.com/typesafe/maven-releases)

- setup TwirlCompiler task type
- setup RoutesCompiler task type
- Compile routes to scala and java
- Compile templates to scala
- Compile all scala (app/*/*.{scala,java}, output of: conf/routes, output of: app/views/*.scala.html) files
- Output class files are part of assemble jar file

#### Test cases

- verify that generated scala template files exists
- verify that generated scala/java route files exists
- `gradle assemble` should trigger compile task and output jar should contain class files

#### Open issues

- Reasonable default memory settings for forked compilation

### Story: Developer runs Play application

Extend the Play support to allow the Play application to be executed.

- Running `gradle assemble` produces an executable Jar that, when executed, runs the Play application.
- Running `gradle run<ComponentName>` builds and executes the Play application.

At this stage, only the default generated Play application is supported, with a hard-coded version of Scala and Play.
          
#### Test cases

- verify that play app can be built and executed with play version 2.3.5 and 2.2.3
- Can configure port for launched PlayApp: default is 9000

### Story: Basic support for multiple play versions

- Can build play application with Play 2.2.3 on Scala 2.10, and Play 2.3.5 on Scala 2.11

#### Test cases

- Verify building and running the 'activator new' app with Play 2.2.3 and Play 2.3.5

#### Open issues

- Compile Scala wrappers around different Play versions, and load these via reflection

### Story: Developer tests the Play Seed application

- Add a `Test` test task per binary that runs the play application tests based on JUnit TestRunner against the binary
- The test sources are compiled against the binary and the `play-test` dependency based on play and scala version of the platform
- Can execute Play unit and integration tests
- Fails with a nice error message

#### Test cases
- Verify that running `gradle testBinary` works expected on the tests generated by `activator new`:
- Verify that tests executes correctly (lists the correct amount of test cases?)
- Verify that a failing test fails

#### Open issues

- Model test suites

## Feature: Developer builds Play application with custom Java, Scala, routes and templates

### Story: Developer includes Scala sources in Jvm library

Add a new 'scala-lang' plugin to add Scala language support implemented in the same manner as the JavaLanguagePlugin.

    plugins {
        id 'jvm-component'
        id 'scala-lang'
    }
    model {
        components {
            myLib(JvmLibrarySpec)
            myOtherLib(JvmLibrarySpec) {
                sources {
                    scala {
                        source.srcDir "src/myOtherLib/myScala"
                    }
                    otherScala(ScalaSourceSet) {
                        source.srcDir "src/otherScala"
                    }
                }
            }
        }
    }

##### Test cases

- Scala source set(s) and locations are visible in the components report
- Uses scala sources from conventional location
- Can configure location of scala sources
- Build is incremental:
    - Changed in Scala comment does not result in rebuilding JVM library
    - Change in Scala compile properties triggers recompilation
    - Change in Scala platform triggers recompilation
    - Change in Scala ToolChain triggers recompilation
    - Removal of Scala source files removes classes for that source file, which are removed from JVM library
- Compile is incremental: consider sources A, B, C where B depends on C
    - Change A, only A is recompiled
    - Change B, only B is recompiled
    - Change C, only B & C are recompiled
    - Remove A, outputs for A are removed
- Can include both Java and Scala sources in lib: no cross-compilation

### Story: Developer configures Scala and Java sources for Play application

Extend the Play support to allow full control over compilation of Scala and Java sources.

- Apply ScalaLanguagePlugin and JavaLanguagePlugin 
    - _not_ ScalaBasePlugin or JavaBasePlugin
- Sources have no dependencies other than Scala and Play.
- All Scala and Java sources in a Play application are joint-compiled


    plugins {
        id 'play-application'
    }
    model {
        components {
            play(PlayApplicationSpec) {
                sources {
                    extraJava(JavaSourceSet) {
                        source.srcDir "src/extraJava"
                    }
                    extraScala(ScalaSourceSet) {
                        source.srcDir "src/extraScala"
                    }
                }
            }
        }
    }

#### Test cases

- Scala and Java source sets (and locations) are visible in the components report
- Can provide additional Scala sources for a Play application, with dependencies on main sources
- Can provide additional Java sources for a Play application, with dependencies on main sources
- Build is incremental:
    - Changed in Scala comment does not result in rebuilding Play app
    - Changed in Java comment does not result in rebuilding Play app
    - Change in Scala or Java compile properties triggers recompilation
    - Change in Platform triggers recompilation
    - Change in ToolChain triggers recompilation
    - Removal of Scala/Java source files removes classes from the Play app

#### Open issues

- Ability for Twirl & Routes compilers to prefer Java types in generated sources (i.e. SBT enablePlugins(PlayJava))

### Story: Developer configures template sources for Play application

Add a TwirlSourceSet and permit multiple instances in a Play application

- Twirl sources show up in component report
- Allow Twirl source location to be configured
- Allow additional Twirl source sets to be configured
- Define a generated ScalaSourceSet as the output for each TwirlSourceSet compilation
    - Generated ScalaSourceSet will be one of the Scala compile inputs
    - Generated ScalaSourceSet should not be visible in components report
- Source generation and compilation should be incremental and remove stale outputs.

#### Open issues

- handle non html templates

### Story: Developer defines routes for Play application

Add a RoutesSourceSet and permit multiple instances in a Play application

- Routes sources show up in component report
- Allow Routes source location to be configured
- Allow additional Routes source sets to be configured
- Define a generated ScalaSourceSet as the output for each RoutesSourceSet compilation
    - Generated ScalaSourceSet will be one of the Scala compile inputs
    - Generated ScalaSourceSet should not be visible in components report
- Source generation and compilation should be incremental and remove stale outputs.

#### Open issues

- handle .routes files

## Feature: Developer includes compiled assets in Play application (Javascript, LESS, CoffeeScript)

Extend the standard build lifecycle to compile the front end assets to CSS and Javascript.

- Compiled assets
    - Coffeescript -> Javascript
    - LESSCSS -> CSS
    - Javascript > Javascript via Google Closure
    - Javascript minification, requirejs optimization
- Include the compiled assets in the Jar
- Include the `public/` assets in the Jar
- Include Play config files in the Jar (e.g. `conf/play.plugins`)
- Define source sets for each type of source file
- Compilation should be incremental and remove stale outputs
- Expose some compiler options

### Implementation

JavaScript language plugin:

- Defines JavaScript library component and associated JavaScript bundle binary.
- Defines JavaScript source set type (a JavaScript bundle and JavaScript source set should be usable in either role).
- Defines transformation from JavaScript source set to JavaScript bundle.

CSS language plugin:

- Defines CSS library component and associated CSS bundle binary.
- Defines CSS source set type (a CSS bundle and CSS source set should be usable in either role).
- Defines transformation from CSS source set to CSS bundle.

CoffeeScript plugin:

- Defines CoffeeScript source set type and transformation to JavaScript bundle.

LESSCSS plugin:

- Defines LESSCSS source set type and transformation to CSS bundle.

Google Closure plugin:

- Defines transformation from JavaScript source set to JavaScript bundle.

Play plugin:

- Defines JavaScript and CSS components for the Play application.
- Wires in the appropriate outputs to assemble the Jar.

### Open issues

- Integration with existing Gradle javascript plugins.

### Story: Developer includes compiled coffeescript assets in Play application

Add a coffee script plugin as well as JavaScriptSourceSet and CoffeeScriptSourceSets and permit multiple instances.

    plugins {
        id 'play-application'
        id 'play-coffeescript'
    }

    model {
        components {
            play(PlayApplicationSpec) {
                sources {
                    extraCoffeeScript(CoffeeScriptSourceSet) {
                        sources.srcDir "src/extraCoffeeScript"
                    }

                    extraJavaScript(JavaScriptSourceSet) {
                        sources.srcDir "src/extraJavaScript"
                    }
                }
            }
        }
    }

- Default coffeescript sourceset should be "app/assets/**/*.coffee"
- Compiled coffeescript files will be added to the jar relative to srcDir
- Default javascript sourceset should be "app/assets/**/*.js"

#### Test cases
- Coffeescript and javascript sources are visible in the components report
- Coffeescript sources successfully compiled to javascript
- Compiled coffeescript is added to jar relative to srcDir
- Javascript sources are copied directly into jar relative to srcDir
- Can provide additional coffeescript sources
- Can provide additional javascript sources
- Build is incremental:
    - Change in coffeescript source triggers recompile
    - No change in coffeescript source does not trigger a recompile
    - Removal of generated javascript triggers recompile
    - Removal of coffeescript source files removes generated javascript

## Feature: Developer chooses target Play, Scala and/or Java platform

### Story: Compilation and building is incremental with respect to changes in Play platform

### Story: Execute Play integration tests against all supported Play platforms

- Most Play integration tests should be able to run against different supported platforms
    - Developer build should run against single version by default
    - CI should run against all supported versions (using '-PtestAllPlatforms=true')

### Story: Build author declares target Play platform

    model {
        components {
            play(PlayApplicationSpec) {
                platform play: "2.3.6"
                platform play: "2.2.3"
            }
        }
    }

#### Test cases

- For each supported Play version: 2.2.3, 2.3.5, 2.3.6
    - Can assemble Play application
    - Can run Play application
    - Can test Play application
- Can build & test multiple Play application variants in single build invocation
    - `gradle assemble` builds all variants
    - `gradle test` tests all variants
- Play version implies Scala version and Java version
- Play version should be visible in components report and dependencies reports.

### Story: Build author declares target Scala platform

    model {
        components {
            play(PlayApplicationSpec) {
                platform play: "2.3.5", scala: "2.11"
            }
        }
    }


### Story: Build author declares target Java platform for Play application

    model {
        components {
            play(PlayApplicationSpec) {
                platform play: "2.3.5", scala: "2.11", java: "1.8"
            }
        }
    }

### Story: New target platform DSL for JVM components

    model {
        components {
            jvmLib(JvmLibrarySpec) {
                platform java: "1.8"
            }
        }
    }

### Story: New target platform DSL for native components

    model {
        components {
            nativeLib(NativeLibrarySpec) {
                platform os: "windows", arch: "x86"
            }
        }
    }


## Feature: Developer builds and runs Play application

Introduce some lifecycle tasks to allow the developer to run or start the Play application. For example, the
developer may run `gradle run` to run the application or `gradle start` to start the application.

- Add basic server + service domain model and some lifecycle tasks
- Model Play application as service
- Lifecycle to run in foreground, or start in background, as per `play run` and `play start`

Note that this story does not address reloading the application when source files change. This is addressed by a later story.

### Implementation

Web application plugin:

- Defines the concept of a web application.
- Defines the concept of a server that can host a web application.
- Defines lifecycle tasks for a given deployment.

Play plugin:

- Defines a Play application as-a web application
- Provides a Play server implementation that can host a Play application.

## Feature: Developer builds Play application distribution

Introduce some lifecycle tasks to allow the developer to package up the Play application. For example, the
developer may run `gradle stage` to stage the local application, or `gradle dist` to create a standalone distribution.

- Build distribution image and zips, as per `play stage` and `play dist`
- Integrate with the distribution plugin.

### Implementation

Play plugin:

- Defines a distribution that bundles a Play server and Play application.

## Further features

- Internal mechanism for plugin to inject renderer(s) into components report.
- Model language transformations, and change Play support to allow a Play application to take any JVM language as input.
- Declare dependencies on other Java/Scala libraries
- Control joint compilation of sources based on source set dependencies.
- Build multiple variants of a Play application.
- Generate an application install, eg with launcher scripts and so on.

# Milestone 2

## Feature: Long running compiler daemon

Reuse the compiler daemon across builds to keep the Scala compiler warmed up. This is also useful for the other compilers.

### Implementation

- Maintain a registry of compiler daemons in ~/.gradle
- Daemons expire some time after build, with much shorter expiry than the build daemon.
- Reuse infrastructure from build daemon.

## Feature: Keep running Play application up-to-date when source changes

This story adds an equivalent of Play's continuous mode (i.e. developer adds ~ before a command such as play ~run), where Gradle
monitors the source files for changes and rebuilds and restarts the application when some change is detected. Note that 'restart'
here means a logical restart.

Add a general-purpose mechanism which is able to keep the output of some tasks up-to-date when source files change. For example,
a developer may run `gradle --watch <tasks>`.

- Gradle runs tasks, then watches files that are inputs to a task but not outputs of some other task. When a file changes, repeat.
- Monitor files that are inputs to the model for changes too.
- When the tasks start a service, stop and restart the service(s) after rebuilding, or reload if supported by the service container.
- The Play application container must support reload. According to the Play docs the plugin can simply recreate the application
  ClassLoader.
- Integrate with the build-announcements plugin, so that desktop notifications can be fired when something fails when rerunning the tasks.

So:

- `gradle --watch run` would build and run the Play application. When a change to the source files are detected, Gradle would rebuild and
  restart the application.
- `gradle --watch test run` would build and run the tests and then the Play application. When a change to the source files is detected,
  Gradle would rerun the tests, rebuild and restart the Play application.
- `gradle --watch test` would build and run the tests. When a source file changes, Gradle would rerun the tests.

Note that for this story, the implementation will assume that any source file affects the output of every task listed on the command-line.
For example, running `gradle --watch test run` would restart the application if a test source file changes.

### Implementation

- Uses Gradle daemon to run build.
- Collect up all input files as build runs.
- Monitor changes to these input files. On change:
    - If previous build started any service, stop that service.
    - Trigger build.
- Deprecate reload properties from Jetty tasks, as they don't work well and are replaced by this general mechanism.

## Feature: Developer triggers rebuild of running Play application

This story adds an equivalent of Play's run command, where a build is triggered by the developer reloading the application in the browser
and some source files have changed.

The plugin will need to depend on Play's [sbt-link](http://repo.typesafe.com/typesafe/releases/com/typesafe/play/sbt-link/) library.
See [Play's BuildLink.java](https://github.com/playframework/playframework/blob/master/framework/src/build-link/src/main/java/play/core/BuildLink.java)
for good documentation about interfacing between Play and the build system. Gradle must implement the BuildLink interface and provide
it to Play's NettyServer. When a new request comes in, Play will call Gradle's implementation of BuildLink.reload and if any files have
changed then Gradle will have to recompile and return a new classloader to Play.

## Feature: Resources are built on demand when running Play application

When running a Play application, start the application without building any resources. Build these resources only when requested
by the client.

- On each request, check whether the task which produces the requested resource has been executed or not. If not, run the task synchronously
  and block until completed.
- Include the transitive input of these tasks as inputs to the watch mechanism, so that further changes in these source files will
  trigger a restart of the application at the appropriate time.
- Failures need to be forwarded to the application for display.

## Feature: Developer views compile and other build failures in Play application

Adapt compiler output to the format expected by Play:

- Model configuration problems
- Java and scala compilation failures
- Asset compilation failures
- Other verification task failures?

# Milestone 3

## Documentation

- Migrating an SBT based Play project to Gradle
- Writing Gradle plugins that extend the base Play plugin

## Native integration with Specs 2

Introduce a test integration which allows Specs 2 specifications to be executed directly by Gradle, without requiring the use of the Specs 2
JUnit integration.

- Add a Specs 2 plugin
- Add some Specs 2 options to test tasks
- Detect specs 2 specifications and schedule for execution
- Execute specs 2 specifications using its API and adapt execution events

Note: no changes to the HTML or XML test reports will be made.

## Developer runs Scala interactive console

Allow the Scala interactive console to be launched from the command-line.

- Build the project's main classes and make them visible via the console
- Add support for client-side execution of actions
- Model the Scala console as a client-side action
- Remove console decoration prior to starting the Scala console

## Scala code quality plugins

- Scalastyle
- SCCT

## Javascript plugins

- Compile Dust templates to javascript and include in the web application image

## Bootstrap a new Play project

Extend the build init plugin so that it can bootstrap a new Play project, producing the same output as `activator new` except with a Gradle build instead of
an SBT build.

# Later milestones

## Publish Play application to a binary repository

Allow a Play application distribution to be published to a binary repository.

Some candidates for later work:

- Improve the HTML test report to render a tree of test executions, for better reporting of Specs 2 execution (and other test frameworks)
- Support the new Java and Scala language plugins
- Improve watch mode so that only those tasks affected be a given change are executed
