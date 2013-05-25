package scaldi

trait Binding {
  protected val identifiers: List[Identifier]
  protected val condition: Option[() => Condition]

  def get: Option[Any]
  def isDefinedFor(desiredIdentifiers: List[Identifier]) =
    (desiredIdentifiers forall (d => identifiers exists (_ sameAs d))) &&
        (condition map (_() satisfies desiredIdentifiers) getOrElse true)
}

case class NonLazyBinding(
   private val createFn: Option[() => Any],
   identifiers: List[Identifier] = Nil,
   condition: Option[() => Condition] = None
) extends Binding {
  lazy val target = createFn map (_())
  def get = target
}

case class LazyBinding(
  private val createFn: Option[() => Any],
  identifiers: List[Identifier] = Nil,
  condition: Option[() => Condition] = None
) extends Binding {
  lazy val target = createFn map (_())
  def get = target
}

case class ProviderBinding(
  private val createFn: () => Any,
  identifiers: List[Identifier] = Nil,
  condition: Option[() => Condition] = None
) extends Binding {
  def target = createFn()
  def get = Some(target)
}