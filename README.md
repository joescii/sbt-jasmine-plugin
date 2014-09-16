# sbt-jasmine-plugin

An SBT plugin for running jasmine tests in your build.

## Installation

Add this plugin like any other to your `project/plugin.sbt` file:

```scala
addSbtPlugin("com.joescii" % "sbt-jasmine-plugin" % "1.3.0")
```

You will also need to import the plugin's settings in the usual way in your `build.sbt` file:

```scala
seq(jasmineSettings : _*)
```

If your build is defined in a `.scala` file, then you will also need to import the plugin's namespace:

```scala
lazy val main = Project(appName, appVersion, appDependencies)
  .settings(seq(jasmineSettings : _*))
```


## Configuration

Override the following settings in your build:

 * appJsDir - the root directory where your application javascript lives
 * appJsLibDir - the root directory where you put javascript library files thast your application uses (e.g jquery)
 * jasmineTestDir - the directory that contains your jasmine tests, jasmine will look for /specs and /mocks sub directories **(note that the plugin only picks up test files named `*.spec.js`!!!)**
 * jasmineConfFile - the test.dependencies.js configuration file that loads the required application js and lib js files into the test context.
 * jasmineRequireJsFile - the file that is your require.js library file
 * jasmineRequireConfFile - the require.conf.js configuration file for require.js
 * jasmineEdition - the edition of Jasmine to use, i.e. the major version number 1 or 2

For a project laid out as follows:

```
src/
|-- main
|   `-- webapp
|       `-- static
|           `-- js
|               `-- samples
|                   |-- <app js files here>
|                   `-- lib
|                       `-- <js library files here>
`-- test
    `-- webapp
        `-- static
            `-- js
                |-- mocks
                |   `-- <jasmine mock js files here>
                |-- specs
                |   |-- <jasmine spec js files here>
                `-- test.dependencies.js

```

The project configuration would be:

```scala
appJsDir <+= sourceDirectory { src => src / "main" / "webapp" / "static" / "js" / "samples"}

appJsLibDir <+= sourceDirectory { src => src / "main" / "webapp" / "static" / "js" / "samples" / "lib" }

jasmineTestDir <+= sourceDirectory { src => src / "test" / "webapp" / "static" / "js" }

jasmineConfFile <+= sourceDirectory { src => src / "test" / "webapp" / "static" / "js" / "test.dependencies.js" }
```

You can now run the `jasmine` task to run the tests.

See [sbt-jasmine-example](https://github.com/guardian/sbt-jasmine-example) for a full working example project.


## Paths exposed to your tests

The following path variables are available to your javascript (in test.dependencies.js and the tests):

* EnvJasmine.testDir = the jasmineTestDir (note no trailing slash on this path)
* EnvJasmine.mocksDir = EnvJasmine.testDir / mocks
* EnvJasmine.specsDir = EnvJasmine.testDir / specs
* EnvJasmine.rootDir = the appJsDir
* EnvJasmine.libDir = the appJsLibRoot

N.B. all path variables have a trailing slash so you don't need to add them yourself when building paths. Thus to load
the query library as in your test.dependencies.js file you would add the following line:

```
EnvJasmine.loadGlobal(EnvJasmine.libDir + "jquery-1.4.4.js");
```


## Running as part of test

To automatically run the jasmine plugin as part of your project's test phase you can add the following to you build.sbt:

```
(test in Test) <<= (test in Test) dependsOn (jasmine)
```

## Generating an html runner page

If you need to run your jasmine tests in a browser (for example if, heaven forbid, you have failing tests and want to debug them)
you can run the ```jasmineGenRunner``` task, this will output a runner html file that you can load in a browser to run your jasmine tests.
A link to the output runner file is output in the sbt console.

## Contributions

Contributions are always welcomed via pull-requests.  Below is the recommended procedure for git:

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request

The following will be greatly appreciated as part of your Pull Request:

1. Updates to this documentation, including updates to the **Change Log** below.
2. If applicable, update the sample project with your feature enhancement.

## Change log

### 1.3.0
* Updated to utilize Jasmine 2.0.3 by default.
* Can optionally specify the Jasmine edition via sbt setting `jasmineEdition`.
* Jasmine edition 1 has been updated from 1.1.0 to 1.3.1.

### 1.2.3
* Fixed bug introduced in 1.2.2 where jasmine-gen-runner would not create all needed files.

### 1.2.2
* Better error message with stack trace.
* Fixed bug introduced in 1.2.1 where it would continue running tests after failure.

### 1.2.1
* Errors while parsing JavaScript will now result in a failed test run. (Thanks [Mikael Berglund](https://github.com/PhroZenOne))

### 1.2.0
* Ownership assumed by [joescii](https://github.com/joescii). 
* Merged [#11](https://github.com/guardian/sbt-jasmine-plugin/pull/11): Removed dependency on jQuery
* Resolved [#15](https://github.com/guardian/sbt-jasmine-plugin/issues/15): Bumped Rhino version to 1.7R4
* Resolved [#17](https://github.com/guardian/sbt-jasmine-plugin/issues/17): Fixed bug in `env.js` which caused angular 1.2.1 and up to not load
* Published binaries as a community sbt plugin

### 0 - 1.1
* The project was created and maintained on [The Guardian's github page](https://github.com/guardian/sbt-jasmine-plugin).
