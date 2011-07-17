package org.am.scaldi

import org.am.scaldi.util.Util._
import sys._

trait Injector {
  def getBinding(identifiers: List[Identifier]): Option[Binding]

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
}

class MutableInjectorAggregation(chain: List[Injector]) extends InitializeableInjector[MutableInjectorAggregation] with MutableInjectorUser {
  initInjector(this)

  def getBindingInternal(identifiers: List[Identifier]) = chain.view.map(_ getBinding identifiers).collectFirst{case Some(b) => b}

  override def injector_=(newParentInjector: Injector) {
    initInjector(newParentInjector)
    super.injector_=(newParentInjector)
  }

  protected def init() = {
    val childInits: List[() => Unit] = chain.flatMap {
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

trait MutableInjectorUser { self: Injector =>
  private var _injector: Injector = this

  implicit def injector = _injector
  def injector_=(newParentInjector: Injector) {
    _injector = newParentInjector
  }
}

trait Initializeable[I] { this: I =>
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

  protected def init(): () => Unit
}

trait InitializeableInjector[I <: InitializeableInjector[I]] extends Injector with Initializeable[I] { this: I =>
  def getBinding(identifiers: List[Identifier]) = initNonLazy() |> (_.getBindingInternal(identifiers))

  def getBindingInternal(identifiers: List[Identifier]): Option[Binding]
}

trait Injectable {
  protected def inject[T](implicit injector: Injector, m: Manifest[T]): T =
    List[Identifier](ClassIdentifier(check(m).erasure)) |>
        (ids => injectWithDefault[T](injector, noBindingFound(ids))(ids))

  protected def inject[T](identifiers: Identifier*)(implicit injector: Injector, m: Manifest[T]): T =
    List[Identifier](ClassIdentifier(check(m).erasure)) ++ identifiers |>
        (ids => injectWithDefault[T](injector, noBindingFound(ids))(ids))

  protected def inject[T](constraints: => InjectConstraints[T])(implicit injector: Injector, m: Manifest[T]): T =
    List(ClassIdentifier(check(m).erasure)) ++ constraints.identifiers |>
      (ids => injectWithDefault[T](injector, constraints.default map(_()) getOrElse noBindingFound(ids))(ids))

  protected def injectWithDefault[T](default: => T)(implicit injector: Injector, m: Manifest[T]): T =
    List(ClassIdentifier(check(m).erasure)) |> injectWithDefault[T](injector, default)

  protected def injectWithDefault[T](identifiers: Identifier*)(default: => T)(implicit injector: Injector, m: Manifest[T]): T =
    List(ClassIdentifier(check(m).erasure)) ++ identifiers |> injectWithDefault[T](injector, default)

  /**
   * This check is requires because Scala can't infer return type, so you can't write something like this:
   *     val host: String = inject
   *
   * In this example `Nothing` would be inferred. And it's not what most users expect!
   *
   * For more info, please refer to https://issues.scala-lang.org/browse/SI-2609
   */
  private def check[T](m: Manifest[T]) =
    if (m.erasure == classOf[Object])
      throw new InjectException("Unfortunately inject can't infer required binding type. " +
          "Please provide expected injection type explicitly: inject [MyType]")
    else m

  private def injectWithDefault[T](injector: Injector, default: => T)(ids: List[Identifier]) =
    injector getBinding ids flatMap (_.get) map (_.asInstanceOf[T]) getOrElse default

  private def noBindingFound(ids: List[Identifier]) =
    throw new InjectException(ids map ("  * " +) mkString ("No biding found with following identifiers:\n", "\n", ""))

  // in case is identifier goes at first

  protected implicit def canBeIdentifiedToConstraints[T : CanBeIdentifier](target: T) =
    new InjectConstraints[Nothing](initialIdentifiers = List(implicitly[CanBeIdentifier[T]].toIdentifier(target)))

  // initial words

  protected val identified = new IdentifiedWord
  protected val by = new ByWord
}

trait OpenInjectable extends Injectable {
  override val identified = new IdentifiedWord
  override val by = new ByWord

  override implicit def canBeIdentifiedToConstraints[T: CanBeIdentifier](target: T) =
    super.canBeIdentifiedToConstraints[T](target)

  override def inject[T](implicit injector: Injector, m: Manifest[T]) =
    super.inject[T](injector, m)

  override def inject[T](identifiers: Identifier*)(implicit injector: Injector, m: Manifest[T]): T =
    super.inject[T](identifiers: _*)(injector, m)

  override def inject[T](constraints: => InjectConstraints[T])(implicit injector: Injector, m: Manifest[T]) =
    super.inject[T](constraints)(injector, m)

  override def injectWithDefault[T](default: => T)(implicit injector: Injector, m: Manifest[T]) =
    super.injectWithDefault[T](default)(injector, m)

  override def injectWithDefault[T](identifiers: Identifier*)(default: => T)(implicit injector: Injector, m: Manifest[T]): T =
    super.injectWithDefault[T](identifiers: _*)(default)(injector, m)
}

object Injectable extends OpenInjectable

class IdentifiedWord[+T](default: Option[() => T] = None, initialIdentifiers: List[Identifier] = Nil) {
  def by[T: CanBeIdentifier](target: T*) = new InjectConstraints(default, initialIdentifiers ++ (target map implicitly[CanBeIdentifier[T]].toIdentifier))
}

class ByWord(initialIdentifiers: List[Identifier] = Nil) {
  def default[T](fn: => T) = new InjectConstraints[T](Some(() => fn), initialIdentifiers)
}

case class InjectConstraints[+T](default: Option[() => T] = None, initialIdentifiers: List[Identifier] = Nil) {
  var identifiers : List[Identifier] = initialIdentifiers

  def and(ids: Identifier*) = {
    identifiers = identifiers ++ ids
    this
  }

  def and(by: ByWord) = new ByWord(identifiers)
  def witch(by: ByWord) = and(by)
  def that(by: ByWord) = and(by)
  def is(by: ByWord) = and(by)

  def and(by: IdentifiedWord[_]) = new IdentifiedWord[T](default, identifiers)
}

class InjectException(message: String) extends RuntimeException(message)