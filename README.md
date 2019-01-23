# comet-java-sdk

### This project is still under development.

#### Using Comet Java SDK:
* download latest jar file from [releases page](https://github.com/comet-ml/comet-java-client/releases)
* to maven pom.xml add:
```
    <dependencies>
        <dependency>
            <groupId>com.comet</groupId>
            <artifactId>comet-java-sdk</artifactId>
            <version>1.0-SNAPSHOT</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/lib/comet-java-client-beta.jar</systemPath>
        </dependency>
    </dependencies>
```
* create experiment - [example](/comet-examples/src/main/java/com/comet/examples)
