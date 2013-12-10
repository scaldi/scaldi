[Scaldi](http://scaldi.org) is scala dependency injection library. Basically Scala
already have everything you need for dependency injection. But still some things can be made easier.
Goal of the project is to provide more standard and easy way to make dependency injection in scala
projects consuming power of scala language. With *Scaldi* you can define your application modules in pure scala
(without any annotations or XML).

You can find project's home page here:

[http://scaldi.org](http://scaldi.org)

Scaladocs for the latest version of the project can be found here:

[http://scaldi.org/api/index.html#scaldi.package](http://scaldi.org/api/index.html#scaldi.package/api/index.html#scaldi.package)

At the moment project is poorly documented, but it will be fixed soon. At the moment I can recommend you to take a look
at the tests:

[http://scaldi.org/scaldi/tree/master/src/test/scala/scaldi](http://scaldi.org/scaldi/tree/master/src/test/scala/scaldi)

[![Build Status](https://travis-ci.org/scaldi/scaldi.png?branch=master)](https://travis-ci.org/scaldi/scaldi)

## Adding Scaldi in Your Build

SBT Configuration:

    libraryDependencies += "com.github.scaldi" %% "scaldi" % "0.2"

Maven Configuration (you need to specify scala version manually in the *artifactId*):

    <dependency>
        <groupId>com.github.scaldi</groupId>
        <artifactId>scaldi_2.10</artifactId>
        <version>0.2</version>
    </dependency>

## License

**Scaldi** is licensed under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).