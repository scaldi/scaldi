package scaldi

import language.{postfixOps, implicitConversions}

import scaldi.util.constraints._
import scaldi.util.Util._
import scala.reflect.runtime.universe.TypeTag
import TypeTagIdentifier._
import scaldi.util.constraints.NotNothing
import scaldi.util.ReflectionHelper

trait Injectable extends Wire {
  protected def injectProvider[T](implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): () => T =
    () => inject(injector, tt, nn)

  protected def injectProvider[T](constraints: => InjectConstraints[T])
                                 (implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): () => T =
    () => inject(constraints)(injector, tt, nn)

  protected def inject[T](implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): T =
    List[Identifier](typeId[T]) |>
        (ids => injectWithDefault[T](injector, noBindingFound(ids))(ids))

  protected def inject[T](constraints: => InjectConstraints[T])
                         (implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): T =
    List(typeId[T]) ++ constraints.identifiers |>
      (ids => injectWithDefault[T](injector, constraints.default map(_()) getOrElse noBindingFound(ids))(ids))

  protected def injectAllOfType[T](implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): List[T] =
    injectAllOfType[T]()(injector, tt, nn)

  protected def injectAllOfType[T](identifiers: Identifier*)(implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): List[T] =
    List[Identifier](typeId[T]) ++ identifiers |>
        (ids => injector getBindings ids flatMap (_.get) map (_.asInstanceOf[T]))

  protected def injectAll(identifiers: Identifier*)(implicit injector: Injector): List[Any] =
    injector getBindings identifiers.toList flatMap (_.get)

  protected def injectWithConstructorDefault[T, C](paramName: String)(implicit injector: Injector, tt: TypeTag[T], ct: TypeTag[C]): T =
    injectWithDefault[T](injector, ReflectionHelper.getDefaultValueOfParam[T, C](paramName))(List(typeId[T]))

  protected def injectWithDefault[T](injector: Injector, default: => T)(ids: List[Identifier]) =
    injector getBinding ids flatMap (_.get) map (_.asInstanceOf[T]) getOrElse default

  protected def noBindingFound(ids: List[Identifier]) =
    throw new InjectException(ids map ("  * " +) mkString ("No binding found with following identifiers:\n", "\n", ""))

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

  override def injectProvider[T](implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): () => T =
    super.injectProvider(injector, tt, nn)

  override def injectProvider[T](constraints: => InjectConstraints[T])(implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): () => T =
    super.injectProvider(constraints)(injector, tt, nn)

  override def inject[T](implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]) =
    super.inject[T](injector, tt, nn)

  override def inject[T](constraints: => InjectConstraints[T])(implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]) =
    super.inject[T](constraints)(injector, tt, nn)

  override def injectAllOfType[T](implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): List[T] =
    super.injectAllOfType[T](injector, tt, nn)

  override def injectAllOfType[T](identifiers: Identifier*)(implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): List[T] =
    super.injectAllOfType[T](identifiers: _*)(injector, tt, nn)

  override def injectAll(identifiers: Identifier*)(implicit injector: Injector): List[Any] =
    super.injectAll(identifiers: _*)(injector)

  override def injectWithConstructorDefault[T, C](paramName: String)(implicit injector: Injector, tt: TypeTag[T], ct: TypeTag[C]): T =
    super.injectWithConstructorDefault[T, C](paramName)(injector, tt, ct)

  override def injectWithDefault[T](injector: Injector, default: => T)(ids: List[Identifier]) =
    super.injectWithDefault[T](injector, default)(ids)

  override def noBindingFound(ids: List[Identifier]) = super.noBindingFound(ids)
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
  def which(by: ByWord) = and(by)
  def that(by: ByWord) = and(by)
  def is(by: ByWord) = and(by)

  def and(by: IdentifiedWord[_]) = new IdentifiedWord[T](default, identifiers)
}
