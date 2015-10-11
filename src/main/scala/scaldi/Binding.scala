package scaldi

import scaldi.util.Util._

trait Identifiable {
  def identifiers: List[Identifier]

  /**
   * Stores condition for binding.
   * Used for conditional binding
   * @return `Option` with function returning condition
   */
  def condition: Option[() => Condition]

  def isDefinedFor(desiredIdentifiers: List[Identifier]) =
    Identifier.sameAs(identifiers, desiredIdentifiers) &&
      (condition map (_() satisfies desiredIdentifiers) getOrElse true)

  def isEager: Boolean = false

  /**
   * Specifies if binding is cacheable
   * @return true if binding is cacheable, false otherwise
   */
  def isCacheable: Boolean = false
}

trait Binding extends Identifiable {
  /**
   * Retrieves stored binding's value, used during binding lookup during injection.
   * If equals to `None`, binding is considered undefined
   * @return `Option` with binding's value (or `None` if the binding is undefined)
   */
  def get: Option[Any]
}

object Binding {
  def apply(lifecycleManager: LifecycleManager, binding: BindingWithLifecycle) = new Binding {
    def get = binding get lifecycleManager
    def condition = binding.condition
    def identifiers = binding.identifiers

    override def isEager = binding.isEager
    override def isCacheable = binding.isCacheable
  }
}

trait BindingWithLifecycle extends Identifiable {
  def lifecycle: BindingLifecycle[Any]
  def get(lifecycleManager: LifecycleManager): Option[Any]
}

object BindingWithLifecycle {
  def apply(binding: Binding) = new BindingWithLifecycle {
    val lifecycle = BindingLifecycle.empty

    def condition = binding.condition
    def identifiers = binding.identifiers

    def get(lifecycleManager: LifecycleManager) = binding.get

    override def isEager = binding.isEager
    override def isCacheable = binding.isCacheable
  }
}

case class NonLazyBinding(
   private val createFn: Option[() => Any],
   identifiers: List[Identifier] = Nil,
   condition: Option[() => Condition] = None,
   lifecycle: BindingLifecycle[Any] = BindingLifecycle.empty
) extends BindingWithLifecycle {
  lazy val target = createFn map (_() <| lifecycle.initializeObject)
  var destroyableAdded = false

  override def get(lifecycleManager: LifecycleManager) = {
    for {
      d <- lifecycle.destroy
      t <- target
      if !destroyableAdded
    } {
      lifecycleManager addDestroyable (() => d(t))
      destroyableAdded = true
    }

    target
  }

  override def isEager = true
  override def isCacheable = true
}

case class LazyBinding(
  private val createFn: Option[() => Any],
  identifiers: List[Identifier] = Nil,
  condition: Option[() => Condition] = None,
  lifecycle: BindingLifecycle[Any] = BindingLifecycle.empty
) extends BindingWithLifecycle {
  lazy val target = createFn map (_() <| lifecycle.initializeObject)
  var destroyableAdded = false

  override def get(lifecycleManager: LifecycleManager) = {
    for {
      d <- lifecycle.destroy
      t <- target
      if !destroyableAdded
    } {
      lifecycleManager addDestroyable (() => d(t))
      destroyableAdded = true
    }

    target
  }

  override def isCacheable = true
}

case class ProviderBinding(
  private val createFn: () => Any,
  identifiers: List[Identifier] = Nil,
  condition: Option[() => Condition] = None,
  lifecycle: BindingLifecycle[Any] = BindingLifecycle.empty
) extends BindingWithLifecycle {
  def target = createFn() <| lifecycle.initializeObject

  override def get(lifecycleManager: LifecycleManager) = {
    val value = target
    lifecycle.destroy foreach (d => lifecycleManager addDestroyable (() => d(value)))
    Some(value)
  }
}

case class SimpleBinding[T](
  boundValue: Option[() => T],
  identifiers: List[Identifier] = Nil,
  condition: Option[() => Condition] = None,
  cacheable: Boolean = false,
  eager: Boolean = false
) extends Binding {
  lazy val get = boundValue map (_())

  override def isCacheable = cacheable && condition.isEmpty
  override def isEager = eager
}