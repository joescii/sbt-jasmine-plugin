importPackage(java.lang);

var RhinoReporter = function() {
    return {
        passedExpectations: [],
        failedExpectations: [],
        currentSuite: null,

        // reporter interface functions:
        jasmineStarted: function() {

        },
        jasmineDone: function() {
            this.saveResults();
        },

        suiteStarted: function(result) {
            this.currentSuite = result.fullName;
        },

        suiteDone: function(result) {

        },

        specStarted: function(result) {

        },

        specDone: function(specResult) {
            if(specResult.failedExpectations.length == 0){
                System.out.print(EnvJasmine.green("."));
            }else{
                for(var i=0; i<specResult.failedExpectations.length; ++i){
                    var failedExpectation = specResult.failedExpectations[i];
                    var msg = [
                        "FAILED",
                        "File : " + EnvJasmine.specFile,
                        "Suite: " + this.currentSuite,
                        "Spec : " + specResult.description,
                        failedExpectation.message
                    ];
                    if('stack' in failedExpectation){
                        msg.push(failedExpectation.stack);
                    }
                    EnvJasmine.results.push(msg.join("\n"));
                }

                System.out.print(EnvJasmine.red("F"));
            }

            this.passedExpectations = this.passedExpectations.concat(specResult.passedExpectations);
            this.failedExpectations = this.failedExpectations.concat(specResult.failedExpectations);
        },

        // helper functions:
        saveResults: function() {
            EnvJasmine.passedCount = this.passedExpectations.length;
            EnvJasmine.failedCount = this.failedExpectations.length;
            EnvJasmine.totalCount = this.passedExpectations.length + this.failedExpectations.length;
        }
    };
};
