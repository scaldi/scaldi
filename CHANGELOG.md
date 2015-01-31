## v0.5 (31.01.2015)

* JSR 330 support. Scaldi now fully implements (except optional static injection) [JSR 330 (Dependency Injection for Java)](https://jcp.org/en/jsr/detail?id=330) spec.
  * New syntax added to bind JSR 330 annotated classes

  ```
  bind [Engine] to annotated [V8Engine]
  ```
  * `OnDemandAnnotationInjector` - New `Injector` that creates JSR 330 compliant bindings on-demand (when they are injected)
  * `AnnotationIdentifier` allows to bind classes with JSR 330 `Qualifier` annotation. You can now also use it in the bindings:

  ```
  import scaldi.jsr330._

  bind [Seat] identifiedBy qualifier [Drivers] to annotated [DriversSeat]
  ```
* Required identifiers. Every identifier now defines, whether it is required during the lookup. The only required built-in identifier
  at the moment is `AnnotationIdentifier`. You can now also make an identifier (not) required in the binding with the new syntax:

  ```
  bind [Tire] identifiedBy required('spare) to annotated [SpareTire]
  bind [Seat] identifiedBy notRequired(qualifier [Drivers]) to annotated [DriversSeat]
  ```
* `ImmutableWrapper` that was previously described in the documentation now is part of the scaldi codebase.
* `in` binding syntax is now deprecated and will be removed in future versions of Scaldi.
* [Typesafe config](https://github.com/typesafehub/config) is now natively supported via `TypesafeConfigInjector`
* `ReflectiveBinder` and `StaticModule` are deprecated and will be removed in the next versions
  * `Module` does not support `ReflectiveBinder` anymore - only word bindings are supported

## v0.4 (22.06.2014)

* Constrictor injection with `injected` macros. Here is an example if it's usage:  `bind [Users] to injected [UserService]`.
* Only Scala version 2.11 is supported from now on (mostly because of the macros)
* Conditions are now composed together with **and** if applied several times for the same binding with `when` block:
  
  ```
  when (inDevMode or inTestMode) {
    bind [Database] to new Riak
    bind [PaymentService] to new MockPaymentService
  }
  ```

## v0.3.2 (24.04.2014)

* Minor bugfix in raw property injector

## v0.3.1 (23.04.2014)

* Added support for scala 2.11 (cross-compiling with 2.10 and 2.11)
* Small clean-up of `Injectable`
    * Dropped several `inject` method overloads in order to make it work with 2.11.
      In most cases you will not notice any difference. If you are using vararg version of `inject` that takes the seq of
      identifies as an argument, then you need to rewrite it with standard `inject` DSL and use `and` to provide several identifiers.
      Here is an example:
      ```
      // don't work anymore
      inject [Database] ('local, 'db)

      // use this syntax instead
      inject [Database] ('local and 'db)

      // or this one
      inject [Database] (identified by 'local and 'db)
      ```
    * Dropped `injectWithDefault` in favour of existing `inject` DSL. So if you want to provide a default for the binding,
      then please use this syntax instead:
      ```
      inject [Database] (by default new Riak)
      ```

## v0.3 (02.03.2014)

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
  bind [Server] to new LifecycleServer initWith (_.init()) destroyWith (_.terminate())
  ```

  Mutable `Injector`s also got `destroy` method which you can call explicitly or, if you forgot to do this, it would be
  called on JVM shutdown automatically.
* `inject` will now make sure during the compilation time, that provided binding class is inferred correctly (in other words it's not `Nothing`)
