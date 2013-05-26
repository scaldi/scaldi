package scaldi

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import scala.util.Random
import java.lang.reflect.Method
import scala.reflect.runtime.universe.{TypeTag, Type, typeTag}

class ReflectionBinderSpec extends WordSpec with ShouldMatchers {
  "ReflectionBinder" should {
    "discover bindings using reflection" in {
      val binder = new StaticModule {
        lazy val server = new HttpServer("localhost", 80)
      }

      binder.getBinding(List("server", classOf[HttpServer])) should be('defined)
    }

    "infer correct type and string identifier taken from member name" in {
      val binder = new StaticModule {
        def myHttpServer: Server = new HttpServer("localhost", 80)
        def otherServer = new HttpServer("test", 8080)
      }

      binder.getBinding(List("myHttpServer", classOf[Server])).get.get should be === Some(HttpServer("localhost", 80))
      binder.getBinding(List("myHttpServer", classOf[HttpServer])) should be ('empty)

      binder.getBinding(List("otherServer", classOf[Server])).get.get should be === Some(HttpServer("test", 8080))
      binder.getBinding(List("otherServer", classOf[HttpServer])).get.get should be === Some(HttpServer("test", 8080))
    }

    "support BidingProvider as return type of class members and use it to retrieve actual binding" in {
      val binder = new StaticModule {
        lazy val someBinding = SpecialBindingProvider(() => HttpServer("test", 8080))
      }

      binder.getBinding(List("someBinding")) should be ('empty)
      binder.getBinding(List(classOf[BindingProvider])) should be ('empty)
      binder.getBinding(List(classOf[Server])).get.get should be === Some(HttpServer("test", 8080))
      binder.getBinding(List("special")).get.get should be === Some(HttpServer("test", 8080))
    }

    "discover lazy vals that have semantics of lazy bindings" in {
      var instanceCount = 0
      val binder = new StaticModule {
        lazy val server = {
          instanceCount += 1
          HttpServer("localhost", Random.nextInt())
        }

        lazy val otherServer = HttpServer("test", 8080)
      }

      instanceCount should be (0)
      (1 to 10).map(x => binder.getBinding(List("server")).get.get).distinct should have size (1)
      instanceCount should be (1)
      binder.getBinding(List("otherServer")).get.get should be === Some(HttpServer("test", 8080))
    }

    "discover normal vals that have semantics of non-lazy bindings and instntiated immediately" in {
      var instanceCount = 0
      val binder = new StaticModule {
        val server = {
          instanceCount += 1
          new HttpServer("localhost", Random.nextInt())
        }

        val otherServer = HttpServer("test", 8080)
      }

      instanceCount should be (1)
      (1 to 10).map(x => binder.getBinding(List("server")).get.get).distinct should have size (1)
      instanceCount should be (1)
      binder.getBinding(List("otherServer")).get.get should be === Some(HttpServer("test", 8080))
    }

    "discover defs that have semantics of provider bindings" in {
      var instanceCount = 0
      val binder = new StaticModule {
        def server = {
          instanceCount += 1
          new HttpServer("localhost", Random.nextInt())
        }

        def otherServer = HttpServer("test", 8080)
      }

      instanceCount should be (0)
      (1 to 10).map(x => binder.getBinding(List("server")).get.get).distinct should have size (10)
      instanceCount should be (10)
      binder.getBinding(List("otherServer")).get.get should be === Some(HttpServer("test", 8080))
    }
  }
}

case class SpecialBindingProvider[T: TypeTag](fn: () => T) extends BindingProvider {
  def getBinding(name: String, tpe: Type) = LazyBinding(Some(fn), List(typeTag[T], "special"))
}