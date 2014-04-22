package scaldi

import org.scalatest.{Matchers, WordSpec}
import scala.util.Random

class WordBinderSpec extends WordSpec with Matchers {
  "WordBinder" should {
    "require to bind something" in {
      val binder = new WordBinder {
        bind [String] identifiedBy 'host
      }

      an [BindingException] should be thrownBy binder.wordBindings
    }

    "collect all identifiers for bindings" in {
      val binder = new WordBinder {
        bind [String] identifiedBy 'host and "httpServer" to "localhost"
        bind [String] as 'host and "httpServer" to "localhost"
        binding identifiedBy classOf[String] and 'host and "httpServer" to "localhost"
        bind [String] to "localhost" identifiedBy 'host and "httpServer"
        bind [String] to "localhost" as 'host and "httpServer"
        binding to "localhost" identifiedBy classOf[String] and 'host and "httpServer"
      }

      binder.wordBindings should have size 6
      binder.wordBindings foreach (_.isDefinedFor(List(classOf[String], "host", "httpServer")) should be (true))
    }

    "infer binding type only when it's not specified" in {
      val binder = new WordBinder {
        binding to new HttpServer("localhost", 80)
      }

      binder.wordBindings should have size 1
      binder.wordBindings(0) isDefinedFor List(classOf[Server]) should be (true)
      binder.wordBindings(0) isDefinedFor List(classOf[HttpServer]) should be (true)
    }

    "not infer binding type only when it is specified explicitly" in {
      val binder = new WordBinder {
        bind [Server] to new HttpServer("localhost", 80)
      }

      binder.wordBindings should have size 1
      binder.wordBindings(0) isDefinedFor List(classOf[Server]) should be (true)
      binder.wordBindings(0) isDefinedFor List(classOf[HttpServer]) should be (false)
    }

    "treat later bindings as overrides for earlier and more that one binding od the same type" in {
      val binder = new DynamicModule {
        bind [Server] to new HttpServer("localhost", 80)
        bind [Server] to new HttpServer("www.test.com", 8080)
      }.initNonLazy()

      binder.wordBindings should have size 2
      binder.getBinding(List(classOf[Server])).get.get should equal (Some(HttpServer("www.test.com", 8080)))

      val bindings = binder.getBindings(List(classOf[Server]))
      bindings should have size 2
      bindings(0).get should equal (Some(HttpServer("www.test.com", 8080)))
      bindings(1).get should equal (Some(HttpServer("localhost", 80)))
    }

    "allow to define normal lazy bindings that would be instantiated only one time" in {
      var instanceCount = 0
      val binder = new DynamicModule {
        bind [Server] identifiedBy 'server and "httpServer" to {
          instanceCount  += 1
          new HttpServer("localhost", Random.nextInt())
        }

        bind [Server] identifiedBy 'otherServer to HttpServer("test", 8080)
      }.initNonLazy()

      instanceCount should be (0)
      (1 to 10).map(x => binder.getBinding(List("server")).get.get).distinct should have size 1
      instanceCount should be (1)
      binder.getBinding(List("otherServer")).get.get should equal (Some(HttpServer("test", 8080)))
    }

    "allow to define normal non-lazy bindings that would be instantiated only one time" in {
      var instanceCount = 0
      val binder = new DynamicModule {
        bind [Server] identifiedBy 'server and "httpServer" toNonLazy {
          instanceCount  += 1
          new HttpServer("localhost", Random.nextInt())
        }

        bind [Server] identifiedBy 'otherServer toNonLazy HttpServer("test", 8080)
      }.initNonLazy()

      instanceCount should be (1)
      (1 to 10).map(x => binder.getBinding(List("server")).get.get).distinct should have size 1
      instanceCount should be (1)
      binder.getBinding(List("otherServer")).get.get should equal (Some(HttpServer("test", 8080)))
    }

    "allow to define provider bindings that would be instantiated each time" in {
      var instanceCount = 0
      val binder = new DynamicModule {
        bind [Server] identifiedBy 'server and "httpServer" toProvider {
          instanceCount  += 1
          new HttpServer("localhost", Random.nextInt())
        }

        bind [Server] identifiedBy 'otherServer toProvider HttpServer("test", 8080)
      }.initNonLazy()

      instanceCount should be (0)
      (1 to 10).map(x => binder.getBinding(List("server")).get.get).distinct should have size 10
      instanceCount should be (10)
      binder.getBinding(List("otherServer")).get.get should equal (Some(HttpServer("test", 8080)))
    }

    "support conditions with 'when'" in {
      var prodMode = true

      val binder = new DynamicModule {
        val ProdMode = Condition(prodMode)
        val DevMode = !ProdMode

        bind [String] as 'host when ProdMode to "www.prod-server.com"
        bind [String] as 'host when DevMode to "localhost"

        bind [Int] when ProdMode as 'port to 1234
      }

      binder.wordBindings should have size 3

      binder.getBinding(List('host)).get.get.get should equal ("www.prod-server.com")
      binder.getBinding(List('port)).get.get.get should equal (1234)

      prodMode = false

      binder.getBinding(List('host)).get.get.get should equal ("localhost")
      binder.getBinding(List('port)) should be ('empty)
    }

    "allow to define init and destroy functions" in {
      implicit val module = new DynamicModule {
        bind [Server] as 'server1 to new LifecycleServer initWith (_.init()) destroyWith (_.terminate())
        bind [Server] as 'server2 to new LifecycleServer initWith (_.init())
        bind [Server] as 'server3 to new LifecycleServer destroyWith (_.terminate())
      }

      import Injectable._

      (1 to 3) foreach (i => inject[Server](s"server$i"))

      val server1 = inject[Server]('server1).asInstanceOf[LifecycleServer]
      val server2 = inject[Server]('server2).asInstanceOf[LifecycleServer]
      val server3 = inject[Server]('server3).asInstanceOf[LifecycleServer]

      server1.initializedCount should equal (1)
      server1.destroyedCount should equal (0)

      server2.initializedCount should equal (1)
      server2.destroyedCount should equal (0)

      server3.initializedCount should equal (0)
      server3.destroyedCount should equal (0)

      module.destroy()
      module.destroy()
      module.destroy()

      server1.initializedCount should equal (1)
      server1.destroyedCount should equal (1)

      server2.initializedCount should equal (1)
      server2.destroyedCount should equal (0)

      server3.initializedCount should equal (0)
      server3.destroyedCount should equal (1)
    }
  }
}