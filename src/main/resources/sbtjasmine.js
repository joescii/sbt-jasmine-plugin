/*
 EnvJasmine: Jasmine test runner for EnvJS.

 EnvJasmine allows you to run headless JavaScript tests.

 Based on info from:
 http://agile.dzone.com/news/javascript-bdd-jasmine-without
 http://www.mozilla.org/rhino/
 http://www.envjs.com/
 http://pivotal.github.com/jasmine/
*/

importPackage(java.lang);
importPackage(java.io);
importPackage(org.mozilla.javascript);
importPackage(com.joescii);

// Create the EnvJasmine namespace
if (!this.EnvJasmine) {
    this.EnvJasmine = {};
}

EnvJasmine.cx = Context.getCurrentContext();
EnvJasmine.cx.setOptimizationLevel(-1);
EnvJasmine.topLevelScope = this;

EnvJasmine.normalizePath = function(path) {
    var endsInSlash = (path.slice(-1) == "/");

    if (path.slice(0, 1) == ".") {
        path = EnvJasmine.rootDir + path;
    }

    return File(path).getCanonicalPath() + (endsInSlash ? "/" : "");
};

EnvJasmine.loadFactory = function(scope) {
    return function (path) {
        var fileIn,
            normalizedPath = EnvJasmine.normalizePath(path);

        try {
            fileIn = new FileReader(normalizedPath);
            EnvJasmine.cx.evaluateReader(scope, fileIn, normalizedPath, 0, null);
        } catch (e) {
            print('Could not read file: ' + normalizedPath + "\n error was: " + e );
        } finally {
            fileIn.close();
        }
    };
};

EnvJasmine.resourceLoadFactory = function(scope) {
    return function (path) {
        var reader;

        try {
            reader = new BundledLibraryReaderFactory(path).reader();
            EnvJasmine.cx.evaluateReader(scope, reader, path, 0, null);
        } catch (e) {
            print('Could not read resource: ' + path + "\n error was: " + e );
        } finally {
            reader.close();
        }
    };
};

EnvJasmine.loadGlobal = EnvJasmine.loadFactory(EnvJasmine.topLevelScope);
EnvJasmine.loadLibGlobal = EnvJasmine.resourceLoadFactory(EnvJasmine.topLevelScope);

EnvJasmine.environment = java.lang.System.getProperty("os.name").indexOf('Windows') > -1 ?  'WIN' : 'UNIX';

function setupDirs(appJsRoot, appJsLibRoot, testRoot, confFile) {

    EnvJasmine.testDir = EnvJasmine.normalizePath(testRoot + "/");
    EnvJasmine.mocksDir = EnvJasmine.normalizePath(EnvJasmine.testDir + "mocks/");
    EnvJasmine.specsDir = EnvJasmine.normalizePath(EnvJasmine.testDir + "specs/");
    EnvJasmine.rootDir = EnvJasmine.normalizePath(appJsRoot + "/");
    EnvJasmine.libDir = EnvJasmine.normalizePath(appJsLibRoot + "/");

    // This is the standard spec suffix
    EnvJasmine.specSuffix = new RegExp(/.spec.js$/);

    EnvJasmine.configFile = confFile;
};

EnvJasmine.SEPARATOR = (function (env) {
    if (env == "UNIX") {
        return "/";
    } else if  (env == "WIN") {
        return "\\";
    } else {
        print("no separator set");
        return "/";
    }
}(EnvJasmine.environment));

EnvJasmine.disableColor = (function (env) {
    return EnvJasmine.disableColorOverride || (env == "WIN");
}(EnvJasmine.environment));

(function() {
    if (EnvJasmine.disableColor) {
        EnvJasmine.green = function(msg) { return msg; };
        EnvJasmine.red = function(msg) { return msg; };
        EnvJasmine.plain = function(msg) { return msg; };
    } else {
        var green = "\033[32m",
            red = "\033[31m",
            end = "\033[0m";

        EnvJasmine.green = function(msg) { return green + msg + end; };
        EnvJasmine.red = function(msg) { return red + msg + end; };
        EnvJasmine.plain = function(msg) { return msg; };
    }
}());

EnvJasmine.results = [];

EnvJasmine.loadConfig = function () {
    EnvJasmine.loadLibGlobal("jasmineEnv.js")
    EnvJasmine.loadGlobal(EnvJasmine.configFile);
};

// only prints the JavaScript path of the stacktrace.
EnvJasmine.printRhinoStackTrace = function(exception){
    var sw = new java.io.StringWriter();
    exception.printStackTrace(new java.io.PrintWriter(sw));
    var lines = sw.toString().split('\n');
    for(var i = 0; i < lines.length; i++){
        if(lines[i].indexOf("at script") !== -1){
            print(lines[i]);
        }
    }
}

