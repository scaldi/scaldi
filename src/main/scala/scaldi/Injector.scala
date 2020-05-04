package scaldi

import scaldi.util.Util._
import annotation.implicitNotFound

/**
  * A base entity that encapsulates the binding lookup mechanism
  */
trait Injector {
  /**
   * Single binding lookup
   * @param identifiers binding's identifiers
   * @return option with binding (`None` if not found)
   */
  def getBinding(identifiers: List[Identifier]): Option[Binding]

  /**
   * List of bindings lookup
   * @param identifiers bindings identifiers
   * @return list of found bindings
   */
  def getBindings(identifiers: List[Identifier]): List[Binding]

  /**
    * Composes two injectors.
    * Polymorphic, needs CanCompose trait implementation available in the scope
    * @param other other injector to be composed with
    * @param comp CanCompose implementation that will handle the composition of the two injectors
    * @tparam I injector's type to be composed with
    * @tparam R resulting injector's type
    * @return composed injector, depends on CanCompose trait's implementation
    */
  def ++[I <: Injector, R <: Injector](other: I)(implicit comp: CanCompose[this.type, I, R]): R = comp.compose(this, other)

  /**
    * Composes two injectors.
    * Note that the operands are inverted because the method starts with ":"
    * Polymorphic, needs CanCompose trait implementation available in the scope
    * @param other other injector to be composed with
    * @param comp CanCompose implementation that will handle the composition of the two injectors
    * @tparam I injector's type to be composed with
    * @tparam R resulting injector's type
    * @return composed injector, depends on CanCompose trait's implementation
    */
  def ::[I <: Injector, R <: Injector](other: I)(implicit comp: CanCompose[I, this.type, R]): R = comp.compose(other, this)
}

/**
  * Mutable injectors that have a lifecycle associated with them.
  */
trait MutableInjector extends Injector

/**
  * Immutable injectors that don't have any lifecycle associated with it.
  */
trait ImmutableInjector extends Injector

object Injector extends LowPriorityImmutableInjectorComposition {

  /**
    * Provides an implementation for implicit `CanCompose` for a composition between
    * an empty injector and an another empty injector
    */
  implicit object nilWithNilComposition extends CanCompose[NilInjector.type, NilInjector.type, NilInjector.type] {
    def compose(cmp1: NilInjector.type, cmp2: NilInjector.type): NilInjector.type = NilInjector
  }

  /**
    * Provides an implementation for implicit `CanCompose` for a composition between
    * an empty injector with a non-empty injector
    */
  implicit def nilWithOtherInjectorComposition[I <: Injector]: CanCompose[NilInjector.type, I, I] = new CanCompose[NilInjector.type, I, I] {
    def compose(cmp1: NilInjector.type, cmp2: I) = cmp2
  }

  /**
    * Provides an implementation for implicit `CanCompose` for a composition between
    * an non-empty injector and an empty injector
    */
  implicit def otherInjectorWithNilComposition[I <: Injector]: CanCompose[I, NilInjector.type, I] = new CanCompose[I, NilInjector.type, I] {
    def compose(cmp1: I, cmp2: NilInjector.type) = cmp1
  }
}

trait LowPriorityImmutableInjectorComposition extends LowPriorityMutableInjectorComposition {
  /**
    * Provides an implementation for implicit `CanCompose` for a composition between
    * two immutable injectors
    * @tparam I1 immutable injector
    * @tparam I2 immutable injector
    * @return composition between two immutable injectors
    */
  implicit def immutableComposition[I1 <: ImmutableInjector, I2 <: ImmutableInjector]: CanCompose[I1, I2, ImmutableInjectorAggregation] =
    new CanCompose[I1, I2, ImmutableInjectorAggregation] {
      def compose(cmp1: I1, cmp2: I2) = new ImmutableInjectorAggregation(List(cmp1, cmp2))
    }
}

