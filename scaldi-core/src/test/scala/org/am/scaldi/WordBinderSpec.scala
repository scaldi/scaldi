package org.am.scaldi

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import scala.util.Random

class WordBinderSpec extends WordSpec with ShouldMatchers {
  "WordBinder" should {
    "require to bind something" in {
      val binder = new WordBinder {
        bind [String] identifiedBy 'host
      }

      evaluating(binder.wordBindings) should produce [BindingException]
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

      binder.wordBindings should have size (6)
      binder.wordBindings foreach (_.isDefinedFor(List(classOf[String], "host", "httpServer")) should be (true))
    }

    "infer binding type only when it's not specified" in {
      val binder = new WordBinder {
        binding to new HttpServer("localhost", 80)
      }

      binder.wordBindings should have size (1)
      binder.wordBindings(0) isDefinedFor (List(classOf[Server])) should be (true)
      binder.wordBindings(0) isDefinedFor (List(classOf[HttpServer])) should be (true)
    }

    "not infer binding type only when it is specified explicitly" in {
      val binder = new WordBinder {
        bind [Server] to new HttpServer("localhost", 80)
      }

      binder.wordBindings should have size (1)
      binder.wordBindings(0) isDefinedFor (List(classOf[Server])) should be (true)
      binder.wordBindings(0) isDefinedFor (List(classOf[HttpServer])) should be (false)
    }

    "treat later bindings as overrieds for earlier and more that one binding od the same type" in {
      val binder = new DynamicModule {
        bind [Server] to new HttpServer("localhost", 80)
        bind [Server] to new HttpServer("www.test.com", 8080)
      }.initNonLazy()

      binder.wordBindings should have size (2)
      binder.getBinding(List(classOf[Server])).get.get should be === Some(HttpServer("www.test.com", 8080))

      val bindings = binder.getBindings(List(classOf[Server]))
      bindings should have size (2)
      bindings(0).get should be === Some(HttpServer("www.test.com", 8080))
      bindings(1).get should be === Some(HttpServer("localhost", 80))
    }

    "allow to define normal lazy bingings that would be instantialted only one time" in {
      var instanceCount = 0
      val binder = new DynamicModule {
        bind [Server] identifiedBy 'server and "httpServer" to {
          instanceCount  += 1
          new HttpServer("localhost", Random.nextInt())
        }

        bind [Server] identifiedBy 'otherServer to HttpServer("test", 8080)
      }.initNonLazy()

      instanceCount should be (0)
      (1 to 10).map(x => binder.getBinding(List("server")).get.get).distinct should have size (1)
      instanceCount should be (1)
      binder.getBinding(List("otherServer")).get.get should be === Some(HttpServer("test", 8080))
    }

    "allow to define normal non-lazy bingings that would be instantialted only one time" in {
      var instanceCount = 0
      val binder = new DynamicModule {
        bind [Server] identifiedBy 'server and "httpServer" toNonLazy {
          instanceCount  += 1
          new HttpServer("localhost", Random.nextInt())
        }

        bind [Server] identifiedBy 'otherServer toNonLazy HttpServer("test", 8080)
      }.initNonLazy()

      instanceCount should be (1)
      (1 to 10).map(x => binder.getBinding(List("server")).get.get).distinct should have size (1)
      instanceCount should be (1)
      binder.getBinding(List("otherServer")).get.get should be === Some(HttpServer("test", 8080))
    }

    "allow to define provider bingings that would be instantialted each time" in {
      var instanceCount = 0
      val binder = new DynamicModule {
        bind [Server] identifiedBy 'server and "httpServer" toProvider {
          instanceCount  += 1
          new HttpServer("localhost", Random.nextInt())
        }

        bind [Server] identifiedBy 'otherServer toProvider HttpServer("test", 8080)
      }.initNonLazy()

      instanceCount should be (0)
      (1 to 10).map(x => binder.getBinding(List("server")).get.get).distinct should have size (10)
      instanceCount should be (10)
      binder.getBinding(List("otherServer")).get.get should be === Some(HttpServer("test", 8080))
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

      binder.wordBindings should have size (3)

      binder.getBinding(List('host)).get.get.get should be === "www.prod-server.com"
      binder.getBinding(List('port)).get.get.get should be === 1234

      prodMode = false

      binder.getBinding(List('host)).get.get.get should be === "localhost"
      binder.getBinding(List('port)) should be ('empty)
    }
  }
}