function runTests(appJsRoot, appJsLibRoot, testRoot, confFile, envHtml) {
    var i, fileIn, len;

    setupDirs(appJsRoot, appJsLibRoot, testRoot, confFile);

    EnvJasmine.specs = [];
    EnvJasmine.passedCount = 0;
    EnvJasmine.failedCount = 0;
    EnvJasmine.totalCount = 0;

    EnvJasmine.loadConfig();
    
    if (typeof EnvJasmine.reporterClass === "undefined") {
    	// Use the standard reporter
    	EnvJasmine.reporterClass = RhinoReporter;
    }

    jasmine.getEnv().addReporter(new EnvJasmine.reporterClass());
    jasmine.getEnv().updateInterval = 0; // do not yield.

    if (EnvJasmine.suppressConsoleMsgs === true) {
        // suppress console messages
        window.console = $.extend({}, window.console, {
            info: jasmine.createSpy(),
            log: jasmine.createSpy(),
            debug: jasmine.createSpy(),
            warning: jasmine.createSpy(),
            error: jasmine.createSpy()
        });
    }

    EnvJasmine.loadLibGlobal("spanDir/spanDir.js");
    if (EnvJasmine.specs.length == 0) {
        spanDir(EnvJasmine.specsDir, function(spec) {
            if (EnvJasmine.specSuffix.test(spec)) {
                EnvJasmine.specs.push(spec);
            }
        });
    }

    for (i = 0, len = EnvJasmine.specs.length >>> 0; i < len; i += 1) {
        try {
            EnvJasmine.currentScope = {};
            EnvJasmine.load = EnvJasmine.loadFactory(EnvJasmine.currentScope);
            EnvJasmine.specFile = EnvJasmine.specs[i];
            print("running spec:" + EnvJasmine.specFile);
            // TODO: allow 'inline' loading when AMD disabled
            // fileIn = new FileReader(EnvJasmine.specFile);
            // EnvJasmine.cx.evaluateReader(EnvJasmine.currentScope, fileIn, EnvJasmine.specs[i], 0, null);
            var specPathOsSpecific = "" + EnvJasmine.specFile; // make sure it's a javascript string object rather than a java object
            if(EnvJasmine.environment == 'WIN') {
                specPathOsSpecific = specPathOsSpecific.replace(/\\/g, "\\\\");
            }
            var specLoader = 'require(["' + specPathOsSpecific + '"]);';
            EnvJasmine.cx.evaluateString(EnvJasmine.currentScope, specLoader, 'Loading '+EnvJasmine.specFile, 0, null);
            print("running the jasmine tests");
            var envHtmlOsSpecific = '' + envHtml; // make sure it's a javascript string object rather than a java object
            if(EnvJasmine.environment == 'WIN') {
                envHtmlOsSpecific = '/' + envHtmlOsSpecific.replace(/\\/g, "\\\\");
            }
            var windowLoader = 'window.location.assign("file://' + envHtmlOsSpecific + '")';
            EnvJasmine.cx.evaluateString(EnvJasmine.currentScope, windowLoader, 'Executing '+EnvJasmine.specs[i], 0, null);
        } catch (e) {

             print(EnvJasmine.red('error running jasmine test: ' + EnvJasmine.specs[i] + "\n error was: " + e));
             if(e.rhinoException){
                EnvJasmine.printRhinoStackTrace(e.rhinoException);
             } else if(e.javaException) {
                 e.javaException.printStackTrace();
             } else {
                /*
                 The exception is a regular javascript exception but I could not get a hold of stack trace anywhere. 
                 Rhino does not support e.stack as chrome or similar. It also does not support rethrowing
                 as a "throw e" will result in a stack that points to here. :/ Let's do nothing until someone can fix this.
                 */
             }
            EnvJasmine.failedCount++;
            EnvJasmine.totalCount++;
        }
        finally {
            if (fileIn) {
              fileIn.close();
              fileIn = null;
            }
        }
    }

    if (EnvJasmine.results.length > 0) {
        print("\n");
        print(EnvJasmine.red(EnvJasmine.results.join("\n\n")));
    }

    print();
    print(EnvJasmine[EnvJasmine.passedCount ? 'green' : 'plain']("Passed: " + EnvJasmine.passedCount));
    print(EnvJasmine[EnvJasmine.failedCount ? 'red' : 'plain']("Failed: " + EnvJasmine.failedCount));
    print(EnvJasmine.plain("Total : " + EnvJasmine.totalCount));

    return EnvJasmine.failedCount;
};