trait LowPriorityMutableInjectorComposition {
  /**
    * Provides an implementation for implicit `CanCompose` for a composition between
    * two mutable injectors
    * @tparam I1 mutable injector
    * @tparam I2 mutable injector
    * @return composition between two mutable injectors
    */
  implicit def mutableInjectorComposition[I1 <: Injector, I2 <: Injector]: CanCompose[I1, I2, MutableInjectorAggregation] =
    new CanCompose[I1, I2, MutableInjectorAggregation] {
      def compose(cmp1: I1, cmp2: I2) = new MutableInjectorAggregation(List(cmp1, cmp2))
    }
}

/**
  * A very simple implementation of an injector that just delegates the binding lookup
  * to some other injector. This will protect the lifecycle of the delegated injector from
  * any changes. It is useful in a case of a scoped binding: if you want to use the injector
  * in a composition, but don't want the composition's lifecycle to influence it. In that case,
  * before creating the composition wrap the injector into the `ImmutableWrapper`.
  * @param delegate another Injector to which delegate the binding
  */
class ImmutableWrapper(delegate: Injector) extends Injector with ImmutableInjector {
  def getBinding(identifiers: List[Identifier]): Option[Binding] =
    delegate.getBinding(identifiers)

  def getBindings(identifiers: List[Identifier]): List[Binding] =
    delegate.getBindings(identifiers)
}

object ImmutableWrapper {
  /**
    * Standard factory method for Immutable Wrapper.
    * All binding lookup will be delegated to the injector provided in the parameters
    * @param delegate injector to which delegate binding lookup
    * @return ImmutableWrapper
    */
  def apply(delegate: Injector) = new ImmutableWrapper(delegate)
}

/**
  * Typeclass that will handle the composition of different injectors
  * @tparam A type of the first injector in the composition
  * @tparam B type of the second injector in the composition
  * @tparam R type of the resulting injector in the composition
  */
@implicitNotFound(msg = "Cannot compose ${A} with ${B}. Please consider defining CanCompose for such composition.")
trait CanCompose[-A, -B, +R] {
  /**
    * Composes two injectors
    * @param cmp1 first injector in the composition
    * @param cmp2 second injector in the composition
    * @return composed injector
    */
  def compose(cmp1: A, cmp2: B): R
}

/**
  * The result of the composition of two or more of the immutable injectors
  * @param chain currently composed immutable injectors
  */
class ImmutableInjectorAggregation(chain: List[ImmutableInjector]) extends Injector {

  /**
    * @inheritdoc
    */
  def getBinding(identifiers: List[Identifier]): Option[Binding] = chain.view.map(_ getBinding identifiers).collectFirst{case Some(b) => b}

  /**
    * @inheritdoc
    */
  def getBindings(identifiers: List[Identifier]): List[Binding] = chain.flatMap(_ getBindings identifiers)
}

/**
  * The result of the composition of two or more of the mutable injectors
  * @param chain currently composed immutable injectors
  */
class MutableInjectorAggregation(chain: List[Injector]) extends InjectorWithLifecycle[MutableInjectorAggregation]
                                                           with MutableInjectorUser
                                                           with ShutdownHookLifecycleManager {
  initInjector(this)

  /**
    * Goes through composed injectors to find a binding having identifiers
    * @param identifiers list of identifiers identifying a dependency
    * @return a binding identified by identifiers
    */
  def getBindingInternal(identifiers: List[Identifier]): Option[BindingWithLifecycle] =
    chain.view.map {
      case child: InjectorWithLifecycle[_] => child getBindingInternal identifiers
      case child => child getBinding identifiers map BindingWithLifecycle.apply
    }.collectFirst{case Some(b) => b}

  /**
    * Goes through composed injectors to find bindings having identifiers
    * @param identifiers list of identifiers identifying dependencies
    * @return a list of bindings identified by identifiers
    */
  def getBindingsInternal(identifiers: List[Identifier]): List[BindingWithLifecycle] =
    chain.flatMap {
      case child: InjectorWithLifecycle[_] => child getBindingsInternal identifiers
      case child => child getBindings identifiers map BindingWithLifecycle.apply
    }

  /**
    * Mutates current injector replacing it with the one in the parameters
    * @param newParentInjector the replacement for current injector
    */
  override def injector_=(newParentInjector: Injector): Unit = {
    super.injector_=(newParentInjector)
    initInjector(newParentInjector)
  }

  /**
    * Initialize bindings that are not lazy in composed injectors
    * @param lifecycleManager entity that will manage the lifecycle of the eager bindings
    */
  protected def init(lifecycleManager: LifecycleManager): () => Unit = {
    val childInits = chain.flatMap {
      case childInjector: InjectorWithLifecycle[_] => Some(childInjector.partialInit(lifecycleManager))
      case _ => None
    }.flatten

    () => childInits.foreach(_())
  }

  /**
    * Initialize all injectors in the composition by setting their parent injector to the one
    * passed in the parameters.
    * @param newParentInjector the injector which will be parent injector for composed injectors
    */
  private def initInjector(newParentInjector: Injector): Unit = {
    chain foreach {
      case childInjector: MutableInjectorUser => childInjector.injector = newParentInjector
      case _ => // skip
    }
  }
}

