**Scaldi** is scala dependency injection framework. Basically Scala already have everything you need for dependency injection. But still
some things can be made easier. Goal of the project is to provide more standard and easy way to make dependency injection in scala
projects consuming power of scala language. With **Scaldi** you can define your application modules in pure scala
(without any annotations or XML).

At the moment **Scaldi** is proof of concept. I hope you will find it helpful. Your feedback is very welcome and it would be very helpful
for the further project development!

*Documentation and examples are coming soon.*

## SBT configuration

At first you need to add new repository:

    resolvers += "Angelsmasterpiece repo" at "https://raw.github.com/OlegIlyenko/angelsmasterpiece-maven-repo/master"

No you can add library dependency:

    libraryDependencies += "org.angelsmasterpiece.scaldi" %% "scaldi" % "1.0-SNAPSHOT"

## Maven configuration

In order to use **Scaldi** in maven project you should add one new repository in your *pom.xml*:

    <repositories>
        <repository>
            <id>angelsmasterpiece-repo</id>
            <name>Angelsmasterpiece Repository</name>
            <url>https://raw.github.com/OlegIlyenko/angelsmasterpiece-maven-repo/master</url>
        </repository>
    </repositories>

Now you can add this dependency (you need to specify scala version manuallu in the *artifactId*):

    <dependency>
        <groupId>org.angelsmasterpiece.scaldi</groupId>
        <artifactId>scaldi_2.9.1</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>