package com.comet.experiment;

import org.slf4j.Logger;

interface ExperimentBuilder {
    ExperimentBuilder withProjectName(String projectName);
    ExperimentBuilder withWorkspace(String workspace);
    ExperimentBuilder withApiKey(String restApiKey);
    ExperimentBuilder withExperimentName(String experimentName);
    ExperimentBuilder withLogger(Logger logger);
    ExperimentBuilder interceptStdout();
    Experiment build();
}
