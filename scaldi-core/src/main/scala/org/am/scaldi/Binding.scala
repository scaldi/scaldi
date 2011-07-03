package org.am.scaldi

trait Binding {
  protected val identifiers: List[Identifier]

  def get: Option[Any]
  def hasIdentifiers(desired: List[Identifier]) = desired forall (d => identifiers exists (_ sameAs d))
}

case class NonLazyBinding(private val createFn: Option[() => Any], identifiers: List[Identifier] = Nil) extends Binding {
  val target = createFn map (_())
  def get = target
}

case class LazyBinding(private val createFn: Option[() => Any], identifiers: List[Identifier] = Nil) extends Binding {
  lazy val target = createFn map (_())
  def get = target
}

case class ProviderBinding(private val createFn: () => Any, identifiers: List[Identifier] = Nil) extends Binding {
  def target = createFn ()
  def get = Some(target)
}