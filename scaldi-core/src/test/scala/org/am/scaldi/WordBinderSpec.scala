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
        binding identifiedBy classOf[String] and 'host and "httpServer" to "localhost"
        bind [String] to "localhost" identifiedBy 'host and "httpServer"
        binding to "localhost" identifiedBy classOf[String] and 'host and "httpServer"
      }

      binder.wordBindings should have size (4)
      binder.wordBindings foreach (_.hasIdentifiers(List(classOf[String], "host", "httpServer")) should be (true))
    }

    "infer binding type only when it's not specified" in {
      val binder = new WordBinder {
        binding to new HttpServer("localhost", 80)
      }

      binder.wordBindings should have size (1)
      binder.wordBindings(0) hasIdentifiers (List(classOf[Server])) should be (true)
      binder.wordBindings(0) hasIdentifiers (List(classOf[HttpServer])) should be (true)
    }

    "not infer binding type only when it is specified explicitly" in {
      val binder = new WordBinder {
        bind [Server] to new HttpServer("localhost", 80)
      }

      binder.wordBindings should have size (1)
      binder.wordBindings(0) hasIdentifiers (List(classOf[Server])) should be (true)
      binder.wordBindings(0) hasIdentifiers (List(classOf[HttpServer])) should be (false)
    }

    "allow to define normal lazy bingings that would be instantialted only one time" in {
      var instanceCount = 0
      val binder = new WordBinder {
        bind [Server] identifiedBy 'host and "httpServer" to {
          instanceCount  += 1
          new HttpServer("localhost", Random.nextInt())
        }
      }

      instanceCount should be (0)
      (1 to 10).map(x => binder.wordBindings(0).get).distinct should have size (1)
      instanceCount should be (1)
    }

    "allow to define normal non-lazy bingings that would be instantialted only one time" in {
      var instanceCount = 0
      val binder = new WordBinder {
        bind [Server] identifiedBy 'host and "httpServer" toNonLazy {
          instanceCount  += 1
          new HttpServer("localhost", Random.nextInt())
        }
      }

      instanceCount should be (0) // non-lazy binding will be initialized by Initializeable but not on it's own
      (1 to 10).map(x => binder.wordBindings(0).get).distinct should have size (1)
      instanceCount should be (1)
    }

    "allow to define provider bingings that would be instantialted each time" in {
      var instanceCount = 0
      val binder = new WordBinder {
        bind [Server] identifiedBy 'host and "httpServer" toProvider {
          instanceCount  += 1
          new HttpServer("localhost", Random.nextInt())
        }
      }

      instanceCount should be (0)
      (1 to 10).map(x => binder.wordBindings(0).get).distinct should have size (10)
      instanceCount should be (10)
    }
  }

  trait Server

  case class HttpServer(host: String, port: Int) extends Server
}