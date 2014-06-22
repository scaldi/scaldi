package scaldi

import org.scalatest.{Matchers, WordSpec}

class ConditionSpec extends WordSpec with Matchers {
  "Condition" should {
    val onlyTest: PartialFunction[List[Identifier], Boolean] = {
      case List(StringIdentifier("test")) => true
      case _ => false
    }

    val C = Condition

    "be sutisfied if underlying function returns true" in {
      C(true) satisfies Nil should be (true)
      C(onlyTest) satisfies List("test") should be (true)
    }

    "not be sutisfied if underlying function returns false" in {
      C(false) satisfies Nil should be (false)
      C(onlyTest) satisfies List("wrong") should be (false)
    }

    "be able to compose with 'and'" in {
      C(true) and C(false) and C(true) satisfies Nil should be (false)
      C(false) and C(false) satisfies Nil should be (false)
      C(true) and C(true) satisfies Nil should be (true)
    }

    "be able to compose with 'or'" in {
      C(true) or C(false) or C(true) satisfies Nil should be (true)
      C(true) or C(true) satisfies Nil should be (true)
      C(false) or C(false) satisfies Nil should be (false)
    }

    "suport unary not (!)" in {
      !C(true) satisfies Nil should be (false)
      C(false) or !C(false) satisfies Nil should be (true)
      !C(false) and C(true) satisfies Nil should be (true)
    }

    "be able to compose with 'or' and 'and', but precedence is regulated by parentheses" in {
      C(true) or C(false) and C(false) satisfies Nil should be (false)
      C(true) or (C(false) and C(false)) satisfies Nil should be (true)
    }
  }

  "SysPropCondition" should {
    "check presence of system property if value is None" in {
      try {
        SysPropCondition("test-property") satisfies Nil should be(false)
        System.setProperty("test-property", "")
        SysPropCondition("test-property") satisfies Nil should be(true)
      } finally {
        System.clearProperty("test-property")
      }
    }

    "check value of system property if value is set" in {
      try {
        SysPropCondition("test-property", Some("test")) satisfies Nil should be (false)
        System.setProperty("test-property", "")
        SysPropCondition("test-property", Some("test")) satisfies Nil should be (false)
        System.setProperty("test-property", "test")
        SysPropCondition("test-property", Some("test")) satisfies Nil should be (true)
      } finally {
        System.clearProperty("test-property")
      }
    }
  }
}