package scaldi

trait Condition {
  def satisfies(identifiers: List[Identifier]): Boolean

  def unary_! = NotCondition(this)
  def and(otherCond: Condition) = AndCondition(List(this, otherCond))
  def or(otherCond: Condition) = OrCondition(List(this, otherCond))

  /**
    * Defines whether this condition can change while `Injector` is still alive.
    *
    * This will influence how non-lazy bindings are initialized. Non-dynamic conditions
    * would be checked during the initialization phase of an `Injector` (with empty list of identifiers) and if
    * condition returns `false`, then non-lazy binding would not be initialized.
    */
  def dynamic: Boolean = true
}

object Condition {
  def apply(fn: => Boolean): Condition = new Condition {
    def satisfies(identifiers: List[Identifier]): Boolean = fn
  }

  def apply(fn: => Boolean, dynamic: Boolean): Condition = {
    val dyn = dynamic

    new Condition {
      def satisfies(identifiers: List[Identifier]): Boolean = fn
      override val dynamic: Boolean = dyn
    }
  }

  def apply(fn: List[Identifier] => Boolean): Condition = new Condition {
    def satisfies(identifiers: List[Identifier]): Boolean = fn(identifiers)
  }

  def apply(fn: List[Identifier] => Boolean, dynamic: Boolean): Condition = {
    val dyn = dynamic

    new Condition {
      def satisfies(identifiers: List[Identifier]): Boolean = fn(identifiers)
      override val dynamic: Boolean = dyn
    }
  }
}

final case class OrCondition (conditions: List[Condition]) extends Condition {
  def satisfies(identifiers: List[Identifier]): Boolean =
    conditions exists (_ satisfies identifiers)

  override val dynamic: Boolean = conditions.exists(_.dynamic)
}

final case class AndCondition (conditions: List[Condition]) extends Condition {
  def satisfies(identifiers: List[Identifier]): Boolean =
    conditions forall (_ satisfies identifiers)

  override val dynamic: Boolean = conditions.exists(_.dynamic)
}

final case class NotCondition (condition: Condition) extends Condition {
  def satisfies(identifiers: List[Identifier]): Boolean =
    !(condition satisfies identifiers)

  override val dynamic: Boolean = condition.dynamic
}

final case class SysPropCondition(name: String, value: Option[String] = None, override val dynamic: Boolean = false) extends Condition {
  def satisfies(identifiers: List[Identifier]): Boolean =
    (value, System.getProperty(name)) match {
      case (Some(v), r) if v == r => true
      case (None, r) if r != null => true
      case _ => false
    }
}