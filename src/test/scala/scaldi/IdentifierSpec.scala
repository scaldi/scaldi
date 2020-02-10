package scaldi

import java.text.{DateFormat, SimpleDateFormat}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IdentifierSpec extends AnyWordSpec with Matchers {
  private type Bar = List[Int]

  "Identifier" when {
    "defaults are used" should {
      "be converted from string" in {
        getId("str") should equal (StringIdentifier("str"))
      }

      "be converted from symbol" in {
        getId(Symbol("sym")) should equal (StringIdentifier("sym"))
      }

      "be converted from class" in {
        getId(classOf[String]) should equal (TypeTagIdentifier.typeId[String])
      }
    }

    "compared" should {
      "not match for different identifier types" in {
        getId(classOf[DateFormat]) sameAs getId("java.text.DateFormat") should be (false)
      }

      "not match for different identifier values" in {
        getId(Symbol("user")) sameAs getId("publisher") should be (false)
        getId(classOf[DateFormat]) sameAs getId(classOf[String]) should be (false)
      }

      "match for the same identifier types and values" in {
        getId(Symbol("publisher")) sameAs getId("publisher") should be (true)
        getId(classOf[String]) sameAs getId(classOf[String]) should be (true)
      }

      "use polymorphism to compare classes" in {
        getId(classOf[SimpleDateFormat]) sameAs getId(classOf[DateFormat]) should be (true)
        getId(classOf[DateFormat]) sameAs getId(classOf[SimpleDateFormat]) should be (false)
      }

      "treat type-aliases as their values" in {
        getId(classOf[List[Int]]) should equal (getId(classOf[Bar]))
        getId(classOf[Bar]) should equal (getId(classOf[List[Int]]))
        getId(classOf[Bar]) should equal (getId(classOf[Bar]))
      }
    }
  }

  def getId[T: CanBeIdentifier](target: T) = implicitly[CanBeIdentifier[T]].toIdentifier(target)
}