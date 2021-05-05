# comet-java-sdk

### This project is still under development.

#### Using Comet Java SDK:
* The latest released version available in Maven Central:
  [![Maven Central](https://maven-badges.herokuapp.com/maven-central/ml.comet/comet-java-sdk/badge.svg)](https://maven-badges.herokuapp.com/maven-central/ml.comet/comet-java-sdk) 
   [![Build Status](https://travis-ci.com/comet-ml/comet-java-sdk.svg?branch=master)](https://travis-ci.com/github/comet-ml/comet-java-sdk)
* Add dependency to pom.xml:
```
    <dependencies>
        <dependency>
            <groupId>ml.comet</groupId>
            <artifactId>comet-java-sdk</artifactId>
            <version>1.0.12</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/lib/comet-java-client-beta.jar</systemPath>
        </dependency>
    </dependencies>
```
* create experiment [example](comet-examples/src/main/java/ml/comet/examples/OnlineExperimentExample.java)
* for more usage examples refer to [tests](comet-java-client/src/test/java/ml/comet/experiment)
