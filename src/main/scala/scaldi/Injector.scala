package scaldi

import scaldi.util.Util._
import annotation.implicitNotFound

trait Injector {
  def getBinding(identifiers: List[Identifier]): Option[Binding]
  def getBindings(identifiers: List[Identifier]): List[Binding]

  def ++[I <: Injector, R <: Injector](other: I)(implicit comp: CanCompose[this.type, I, R]): R = comp.compose(this, other)
  def ::[I <: Injector, R <: Injector](other: I)(implicit comp: CanCompose[I, this.type, R]): R = comp.compose(other, this)
}

object Injector extends LowPriorityMutableWithOtherInjectorCompositions {
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

trait LowPriorityMutableWithOtherInjectorCompositions extends LowPriorityOtherWithMutableInjectorCompositions {
  implicit def mutableInjectorWithOtherComposition[I1 <: Injector with MutableInjector, I2 <: Injector] = new CanCompose[I1, I2, MutableInjectorAggregation] {
    def compose(cmp1: I1, cmp2: I2) = new MutableInjectorAggregation(List(cmp1, cmp2))
  }
}

trait LowPriorityOtherWithMutableInjectorCompositions extends LowPriorityImmutableInjectorCompositions {
  implicit def otherInjectorWithMutableComposition[I1 <: Injector, I2 <: Injector with MutableInjector] = new CanCompose[I1, I2, MutableInjectorAggregation] {
    def compose(cmp1: I1, cmp2: I2) = new MutableInjectorAggregation(List(cmp1, cmp2))
  }
}

trait LowPriorityImmutableInjectorCompositions {
   implicit def immutableComposition[I1 <: Injector, I2 <: Injector] = new CanCompose[I1, I2, ImmutableInjectorAggregation] {
    def compose(cmp1: I1, cmp2: I2) = new ImmutableInjectorAggregation(List(cmp1, cmp2))
  }
}

@implicitNotFound(msg = "Cannot compose ${A} with ${B}. Please consider defining CanCompose for such composition.")
trait CanCompose[-A, -B, +R] {
  def compose(cmp1: A, cmp2: B): R
}

class ImmutableInjectorAggregation(chain: List[Injector]) extends Injector {
  def getBinding(identifiers: List[Identifier]) = chain.view.map(_ getBinding identifiers).collectFirst{case Some(b) => b}
  def getBindings(identifiers: List[Identifier]) = chain.flatMap(_ getBindings identifiers)
}

class MutableInjectorAggregation(chain: List[Injector]) extends InitializeableInjector[MutableInjectorAggregation] with MutableInjectorUser {
  initInjector(this)

  def getBindingInternal(identifiers: List[Identifier]) = chain.view.map(_ getBinding identifiers).collectFirst{case Some(b) => b}
  def getBindingsInternal(identifiers: List[Identifier]) = chain.flatMap(_ getBindings identifiers)

  override def injector_=(newParentInjector: Injector) {
    initInjector(newParentInjector)
    super.injector_=(newParentInjector)
  }

  protected def init() = {
    val childInits = chain.flatMap {
      case childInjector: InitializeableInjector[_] => Some(childInjector.partialInit())
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

trait Initializeable[I] extends Freezable { this: I =>
  private var initialized = false

  def initNonLazy() : I = partialInit() match {
    case Some(fn) =>
      fn()
      this
    case None =>
      this
  }

  def partialInit() : Option[() => Unit] = {
    if (!initialized) {
      val initFn = init()

      initialized = true
      Some(initFn)
    } else None
  }

  protected def isFrozen = initialized

  protected def init(): () => Unit
}

trait InitializeableInjector[I <: InitializeableInjector[I]] extends Injector with Initializeable[I] with MutableInjector { this: I =>
  final def getBinding(identifiers: List[Identifier]) = initNonLazy() |> (_.getBindingInternal(identifiers))
  final def getBindings(identifiers: List[Identifier]) = initNonLazy() |> (_.getBindingsInternal(identifiers))

  def getBindingInternal(identifiers: List[Identifier]): Option[Binding]
  def getBindingsInternal(identifiers: List[Identifier]): List[Binding]
}

trait MutableInjector

class InjectException(message: String) extends RuntimeException(message)