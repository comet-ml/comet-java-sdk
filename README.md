# comet-java-sdk

### This project is still under development.

#### Using Comet Java SDK:
* The latest released version available in Maven Central:
  [![Maven Central](https://maven-badges.herokuapp.com/maven-central/ml.comet/comet-java-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/ml.comet/comet-java-client) 
   [![Build Status](https://travis-ci.com/comet-ml/comet-java-sdk.svg?branch=master)](https://travis-ci.com/github/comet-ml/comet-java-sdk)
* Add dependency to pom.xml:
```
    <dependencies>
        <dependency>
            <groupId>ml.comet</groupId>
            <artifactId>comet-java-client</artifactId>
            <version>1.1.2</version>
        </dependency>
    </dependencies>
```
* create experiment and log stuff:
```java
OnlineExperiment experiment = OnlineExperimentImpl.builder()
                .withApiKey("someApiKey")
                .withProjectName("someProject")
                .withWorkspace("someWorkspace")
                .build();
        experiment.setExperimentName("My experiment");
        experiment.logParameter("batch_size", "500");
        experiment.logMetric("strMetric", 123);
        experiment.end();
```

* Configure you experiment object:
```
#Configuration hierarchy:
#Environment Variable > Configuration File Override > Default config file (application.conf)

#Setting configuration in code:
OnlineExperimentImpl.builder().withApiKey("someApiKey").build();

#Override configuration file (can have partial keys)
OnlineExperimentImpl.builder().withConfig(new File("/tmp/comet.conf")).build();

# Read from environment variables OR from configuration file in classpath (application.conf)
OnlineExperimentImpl.builder().build();
```

* Full list of environment variables:
```
COMET_API_KEY
COMET_PROJECT_NAME
COMET_WORKSPACE_NAME
COMET_BASE_URL
COMET_MAX_AUTH_RETRIES
```

#### Examples

* You also can check [sample experiment](comet-examples/src/main/java/ml/comet/examples/OnlineExperimentExample.java)
* For more usage examples refer to [tests](comet-java-client/src/test/java/ml/comet/experiment)
