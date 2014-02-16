## v0.3 (coming soon) ~ 01.02.2014

* GroupId is changed to `org.scaldi`. So if you want to include it in the project, you need to use following configuration now:
 ```
 libraryDependencies += "org.scaldi" %% "scaldi" % "0.3"
 ```
* Using Scala 2.10 reflection API under the hood. This will allow to bind and inject things like lists and functions by type. For example:
  ```
  bind [(Int, Int) => Int] to ((a: Int, b: Int) => a + b)
  bind [List[Int]] to List(1, 2, 3)

  val adder = inject [(Int, Int) => Int]
  val ints = inject [List[Int]]
  ```
* Updated SBT build to 0.13.1
* TODO: `injectProvider`
* TODO: removal of `CreationHelper`
* TODO: lifecycle