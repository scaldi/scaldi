package scaldi

import org.scalatest.{Matchers, WordSpec}

import scala.language.postfixOps

class ModuleSpec extends WordSpec with Matchers {
  "Module" should {
    "throw exception when binding is ambiguous" in {
      import scaldi.Injectable._

      implicit val injector = new Module {
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

