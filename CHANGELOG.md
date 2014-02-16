## v0.3 (coming soon) ~ 21.0.2014

* GroupId is changed to `org.scaldi`. So if you want to include it in the project, you need to use following configuration now:
 ```
 libraryDependencies += "org.scaldi" %% "scaldi" % "0.3"
 ```
* Using Scala 2.10 reflection API under the hood. This will allow to bind
  and inject things like lists and functions by type. For example:
  ```
  bind [(Int, Int) => Int] to ((a: Int, b: Int) => a + b)
  bind [List[Int]] to List(1, 2, 3)

  val adder = inject [(Int, Int) => Int]
  val ints = inject [List[Int]]
  ```
* Updated SBT build to 0.13.1
* Now you can also use `injectProvider` instead of `inject`. It will give back a function `() => T`, which you can use
  to inject value elsewhere. It can be useful for provider bindings (which are creating new instances each time you inject them)
  or conditional binding which also can give different objects each time you inject.
* `CreationHelper` utility object is removed.
* Added binding lifecycle. Now you can add `initWith` and `destroyWith` functions to the bindings:
  ```
  bind [Server] as to new LifecycleServer initWith (_.init()) destroyWith (_.terminate())
  ```
  Mutable `Injector`s also got `destroy` method which you can call explicitly or, if you forgot to do this, it would be
  called on JVM shutdown automatically.
* Documentation for all public API.