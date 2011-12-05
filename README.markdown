[Scaldi](http://olegilyenko.github.com/scaldi/Scaldi.html) is scala dependency injection library. Basically Scala
already have everything you need for dependency injection. But still some things can be made easier.
Goal of the project is to provide more standard and easy way to make dependency injection in scala
projects consuming power of scala language. With *Scaldi* you can define your application modules in pure scala
(without any annotations or XML).

At the moment *Scaldi* is proof of concept. I hope you will find it helpful. Your feedback is very welcome and it would be very helpful
for the further project development!

You can find project's home page here:

[http://olegilyenko.github.com/scaldi/Scaldi.html](http://olegilyenko.github.com/scaldi/Scaldi.html)

Scaladocs for the latest version of the project can be found here:

[http://olegilyenko.github.com/scaldi/api/index.html#scaldi.package](http://olegilyenko.github.com/scaldi/api/index.html#scaldi.package)

At the moment project is poorly documented, but it will be fixed soon. At the moment I can recommend you to take a look
at the tests:

[http://github.com/OlegIlyenko/scaldi/tree/master/src/test/scala/scaldi](http://github.com/OlegIlyenko/scaldi/tree/master/src/test/scala/scaldi)

## SBT Configuration

At first you need to add new repository:

    resolvers += "Angelsmasterpiece repo" at "https://raw.github.com/OlegIlyenko/angelsmasterpiece-maven-repo/master"

Now you can add library dependency:

    libraryDependencies += "org.angelsmasterpiece.scaldi" %% "scaldi" % "0.1"

## Maven Configuration

In order to use **Scaldi** in maven project you should add one new repository in your *pom.xml*:

    <repositories>
        <repository>
            <id>angelsmasterpiece-repo</id>
            <name>Angelsmasterpiece Repository</name>
            <url>https://raw.github.com/OlegIlyenko/angelsmasterpiece-maven-repo/master</url>
        </repository>
    </repositories>

Now you can add this dependency (you need to specify scala version manually in the *artifactId*):

    <dependency>
        <groupId>org.angelsmasterpiece.scaldi</groupId>
        <artifactId>scaldi_2.9.1</artifactId>
        <version>0.1</version>
    </dependency>

## License

**Scaldi** is licensed under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).