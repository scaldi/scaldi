package scaldi

import scaldi.util.Util._
import annotation.implicitNotFound

trait Injector {
  def getBinding(identifiers: List[Identifier]): Option[Binding]
  def getBindings(identifiers: List[Identifier]): List[Binding]

  def ++[I <: Injector, R <: Injector](other: I)(implicit comp: CanCompose[this.type, I, R]): R = comp.compose(this, other)
  def ::[I <: Injector, R <: Injector](other: I)(implicit comp: CanCompose[I, this.type, R]): R = comp.compose(other, this)
}

trait MutableInjector extends Injector
trait ImmutableInjector extends Injector

object Injector extends LowPriorityImmutableInjectorComposition {
  implicit object nilWithNilComposition extends CanCompose[NilInjector.type, NilInjector.type, NilInjector.type] {
    def compose(cmp1: NilInjector.type, cmp2: NilInjector.type) = NilInjector
  }

  implicit def nilWithOtherInjectorComposition[I <: Injector] = new CanCompose[NilInjector.type, I, I] {
    def compose(cmp1: NilInjector.type, cmp2: I) = cmp2
  }

  implicit def otherInjectorWithNilComposition[I <: Injector] = new CanCompose[I, NilInjector.type, I] {
    def compose(cmp1: I, cmp2: NilInjector.type) = cmp1
  }
}

trait LowPriorityImmutableInjectorComposition extends LowPriorityMutableInjectorComposition {
  implicit def immutableComposition[I1 <: ImmutableInjector, I2 <: ImmutableInjector] =
    new CanCompose[I1, I2, ImmutableInjectorAggregation] {
      def compose(cmp1: I1, cmp2: I2) = new ImmutableInjectorAggregation(List(cmp1, cmp2))
    }
}

trait LowPriorityMutableInjectorComposition {
  implicit def mutableInjectorComposition[I1 <: Injector, I2 <: Injector] =
    new CanCompose[I1, I2, MutableInjectorAggregation] {
      def compose(cmp1: I1, cmp2: I2) = new MutableInjectorAggregation(List(cmp1, cmp2))
    }
}

@implicitNotFound(msg = "Cannot compose ${A} with ${B}. Please consider defining CanCompose for such composition.")
trait CanCompose[-A, -B, +R] {
  def compose(cmp1: A, cmp2: B): R
}

class ImmutableInjectorAggregation(chain: List[ImmutableInjector]) extends Injector {
  def getBinding(identifiers: List[Identifier]) = chain.view.map(_ getBinding identifiers).collectFirst{case Some(b) => b}
  def getBindings(identifiers: List[Identifier]) = chain.flatMap(_ getBindings identifiers)
}

class MutableInjectorAggregation(chain: List[Injector]) extends InjectorWithLifecycle[MutableInjectorAggregation]
                                                           with MutableInjectorUser
                                                           with ShutdownHookLifecycleManager {
  initInjector(this)

  def getBindingInternal(identifiers: List[Identifier]) =
    chain.view.map {
      case child: InjectorWithLifecycle[_] => child getBindingInternal identifiers
      case child => child getBinding identifiers map BindingWithLifecycle.apply
    }.collectFirst{case Some(b) => b}

  def getBindingsInternal(identifiers: List[Identifier]) =
    chain.flatMap {
      case child: InjectorWithLifecycle[_] => child getBindingsInternal identifiers
      case child => child getBindings identifiers map BindingWithLifecycle.apply
    }

  override def injector_=(newParentInjector: Injector) {
    super.injector_=(newParentInjector)
    initInjector(newParentInjector)
  }

  protected def init(lifecycleManager: LifecycleManager) = {
    val childInits = chain.flatMap {
      case childInjector: InjectorWithLifecycle[_] => Some(childInjector.partialInit(lifecycleManager))
      case _ => None
    }.flatten

    () => childInits.foreach(_())
  }

  private def initInjector(newParentInjector: Injector) {
    chain foreach {
      case childInjector: MutableInjectorUser => childInjector.injector = newParentInjector
      case _ => // skip
    }
  }
}

trait Freezable {
  protected def isFrozen: Boolean
}

trait MutableInjectorUser extends MutableInjector { self: Injector with Freezable =>
  private var _injector: Injector = this

  implicit def injector = _injector

  def injector_=(newParentInjector: Injector) {
    if (isFrozen) throw new InjectException("Injector already frozen, so you can't mutate it anymore!")

    _injector = newParentInjector
  }
}

trait Initializeable[I] extends Freezable { this: I with LifecycleManager =>
  private var initialized = false

  def initNonLazy() : I = partialInit(this) match {
    case Some(fn) =>
      fn()
      this
    case None =>
      this
  }

  def partialInit(lifecycleManager: LifecycleManager) : Option[() => Unit] = {
    if (!initialized) {
      val initFn = init(lifecycleManager)

      initialized = true
      Some(initFn)
    } else None
  }

  protected def isFrozen = initialized

  protected def init(lifecycleManager: LifecycleManager): () => Unit
}

trait InjectorWithLifecycle[I <: InjectorWithLifecycle[I]] extends Injector with Initializeable[I] with MutableInjector {
  this: I with LifecycleManager =>

  final def getBinding(identifiers: List[Identifier]) = initNonLazy() |> (_ getBindingInternal identifiers map (Binding.apply(this, _)))
  final def getBindings(identifiers: List[Identifier]) = initNonLazy() |> (_ getBindingsInternal identifiers map (Binding.apply(this, _)))

  def getBindingInternal(identifiers: List[Identifier]): Option[BindingWithLifecycle]
  def getBindingsInternal(identifiers: List[Identifier]): List[BindingWithLifecycle]
}

class InjectException(message: String) extends RuntimeException(message)