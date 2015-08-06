package scaldi.injectable

import scaldi.TypeTagIdentifier._
import scaldi._
import scaldi.util.ReflectionHelper
import scaldi.util.Util._
import scaldi.util.constraints.NotNothing

import scala.language.{implicitConversions, postfixOps}
import scala.reflect.runtime.universe.TypeTag

/**
 * The only responsibility of this trait is to provide you with inject function
 * (so it just provides nice syntax for injecting dependencies).
 *
 * It’s important to understand, that it’s the only purpose of it. So it is
 * completely stateless and knows nothing about actual bindings you have
 * defined in the module.
 *
 * In order to actually find and inject dependencies, inject function always
 * takes an implicit parameter of type Injector.
 */
trait Injectable extends Wire {
  protected def injectProvider[T](implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): () => T =
    () => inject(injector, tt, nn)

  protected def injectProvider[T](constraints: => InjectConstraints[T])
                                 (implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): () => T =
    () => inject(constraints)(injector, tt, nn)

  /**
   * Inject a dependency that was previously defined in a module
   * @param injector implicit, should be defined in the scope
   * @param tt implicit, already defined in the scope
   * @param nn implicit, already defined in the scope
   * @tparam T the class of the injected dependency
   * @return instance of injected dependency
   */
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

  protected implicit def canBeIdentifiedToConstraints[T : CanBeIdentifier](target: T): InjectConstraints[Nothing] =
    new InjectConstraints[Nothing](initialIdentifiers = List(implicitly[CanBeIdentifier[T]].toIdentifier(target)))

  // initial words

  protected val identified = new IdentifiedWord
  protected val by = new ByWord
}


object Injectable extends OpenInjectable



