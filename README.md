# thrift-to-grpc
A tool use to load thrift class from dependency jar transform to gRPC proto, generate marshaller and service.

# MAVEN:

```
<dependency>
  <groupId>com.yinxiang.utils</groupId>
  <artifactId>thrift-to-grpc</artifactId>
  <version>1.0.0</version>
</dependency>
```

# SUMMARY:

1. Collect definitions of Thrift from the thrift generate dependency.
2. Transform the definitions of Thrift to gRPC proto.
3. Generate gRPC stubs code by protoc.
4. Generate two kind marshaller: gRPC to Thrift and Thrift to gRPC. (option)
5. Generate all gRPC services. (option)

# NOTE:

1. Enums of Thrift will change to ``int32`` in gRPC.
2. Parameters of Thrift method will package to a new message in gRPC.
3. Some message will move the 'COMMON.proto' because they are circle dependencies.
4. The same name message will append '_' + [1,2...].
5. Set in Thrift will change to ``repeated`` in gRPC.
6. If generate gRPC services, must create a ThriftStub.java(Any name is ok) and have all Thrift client method.

```
project
  -proto
    ...
      
  -stubs(moudle)
    target
      generated-sources
    pom.xml
    
  -marshaller(moudle)
    target
      generated-sources
    pom.xml
    
  -server(moudle)
    src
      main
        java
          ...
            ThriftStub.java (Any name is ok)
    target
      generated-sources
    pom.xml
    
  pom.xml
```

## Step one:
Add the thrift generate dependency and this dependency to stubs pom, and append like this: 
* gRPC proto will generate to the project.proto...

```
  <build>
    <extensions>
      <extension>
        <!-- Required for protobuf-maven-plugin since Google provides platform-specific
             protoc executables -->
        <groupId>kr.motd.maven</groupId>
        <artifactId>os-maven-plugin</artifactId>
        <version>1.5.0.Final</version>
      </extension>
    </extensions>
    <plugins>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>3.0.0</version>
        <executions>
          <execution>
            <phase>generate-sources</phase>
            <goals>
              <goal>java</goal>
            </goals>
            <configuration>
              <mainClass>com.yinxiang.utils.thrift.grpc.entrances.ProtoMaker</mainClass>
              <arguments>
                <argument>[Thrift package name which want to transform to gRPC. e.g: com.test]</argument>
                <argument>[The file names which want to scan, defalut empty. e.g: TestsIface]</argument>
                <argument>[The file names which want to skip, defalut empty. e.g: TestsIface]</argument>
                <argument>${basedir}/../proto/</argument>
                <argument>[The proto package. e.g: com/test/grpc]</argument>
              </arguments>
            </configuration>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.xolstice.maven.plugins</groupId>
        <artifactId>protobuf-maven-plugin</artifactId>
        <version>0.6.1</version>
        <configuration>
          <protocArtifact>com.google.protobuf:protoc:3.6.1:exe:${os.detected.classifier}
          </protocArtifact>
          <pluginId>grpc-java</pluginId>
          <pluginArtifact>
            io.grpc:protoc-gen-grpc-java:${grpc.version}:exe:${os.detected.classifier}
          </pluginArtifact>
          <protoSourceRoot>${basedir}/../proto</protoSourceRoot>
          <clearOutputDirectory>true</clearOutputDirectory>
        </configuration>
        <executions>
          <execution>
            <goals>
              <goal>compile</goal>
              <goal>compile-custom</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-source-plugin</artifactId>
        <version>3.0.1</version>
        <executions>
          <execution>
            <id>attach-sources</id>
            <goals>
              <goal>jar</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

You can ``mvn install`` to generate all proto and gRPC classes.

## Step two:
Add the Step one dependency to pom, like this:

```
  <dependencies>
    <dependency>
      <groupId></groupId>
      <artifactId>stubs</artifactId>
      <version></version>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>build-helper-maven-plugin</artifactId>
        <version>1.8</version>
        <executions>
          <execution>
            <id>add-source</id>
            <phase>generate-sources</phase>
            <goals>
              <goal>add-source</goal>
            </goals>
            <configuration>
              <sources>
                <source>src/main/java</source>
                <source>target/generated-sources/java</source>
              </sources>
            </configuration>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <source>1.8</source>
          <target>1.8</target>
          <encoding>UTF-8</encoding>
          <compilerArguments> 
            <extdirs>${project.basedir}/lib</extdirs>
          </compilerArguments>
        </configuration>
      </plugin>

      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>3.0.0</version>
        <executions>
          <execution>
            <phase>generate-sources</phase>
            <goals>
              <goal>java</goal>
            </goals>
            <configuration>
              <mainClass>com.yinxiang.utils.thrift.grpc.entrances.MarshallerMaker</mainClass>
              <arguments>
                <argument>[Thrift package name which want to transform to gRPC. e.g: com.test]</argument>
                <argument>[The file names which want to scan, defalut empty. e.g: TestsIface]</argument>
                <argument>[The file names which want to skip, defalut empty. e.g: TestsIface]</argument>
                <argument>[The proto package. e.g: com/test/grpc]</argument>
                <argument>${basedir}/target/generated-sources/java/</argument>
                <argument>[The package of thrift marshaller. e.g: com/test/marshaller/thrift]</argument>
                <argument>[The package of gRPC marshaller. e.g: com/test/marshaller/grpc]</argument>
              </arguments>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

