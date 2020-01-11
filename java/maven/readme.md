# 使用maven对jar包进行打包

本文主要简述在非springboot环境下利用对于java项目进行打包，我们一共使用了两种plugin，第一种是**maven-assembly-plugin**，第二种是**maven-jar-plugin**。

## assembly-plugin

assembly-plugin主要是能够打包出一个uber-jar，这样能够只需要一个jar包就能够在不同的jre里面直接运行，而不需要额外的配置，这样jre（泛指jar所在的虚拟机/物理机）是stateless的，只有jar包是带状态的。这对于小项目而言是非常友好的，但是对于大型项目而言，每一次都需要在package时需要copy很多的依赖，每一次的一点小改动都会执行该操作，而大部分时间，我们都是不需要修改依赖的，这将会导致消耗的时间会非常大。

```xml
    <build>
      <pluginManagement>
		<plugin>
          <artifactId>maven-assembly-plugin</artifactId>
          <configuration>
            <archive>
              <manifest>
                <mainClass>cse.App</mainClass>
              </manifest>
            </archive>
            <descriptorRefs>
              <descriptorRef>jar-with-dependencies</descriptorRef>
            </descriptorRefs>
          </configuration>
          <executions>
            <execution>
              <id>make-assembly</id> <!-- this is used for inheritance merges -->
              <phase>package</phase> <!-- bind to the packaging phase -->
              <goals>
                <goal>single</goal>
              </goals>
            </execution>
          </executions>
        </plugin>
      </pluginManagement>
    </build>
```

主要确定好整个包的入口，这里默认使用了jar-with-dependencies的模式进行打包。之后执行

`mvn clean compile assembly:single`最终获取到带有jar-with-dependencies的jar在target文件夹中。

## maven-jar-plugin

maven-jar-plugin只会将你写的code打包，并不会将相关依赖库进行打包。因此我们也需要额外的操作来进行dependency的下载

```xml
<plugin>
  <artifactId>maven-jar-plugin</artifactId>
  <version>3.0.2</version>
  <configuration>
    <archive>
      <manifest>
       <addClasspath>true</addClasspath>
         <classpathPrefix>dependency/</classpathPrefix>
           <mainClass>cse.App</mainClass>
      </manifest>
    </archive>
  </configuration>
</plugin>
```

plugin中规定了dependency的位置以及主函数的位置，我们此时只需要把相关的dependency放到jar包的相对路径下即可。

`mvn -DoutputDirectory=./dependency dependency:copy-dependencies`将所有的依赖下载放置于dependency目录中，之后将该目录放置进入到jar相对位置即可运行。

这样进行部署带来的缺点之处在于，如果在开发环境中使用不同版本的依赖进行开发，会导致在本地或是在CI中完成了测试，也会在production环境中因版本不匹配造成相应的问题。



