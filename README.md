# comet-java-sdk

### This project is still under development.

#### adding the jar file in a maven project
* download latest jar file from [releases page](https://github.com/comet-ml/comet-java-client/releases)
* to maven pom.xml add:
```
    <repositories>
        <repository>
            <id>in-project</id>
            <name>In Project Repo</name>
            <url>file://${project.basedir}/path/to/jar/comet-java-client-01.jar</url>
        </repository>
    </repositories>
```
* create experiment - [example](/tree/master/comet-examples/src/main/java/com/comet/examples)