You can ``mvn install`` to generate all marshaller.

## Step three:
Add the Step two dependency to pom, like this:

```
  <dependencies>
    <dependency>
      <groupId></groupId>
      <artifactId>marshaller</artifactId>
      <version></version>
    </dependency>
  </dependencies>
  <build>
    <resources>
      <resource>
        <directory>src/main/resources</directory>
        <targetPath>${project.build.directory}/classes</targetPath>
      </resource>
      <resource>
        <directory>src/main/java</directory>
        <filtering>true</filtering>
      </resource>
    </resources>
    <plugins>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>build-helper-maven-plugin</artifactId>
        <version>1.8</version>
        <executions>
          <execution>
            <id>add-source</id>
            <phase>generate-sources</phase>
            <goals>
              <goal>add-source</goal>
            </goals>
            <configuration>
              <sources>
                <source>src/main/java</source>
                <source>target/generated-sources/java</source>
              </sources>
            </configuration>
          </execution>
        </executions>
      </plugin>

      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <source>1.8</source>
          <target>1.8</target>
          <encoding>UTF-8</encoding>
          <compilerArguments>
            <extdirs>${project.basedir}/lib</extdirs>
          </compilerArguments>
        </configuration>
      </plugin>

      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>3.0.0</version>
        <executions>
          <execution>
            <phase>generate-sources</phase>
            <goals>
              <goal>java</goal>
            </goals>
            <configuration>
              <mainClass>com.yinxiang.utils.thrift.grpc.entrances.ServiceMaker</mainClass>
              <arguments>
                <argument>[Thrift package which want to transform to gRPC. e.g: com.test]</argument>
                <argument>[The file names which want to scan, defalut empty. e.g: TestsIface]</argument>
                <argument>[The file names which want to skip, defalut empty. e.g: TestsIface]</argument>
                <argument>[The proto package. e.g: com/test/grpc]</argument>
                <argument>${basedir}/target/generated-sources/java/</argument>
                <argument>${basedir}/target/generated-sources/java/</argument>
                <argument>[The package of thrift marshaller. e.g: com/test/marshaller/thrift]</argument>
                <argument>[The package of gRPC marshaller. e.g: com/test/marshaller/grpc]</argument>
                <argument>[The package of stub. e.g: com/test/stub]</argument>
                <argument>${basedir}/target/generated-sources/java/</argument>
                <argument>[The package of services. e.g: com/test/services]</argument>
              </arguments>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```
