package scaldi

trait Condition {
  def satisfies(identifiers: List[Identifier]): Boolean

  def unary_! = NotCondition(this)
  def and(otherCond: Condition) = AndCondition(List(this, otherCond))
  def or(otherCond: Condition) = OrCondition(List(this, otherCond))
}

object Condition {
  def apply(fn: => Boolean) = new Condition {
    def satisfies(identifiers: List[Identifier]) = fn
  }

  def apply(fn: (List[Identifier]) => Boolean) = new Condition {
    def satisfies(identifiers: List[Identifier]) = fn(identifiers)
  }
}

case class OrCondition (conditions: List[Condition]) extends Condition {
  def satisfies(identifiers: List[Identifier]) =
    conditions exists (_ satisfies (identifiers))
}

case class AndCondition (conditions: List[Condition]) extends Condition {
  def satisfies(identifiers: List[Identifier]) =
    conditions forall (_ satisfies (identifiers))
}

case class NotCondition (condition: Condition) extends Condition {
  def satisfies(identifiers: List[Identifier]) =
    !(condition satisfies (identifiers))
}

case class SysPropCondition(name: String, value: Option[String] = None) extends Condition {
  def satisfies(identifiers: List[Identifier]) =
    (value, System.getProperty(name)) match {
      case (Some(v), r) if v == r => true
      case (None, r) if r != null => true
      case _ => false
    }
}