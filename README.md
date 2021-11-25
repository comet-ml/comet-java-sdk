# https://www.comet.ml official Java SDK    
[![version](https://img.shields.io/github/v/tag/comet-ml/comet-java-sdk.svg?sort=semver)](https://github.com/comet-ml/comet-java-sdk/releases/latest) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/ml.comet/comet-java-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/ml.comet/comet-java-client) [![license](https://img.shields.io/github/license/comet-ml/comet-java-sdk.svg)](https://github.com/comet-ml/comet-java-sdk/blob/master/LICENSE) [![yaricom/goNEAT](https://tokei.rs/b1/github/comet-ml/comet-java-sdk?category=lines)](https://github.com/comet-ml/comet-java-sdk)

| Branch | Tests                                                                                      | Coverage                                                                                                                             | Linting                                                                    | Code Security                                                                    |
|--------|--------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|----------------------------------------------------------------------------|
| master | [![CI](https://github.com/comet-ml/comet-java-sdk/actions/workflows/ci-maven.yml/badge.svg)](https://github.com/comet-ml/comet-java-sdk/workflows/ci-maven.yml) | [![codecov](https://codecov.io/gh/comet-ml/comet-java-sdk/branch/master/graph/badge.svg)](https://codecov.io/gh/comet-ml/comet-java-sdk) | [![Lint Code Base](https://github.com/comet-ml/comet-java-sdk/actions/workflows/super-linter.yml/badge.svg)](https://github.com/comet-ml/comet-java-sdk/actions/workflows/super-linter.yml) | [![CodeQL](https://github.com/comet-ml/comet-java-sdk/actions/workflows/codeQL.yml/badge.svg)](https://github.com/comet-ml/comet-java-sdk/actions/workflows/codeQL.yml) |

### Using Comet Java SDK:
#### Add dependency to pom.xml:
```
    <dependencies>
        <dependency>
            <groupId>ml.comet</groupId>
            <artifactId>comet-java-client</artifactId>
            <version>1.1.4</version>
        </dependency>
    </dependencies>
```
#### Create experiment and log metrics and parameters:
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

#### Configure you experiment object:
```java
# Configuration hierarchy:
Environment Variable > Configuration File Override > Default config file (application.conf)

# Setting configuration in code:
OnlineExperimentImpl.builder().withApiKey("someApiKey").build();

# Override configuration file (can have partial keys)
OnlineExperimentImpl.builder().withConfig(new File("/tmp/comet.conf")).build();

# Read from environment variables OR from configuration file in classpath (application.conf)
OnlineExperimentImpl.builder().build();
```

#### Full list of environment variables:
```java
COMET_API_KEY
COMET_PROJECT_NAME
COMET_WORKSPACE_NAME
COMET_BASE_URL
COMET_MAX_AUTH_RETRIES
```

#### Examples

* You also can check 
  * [sample experiment](comet-examples/src/main/java/ml/comet/examples/OnlineExperimentExample.java)
  * [MNIST classification experiment](comet-examples/src/main/java/ml/comet/examples/mnist/MnistExperimentExample.java)
* For more usage examples refer to [tests](comet-java-client/src/test/java/ml/comet/experiment)
