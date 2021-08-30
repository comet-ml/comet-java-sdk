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
            <version>1.1.1</version>
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
####You also can check [sample experiment](comet-examples/src/main/java/ml/comet/examples/OnlineExperimentExample.java)
For more usage examples refer to [tests](comet-java-client/src/test/java/ml/comet/experiment)
