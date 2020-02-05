Dipendi
========

[![Build Status](https://travis-ci.org/protenus/dipendi.png?branch=master)](https://travis-ci.org/protenus/dipendi)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.scaldi/scaldi_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.scaldi/scaldi_2.12)

Dipendi provides a simple and elegant way to do dependency injection in Scala.
By using the expressive power of the Scala language, it defines an intuitive and
idiomatic DSL for binding and injecting dependencies. It is a highly extensible
library; you can easily customise almost any aspect of it. Some of its more unique
features are advanced module composition and conditional bindings, which can
help you build all kinds of applications - from small command-line tools to
large web applications. Dipendi also integrates nicely with Akka and Play.

Dipendi is a fork of [Scaldi](https://github.com/scaldi/scaldi), created to continue
the library's development in lieu of a new maintainer who can access the Scaldi
repository (see [scaldi/scaldi#81](https://github.com/scaldi/scaldi/issues/81)).

Some aspects of Scaldi project, including its documentation, are still being migrated.
Until the fork and migration are completed, you may need to rely on the original
project's documentation.

You can find an archive of the original project's homepage
[here](https://web.archive.org/web/20190616212058/http://scaldi.org/), or jump directly
to the documentation
[here](https://web.archive.org/web/20190618005634/http://scaldi.org/learn). Due to it
being an archived website, some of the links on it may not work properly.

## Adding Dipendi in Your Build

Dipendi has not yet been released following its creation. Until its release, you can
depend on Scaldi instead.

SBT Configuration:

```sbtshell
libraryDependencies += "org.scaldi" %% "scaldi" % "0.5.8"
```

Once Dipendi is released, it will support Scala 2.12 and 2.13. The release will
either be a drop-in replacement for Scaldi, or include a shim to make it one - most
likely the former.

## License

**Dipendi** is licensed under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
