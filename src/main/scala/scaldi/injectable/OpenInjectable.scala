package scaldi.injectable

import scaldi.util.constraints.NotNothing
import scaldi.{Identifier, Injector, CanBeIdentifier}
import scala.reflect.runtime.universe.TypeTag

/**
 * Just like Injectable trait, but with all methods made public
 */
trait OpenInjectable extends Injectable {
  override val identified = new IdentifiedWord
  override val by = new ByWord

  override implicit def canBeIdentifiedToConstraints[T: CanBeIdentifier](target: T): InjectConstraints[Nothing] =
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

