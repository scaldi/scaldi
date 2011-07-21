package org.am.scaldi

import org.am.scaldi.util.Util._

trait Injector {
  def getBinding(identifiers: List[Identifier]): Option[Binding]
  def getBindings(identifiers: List[Identifier]): List[Binding]

  def compose(other: Injector): Injector = InjectorAggregation(List(this, other))

  def ++(other: Injector): Injector = compose(other)

  def ::(other: Injector): Injector = InjectorAggregation(List(other, this))
}

object InjectorAggregation {
  def apply(chain: List[Injector]) = chain filter (NilInjector !=) match {
    case Nil => NilInjector
    case inj :: Nil => inj
    case c @ inj :: rest =>
      (c
        find (i => i.isInstanceOf[MutableInjectorUser] || i.isInstanceOf[InitializeableInjector[_]])
        map (u => new MutableInjectorAggregation(chain))
        getOrElse new ImmutableInjectorAggregation(chain))
  }
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

trait MutableInjectorUser { self: Injector with Freezable =>
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

trait InitializeableInjector[I <: InitializeableInjector[I]] extends Injector with Initializeable[I] { this: I =>
  def getBinding(identifiers: List[Identifier]) = initNonLazy() |> (_.getBindingInternal(identifiers))
  def getBindings(identifiers: List[Identifier]) = initNonLazy() |> (_.getBindingsInternal(identifiers))

  def getBindingInternal(identifiers: List[Identifier]): Option[Binding]
  def getBindingsInternal(identifiers: List[Identifier]): List[Binding]
}

class InjectException(message: String) extends RuntimeException(message)