/**
  * Trait for injectors that are freezable i.e. may no longer be modified from e certain point
  */
trait Freezable {
  /**
    * Determines if the injector is frozen
    * @return true if injector may no longer be modified, false otherwise
    */
  protected def isFrozen: Boolean
}

/**
  * Contains implicit reference to injector: the final injector composition which is used by inject.
  * Injector aggregation will set it during the initialization phase
  */
trait MutableInjectorUser extends MutableInjector { self: Injector with Freezable =>
  private var _injector: Injector = this

  implicit def injector: Injector = _injector

  implicit val injectorFn: () => Injector = () => injector

  /**
    * Mutates current injector replacing it with the one in the parameters.
    * Works only if current injector is not frozen
    * @param newParentInjector the replacement for current injector
    */
  def injector_=(newParentInjector: Injector): Unit = {
    if (isFrozen) throw new InjectException("Injector already frozen, so you can't mutate it anymore!")

    _injector = newParentInjector
  }
}

/**
  * Trait for injectors that are initializeable i.e. have initialization phase
  * @tparam I type for initializeable injector
  */
trait Initializeable[I] extends Freezable { this: I with LifecycleManager =>
  private var initialized = false

  /**
    * Initializes binding that are not lazy
    * @return initializeable injector
    */
  def initNonLazy() : I = partialInit(this) match {
    case Some(fn) =>
      fn()
      this
    case None =>
      this
  }

  /**
    * Composes function for initialization
    * @param lifecycleManager where to look for initializations
    * @return initialization function
    */
  def partialInit(lifecycleManager: LifecycleManager) : Option[() => Unit] = {
    if (!initialized) {
      val initFn = init(lifecycleManager)

      initialized = true
      Some(initFn)
    } else None
  }

  protected def isFrozen: Boolean = initialized

  /**
   * Initializes bindings that are not Lazy
   * @param lifecycleManager entity that will manage the lifecycle of the eager bindings
   */
  protected def init(lifecycleManager: LifecycleManager): () => Unit
}

/**
  * Trait for injectors that have lifecycle
  * @tparam I type for initializeable injector
  */
trait InjectorWithLifecycle[I <: InjectorWithLifecycle[I]] extends Injector with Initializeable[I] with MutableInjector {
  this: I with LifecycleManager with Initializeable[I] =>

  /**
   * @inheritdoc
   */
  final def getBinding(identifiers: List[Identifier]): Option[Binding] = initNonLazy() |> (_ getBindingInternal identifiers map (Binding.apply(this, _)))

  /**
   * @inheritdoc
   */
  final def getBindings(identifiers: List[Identifier]): List[Binding] = initNonLazy() |> (_ getBindingsInternal identifiers map (Binding.apply(this, _)))

  /**
   * Binding lookup logic
   * @param identifiers list of identifiers identifying a dependency
   * @return a binding identified by identifiers
   */
  def getBindingInternal(identifiers: List[Identifier]): Option[BindingWithLifecycle]

  /**
   * Bindings lookup logic
   * @param identifiers list of identifiers identifying dependencies
   * @return a list of bindings identified by identifiers
   */
  def getBindingsInternal(identifiers: List[Identifier]): List[BindingWithLifecycle]
}

/**
  * Custom exception for injectors
  * @param message String message describing the problem
  */
class InjectException(message: String) extends RuntimeException(message)