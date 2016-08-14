package scaldi

import org.scalatest.{Matchers, WordSpec}

import scala.language.postfixOps

class AmbiguousAwareInjectorSpec extends WordSpec with Matchers {
  "AmbiguousAwareInjector" should {
    "throw exception when stacked in Module with ambiguous bindings" in {
      import scaldi.Injectable._

      implicit val injector = new Module with AmbiguousAwareInjector {
        binding to 1
        binding to 42

        binding identifiedBy 'httpPort to "4321"
        binding identifiedBy 'httpPort to "1234"
      }

      an [BindingException] should be thrownBy inject [Int]
      an [BindingException] should be thrownBy inject [String] (identified by 'httpPort)
    }

    "throw exception when stacked in StaticModule with ambiguous bindings" in {
      import scaldi.Injectable._

      implicit val injector = new StaticModule with AmbiguousAwareInjector {
        val tcpHost = "tcp-test"
        val tcpPort = 1234
        val httpHost = "localhost"
        val httpPort = 4321
      }

      an [BindingException] should be thrownBy inject [Int]
      inject [String] (identified by 'tcpHost) should be("tcp-test")
    }

    "throw exception when stacked in DynamicModule with ambiguous bindings" in {
      import scaldi.Injectable._

      implicit val injector = new DynamicModule with AmbiguousAwareInjector {
        binding to 1
        binding to 42

        binding identifiedBy 'httpPort to "4321"
        binding identifiedBy 'httpPort to "1234"
      }

      an [BindingException] should be thrownBy inject [Int]
      an [BindingException] should be thrownBy inject [String] (identified by 'httpPort)
    }
  }
}