package scaldi

import java.text.{DateFormat, SimpleDateFormat}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IdentifierSpec extends AnyWordSpec with Matchers {
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
      "should not match for different identifier types" in {
        getId(classOf[DateFormat]) sameAs getId("java.text.DateFormat") should be (false)
      }

      "should match for the same identifier types" in {
        getId(Symbol("publisher")) sameAs getId("publisher") should be (true)
        getId(Symbol("user")) sameAs getId("publisher") should be (false)

        getId(classOf[String]) sameAs getId(classOf[String]) should be (true)
        getId(classOf[DateFormat]) sameAs getId(classOf[String]) should be (false)
      }

      "use polymothism to compare classes" in {
        getId(classOf[SimpleDateFormat]) sameAs getId(classOf[DateFormat]) should be (true)
        getId(classOf[DateFormat]) sameAs getId(classOf[SimpleDateFormat]) should be (false)
      }
    }
  }

  def getId[T: CanBeIdentifier](target: T) = implicitly[CanBeIdentifier[T]].toIdentifier(target)
}