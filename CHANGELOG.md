## v0.3 (coming soon) ~ 01.02.2014

* Using Scala 2.10 reflection API under the hood. This will allow to bind and inject things like lists and functions by type. For example:
  ```
  bind [(Int, Int) => Int] to ((a: Int, b: Int) => a + b)
  bind [List[Int]] to List(1, 2, 3)

  val adder = inject [(Int, Int) => Int]
  val ints = inject [List[Int]]
  ```
* Updated SBT build to 0.13.1
* TOOD: `injectProvider`
* TODO: removal of `CreationHelper`