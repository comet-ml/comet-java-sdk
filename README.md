# https://www.comet.ml official Java SDK    
[![version](https://img.shields.io/github/v/tag/comet-ml/comet-java-sdk.svg?sort=semver)](https://github.com/comet-ml/comet-java-sdk/releases/latest) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/ml.comet/comet-java-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/ml.comet/comet-java-client) [![license](https://img.shields.io/github/license/comet-ml/comet-java-sdk.svg)](https://github.com/comet-ml/comet-java-sdk/blob/master/LICENSE) [![javadoc](https://javadoc.io/badge2/ml.comet/comet-java-client/javadoc.svg)](https://javadoc.io/doc/ml.comet/comet-java-client) [![yaricom/goNEAT](https://tokei.rs/b1/github/comet-ml/comet-java-sdk?category=lines)](https://github.com/comet-ml/comet-java-sdk) 

| Branch | Tests                                                                                      | Coverage                                                                                                                             | Linting                                                                    | Code Security                                                                    |
|--------|--------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|----------------------------------------------------------------------------|
| master | [![CI](https://github.com/comet-ml/comet-java-sdk/actions/workflows/ci-maven.yml/badge.svg)](https://github.com/comet-ml/comet-java-sdk/workflows/ci-maven.yml) | [![codecov](https://codecov.io/gh/comet-ml/comet-java-sdk/branch/master/graph/badge.svg)](https://codecov.io/gh/comet-ml/comet-java-sdk) | [![Lint Code Base](https://github.com/comet-ml/comet-java-sdk/actions/workflows/super-linter.yml/badge.svg)](https://github.com/comet-ml/comet-java-sdk/actions/workflows/super-linter.yml) | [![CodeQL](https://github.com/comet-ml/comet-java-sdk/actions/workflows/codeQL.yml/badge.svg)](https://github.com/comet-ml/comet-java-sdk/actions/workflows/codeQL.yml) |

## Using Comet Java SDK
### Add dependency to the pom.xml
```
    <dependencies>
        <dependency>
            <groupId>ml.comet</groupId>
            <artifactId>comet-java-client</artifactId>
            <version>1.1.11</version>
        </dependency>
    </dependencies>
```
### Create experiment and log metrics and parameters
```java
OnlineExperiment experiment = ExperimentBuilder.OnlineExperiment()
                .withApiKey("someApiKey")
                .withProjectName("someProject")
                .withWorkspace("someWorkspace")
                .build();
    experiment.setExperimentName("My experiment");
    experiment.logParameter("batch_size", "500");
    experiment.logMetric("strMetric", 123);
    experiment.end();
```
The ```OnlineExperiment``` also can be used with [try-with-resources](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) statement which automatically
handles call to the ```experiment.end()```.
```java
try (OnlineExperiment experiment = ExperimentBuilder.OnlineExperiment()
                .withApiKey("someApiKey")
                .withProjectName("someProject")
                .withWorkspace("someWorkspace")
                .build()) {
    experiment.setExperimentName("My experiment");
    experiment.logParameter("batch_size", "500");
    experiment.logMetric("strMetric", 123);
} catch (Exception e) {
    e.printStackTrace();
}
```

### Configure experiment object

#### Configuration sources hierarchy

The configuration parameters search order as following (first-listed are higher priority):
 * system properties or environment variables
 * configuration file set by call to [```withConfigOverride(java.io.File)```](comet-java-client/src/main/java/ml/comet/experiment/builder/BaseCometBuilder.java)
 * ```application.conf``` (all resources on the classpath with this name)
 * ```reference.conf``` (all resources on the classpath with this name)
 
#### Programmatic configuration

It is possible to override some or all configuration parameters programmatically when 
you create a new experiment's instance using [```ExperimentBuilder```](comet-java-client/src/main/java/ml/comet/experiment/ExperimentBuilder.java)
factory.
```java
// Setting specific configuration parameters with builder
ExperimentBuilder.OnlineExperiment().withApiKey("someApiKey").build();

// Override configuration file (can have partial keys)
ExperimentBuilder.OnlineExperiment().withConfigOverride(new File("/tmp/comet.conf")).build();

// Read from environment variables OR from configuration file in classpath (application.conf)
ExperimentBuilder.OnlineExperiment().build();
```

### Full list of environment variables
```text
COMET_API_KEY
COMET_PROJECT_NAME
COMET_WORKSPACE_NAME
COMET_BASE_URL
COMET_MAX_AUTH_RETRIES
```

### Examples

* You also can check 
  * [sample experiment](comet-examples/src/main/java/ml/comet/examples/OnlineExperimentExample.java)
  * [MNIST classification experiment](comet-examples/src/main/java/ml/comet/examples/mnist/MnistExperimentExample.java)
  * [Comet artifact examples](comet-examples/src/main/java/ml/comet/examples/ArtifactExample.java)
  * [Log model example](comet-examples/src/main/java/ml/comet/examples/LogModelExample.java)
  * [model registry/Comet API example](comet-examples/src/main/java/ml/comet/examples/RegistryModelExample.java)
* For more usage examples refer to [tests](comet-java-client/src/test/java/ml/comet/experiment)
