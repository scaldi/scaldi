package scaldi

import scaldi.util.Util._

/**
  * Entities that extend `Identifiable` can be identified using Scaldi identifiers.
  * They can also contain conditions.
  */
trait Identifiable {
  def identifiers: List[Identifier]

  /**
    * Stores condition for binding.
    * Used for conditional binding
    * @return `Option` with function returning condition
    */
  def condition: Option[() => Condition]

  protected lazy val resolvedCondition = condition map (fn ⇒ fn())

  def isDefinedFor(desiredIdentifiers: List[Identifier]) =
    Identifier.sameAs(identifiers, desiredIdentifiers) &&
      resolvedCondition.fold(true)(_ satisfies desiredIdentifiers)

  /**
    * Defines if binding is lazy or not
    * `false` by default
    *
    * @return `true` if not lazy, `false` otherwise
    */
  def isEager: Boolean = false

  /**
    * Specifies if binding is cacheable
    * False by default
    * @return `Boolean` true if binding is cacheable, false otherwise
    */
  def isCacheable: Boolean = false
}

/**
  * Binding is defined in a `Module` as Type -> instance relationship (sometimes
  * with additional identifiers).
  * I can be injected in classes where implicit `Injector` is available.
  * Binding can be defined (containing a value) or undefined.
  */
trait Binding extends Identifiable {
  /**
    * Retrieves stored binding's value, used during binding lookup during injection.
    * If equals to `None`, binding is considered undefined
    * @return `Option` with binding's value (or `None` if the binding is undefined)
    */
  def get: Option[Any]
}

object Binding {
  /**
    * Standard factory method for binding that from `LifecycleManager` and
    * `BindingWithLifecycle` creates new `Binding`
    *
    * @param lifecycleManager `LifecycleManager` that will handle initialization / destruction events for binding
    * @param binding `BindingWithLifecycle` actual binding
    * @return a new binding (without lifecycle) which delegates all method calls to the provided `binding`
    */
  def apply(lifecycleManager: LifecycleManager, binding: BindingWithLifecycle) = new Binding {
    def get = binding get lifecycleManager

    def condition = binding.condition

    def identifiers = binding.identifiers

    override def isEager = binding.isEager

    override def isCacheable = binding.isCacheable
  }
}

/**
  * Bindings with lifecycle may have initialization and destruction handlers
  */
trait BindingWithLifecycle extends Identifiable {
  /**
    * Bindings with lifecycle should have `BindingLifecycle` attached to them
    * @return `BindingLifecycle` current binding's lifecycle
    */
  def lifecycle: BindingLifecycle[Any]

  /**
    * Retrieves stored binding's value, used during binding lookup during injection.
    * If equals to `None`, binding is considered undefined
    *
    * @param lifecycleManager `LifecycleManager` that will handle the destruction of the binding at the end of the lifecycle
    * @return `Option` with binding's value (or `None` if the binding is undefined)
    */
  def get(lifecycleManager: LifecycleManager): Option[Any]

  def init(lifecycleManager: LifecycleManager): Unit = {
    resolvedCondition match {
      case None ⇒
        get(lifecycleManager)
      case Some(c) if c.dynamic || c.satisfies(Nil) ⇒
        get(lifecycleManager)
      case _ ⇒
      // do nothing because eager binding should not be initialized
    }
  }
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

/**
  * Binding that is initialized at the Injector initialization phase (normally during an
  * application startup) and never changes afterwards.
  *
  * @param createFn function that creates and returns value that will be associated with binding
  * @param identifiers binding's identifiers (empty by default)
  * @param condition binding's conditions (empty by default)
  * @param lifecycle binding's lifecycle (empty by default)
  */
case class NonLazyBinding(private val createFn: Option[() => Any],
                           identifiers: List[Identifier] = Nil,
                           condition: Option[() => Condition] = None,
                           lifecycle: BindingLifecycle[Any] = BindingLifecycle.empty) extends BindingWithLifecycle {
  lazy val target = createFn map (fn => fn() <| lifecycle.initializeObject)
  var destroyableAdded = false

  /**
    * @inheritdoc
    */
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

  /**
    * @inheritdoc
    */
  override def isEager = true

  /**
    * @inheritdoc
    */
  override def isCacheable = true
}

/**
  * Binding that is initialized on on first use and never changes afterwards.
  *
  * @param createFn function that creates and returns value that will be associated with binding
  * @param identifiers binding's identifiers (empty by default)
  * @param condition binding's conditions (empty by default)
  * @param lifecycle binding's lifecycle (empty by default)
  */
case class LazyBinding(private val createFn: Option[() => Any],
                        identifiers: List[Identifier] = Nil,
                        condition: Option[() => Condition] = None,
                        lifecycle: BindingLifecycle[Any] = BindingLifecycle.empty) extends BindingWithLifecycle {
  lazy val target = createFn map (fn => fn() <| lifecycle.initializeObject)
  var destroyableAdded = false

  /**
    * @inheritdoc
    */
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

  /**
    * @inheritdoc
    */
  override def isCacheable = true
}

/**
  * Binding that is initialized on every injection with createFn function.
  *
  * @param createFn function that creates and returns value that will be associated with binding
  * @param identifiers binding's identifiers (empty by default)
  * @param condition binding's conditions (empty by default)
  * @param lifecycle binding's lifecycle (empty by default)
  */
case class ProviderBinding(private val createFn: () => Any,
                            identifiers: List[Identifier] = Nil,
                            condition: Option[() => Condition] = None,
                            lifecycle: BindingLifecycle[Any] = BindingLifecycle.empty) extends BindingWithLifecycle {
  def target = createFn() <| lifecycle.initializeObject

  /**
    * @inheritdoc
    */
  override def get(lifecycleManager: LifecycleManager) = {
    val value = target
    lifecycle.destroy foreach (d => lifecycleManager addDestroyable (() => d(value)))
    Some(value)
  }
}

/**
  * Binding that only contains a value.
  * As it is just a fixed value, no lifecycle is needed
  *
  * @param boundValue binding's value
  * @param identifiers binding's identifiers (empty by default)
  * @param condition binding's conditions (empty by default)
  * @param cacheable if binding should be cacheable (false by default)
  * @param eager if binding is eager (non lazy) (false by default)
  * @tparam T binding's type
  */
case class SimpleBinding[T](boundValue: Option[() => T],
                             identifiers: List[Identifier] = Nil,
                             condition: Option[() => Condition] = None,
                             cacheable: Boolean = false,
                             eager: Boolean = false) extends Binding {

  /**
    * @inheritdoc
    */
  lazy val get = boundValue map (fn => fn())

  /**
    * @inheritdoc
    */
  override def isCacheable = cacheable && condition.isEmpty

  /**
    * @inheritdoc
    */
  override def isEager = eager
}