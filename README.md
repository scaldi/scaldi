Scaldi
========

![Continuous Integration](https://github.com/scaldi/scaldi/workflows/Continuous%20Integration/badge.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.scaldi/scaldi_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.scaldi/scaldi_2.13)

Scaldi provides a simple and elegant way to do dependency injection in Scala.
By using the expressive power of the Scala language, it defines an intuitive and
idiomatic DSL for binding and injecting dependencies. It is a highly extensible
library; you can easily customise almost any aspect of it. Some of its more unique
features are advanced module composition and conditional bindings, which can
help you build all kinds of applications - from small command-line tools to
large web applications. Scaldi also integrates nicely with Akka and Play.

The Scaldi documentation DNS record ownership is in flux.
Until that is resolved, you may need to rely on the original project's documentation.
You can find an archive of the original project's homepage
[here](https://web.archive.org/web/20190616212058/http://scaldi.org/), or jump directly
to the documentation
[here](https://web.archive.org/web/20190618005634/http://scaldi.org/learn). Due to it
being an archived website, some of the links on it may not work properly.

## Adding Scaldi to Your Build

SBT Configuration:

```sbtshell
libraryDependencies += "org.scaldi" %% "scaldi" % "0.6.0"
```

Scaldi supports Scala 2.11, 2.12 and 2.13.

## License

**Scaldi** is licensed under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
