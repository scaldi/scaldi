package scaldi

import scaldi.TypeTagIdentifier._
import scaldi.util.ReflectionHelper
import scaldi.util.Util._
import scaldi.util.constraints.NotNothing

import scala.language.{implicitConversions, postfixOps}
import scala.reflect.runtime.universe.TypeTag

/**
 * The only responsibility of this trait is to provide you with inject function
 * (so it just provides nice syntax for injecting dependencies).
 *
 *
 * It’s important to understand, that it’s the only purpose of it. So it is
 * completely stateless and knows nothing about actual bindings you have
 * defined in the module.
 *
 *
 * In order to actually find and inject dependencies, "inject" method always
 * takes an implicit parameter of type Injector.
 */
trait Injectable extends Wire {
  /**
   * Injects a dependency that was previously defined in a module
   * Example:
   * `val database = inject[Database]  // database can now be used as an instance of Database class`
   *
   * @param injector implicit Injector, should be defined in the scope. It's used to lookup bindings
   * @param tt ensures that meta-information if type T is available at the runtime
   * @param nn ensures, that type T is not inferred as Nothing by scala compiler,
   *           since inject needs to know the actual type at the runtime
   * @tparam T class of the injected dependency
   * @return instance of injected dependency
   */
  protected def inject[T](implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): T =
    List[Identifier](typeId[T]) |>
      (ids => injectWithDefault[T](injector, noBindingFound(ids))(ids))

  /**
   * Injects a dependency defined by a constraint
   * Usually used when there are more than one injections for a defined TypeClass
   *
   * Example:
   * {{{
   * binding identifiedBy 'host to "hostName"
   * val host = inject[String]('host)  // equals to "hostName"
   * val db = inject (by default defaultDb) // notice that TypeClass is optional in inferred in this case
   * }}}
   *
   * @param constraints constraints helping to define what dependency of the TypeClass
   *                    to inject (typically an identifier, string or a default value)
   * @param injector implicit Injector, should be defined in the scope. It's used to lookup bindings
   * @param tt ensures that meta-information if type T is available at the runtime
   * @param nn ensures, that type T is not inferred as Nothing by scala compiler,
   *           since inject needs to know the actual type at the runtime
   * @tparam T class of the injected dependency
   * @return instance of injected dependency
   */
  protected def inject[T](constraints: => InjectConstraints[T])
                         (implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): T =
    List(typeId[T]) ++ constraints.identifiers |>
      (ids => injectWithDefault[T](injector, constraints.default map(_()) getOrElse noBindingFound(ids))(ids))

  /**
   * Injects a dependency that was previously defined as a provider in a module
   *
   * @param injector implicit Injector, should be defined in the scope. It's used to lookup bindings
   * @param tt ensures that meta-information if type T is available at the runtime
   * @param nn ensures, that type T is not inferred as Nothing by scala compiler,
   *           since inject needs to know the actual type at the runtime
   * @tparam T class of the injected dependency
   * @return a new instance of injected dependency
   */
  protected def injectProvider[T](implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): () => T =
    () => inject(injector, tt, nn)

  /**
   * Injects a dependency (defined by a constraint) that was previously defined as a provider in a module
   * Usually used when there are more than one provider for a defined TypeClass
   *
   * @param constraints constraints helping to define what dependency of the TypeClass
   *                    to inject (typically an identifier, string or a default value)
   * @param injector implicit Injector, should be defined in the scope. It's used to lookup bindings
   * @param tt ensures that meta-information if type T is available at the runtime
   * @param nn ensures, that type T is not inferred as Nothing by scala compiler,
   *           since inject needs to know the actual type at the runtime
   * @tparam T class of the injected dependency
   * @return a new instance of injected dependency
   */
  protected def injectProvider[T](constraints: => InjectConstraints[T])
                                 (implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): () => T =
    () => inject(constraints)(injector, tt, nn)

  /**
   * Inject all dependencies of class T that were defined in a module
   *
   * @param injector implicit Injector, should be defined in the scope. It's used to lookup bindings
   * @param tt ensures that meta-information if type T is available at the runtime
   * @param nn ensures, that type T is not inferred as Nothing by scala compiler,
   *           since inject needs to know the actual type at the runtime
   * @tparam T class of the injected dependency
   * @return a list of injections satisfying the TypeClass constraint
   */
  protected def injectAllOfType[T](implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): List[T] =
    injectAllOfType[T]()(injector, tt, nn)

  /**
   * Inject all dependencies of class T that were defined in a module and that are defined by supplied identifiers
   *
   * @param identifiers identifiers that were defined in module for requested injections
   * @param injector implicit Injector, should be defined in the scope. It's used to lookup bindings
   * @param tt ensures that meta-information if type T is available at the runtime
   * @param nn ensures, that type T is not inferred as Nothing by scala compiler,
   *           since inject needs to know the actual type at the runtime
   * @tparam T class of the injected dependency
   * @return a list of injections satisfying the TypeClass constraint and identifiers
   */
  protected def injectAllOfType[T](identifiers: Identifier*)(implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): List[T] =
    List[Identifier](typeId[T]) ++ identifiers |>
      (ids => injector getBindings ids flatMap (_.get) map (_.asInstanceOf[T]))

  /**
   * Injects all dependencies that are defined by identifiers
   *
   * @param identifiers identifiers that were defined in module for requested injections
   * @param injector implicit Injector, should be defined in the scope. It's used to lookup bindings
   * @return a list of injections defined by identifiers without any TypeClass constraint
   */
  protected def injectAll(identifiers: Identifier*)(implicit injector: Injector): List[Any] =
    injector getBindings identifiers.toList flatMap (_.get)

  /**
   * Setup an injection that returns default value if an injection with supplied identifiers is not found
   *
   * @param injector current Injector, used to lookup bindings
   * @param default default value to return in case injection is not found
   * @param identifiers identifiers that define requested injection
   * @tparam T class of the injected dependency
   * @return injection defined by identifiers or default value if injection is not found
   */
  protected def injectWithDefault[T](injector: Injector, default: => T)(identifiers: List[Identifier]) =
    injector getBinding identifiers flatMap (_.get) map (_.asInstanceOf[T]) getOrElse default

  /**
   * Injects a dependency defined by TypeClass. If dependency is not found, initializes it with constructor
   * Constructor must have a parameter paramName with a default value
   *
   * @param paramName name of the
   * @param injector implicit Injector, should be defined in the scope. It's used to lookup bindings
   * @param tt ensures that meta-information if type T is available at the runtime
   * @param ct ensures that meta-information if type C is available at the runtime
   * @tparam T class of the injected dependency
   * @tparam C class of constructor in case dependency cannot be found by T identifier
   * @return instance of injected dependency. If not found by, initializes one using constructor C
   */
  protected def injectWithConstructorDefault[T, C](paramName: String)(implicit injector: Injector, tt: TypeTag[T], ct: TypeTag[C]): T =
    injectWithDefault[T](injector, ReflectionHelper.getDefaultValueOfParam[T, C](paramName))(List(typeId[T]))

  /**
   * Helper method for throwing a readable exception when a binding could not be found
   *
   * @param identifiers a list of identifiers
   */
  protected def noBindingFound(identifiers: List[Identifier]) =
    throw new InjectException(identifiers map ("  * " +) mkString ("No binding found with following identifiers:\n", "\n", ""))

  /**
   * Implicit cast of classes that can be identifiers to an InjectConstraints with the identified as a first element
   * Often used when a list of identifiers should be passed (for example for "inject(constraint)" method)
   * This lets us to put the identifier in a first place to make the parameters look readable
   *
   * @param target Instance of a class that qualifies as an identifier
   * @tparam T class that qualifies as an identifier
   * @return InjectConstraints
   */
  protected implicit def canBeIdentifiedToConstraints[T : CanBeIdentifier](target: T): InjectConstraints[Nothing] =
    new InjectConstraints[Nothing](initialIdentifiers = List(implicitly[CanBeIdentifier[T]].toIdentifier(target)))

  /**
   * Convenience field to make the injection look more readable when used with identifiers
   * Example:
   * `inject [Database] (identified by 'local)`
   */
  protected val identified = new IdentifiedWord

  /**
   * Convenience field to make the injection look more readable when used with default values for the injection
   * Example:
   * `inject [Database] (by default defaultDb)`
   */
  protected val by = new ByWord
}

/**
 * Exactly the same as Injectable trait, but with all methods made public
 */
trait OpenInjectable extends Injectable {

  override def inject[T](implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]) =
    super.inject[T](injector, tt, nn)

  override def inject[T](constraints: => InjectConstraints[T])(implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]) =
    super.inject[T](constraints)(injector, tt, nn)

  override def injectProvider[T](implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): () => T =
    super.injectProvider(injector, tt, nn)

  override def injectProvider[T](constraints: => InjectConstraints[T])(implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): () => T =
    super.injectProvider(constraints)(injector, tt, nn)

  override def injectAllOfType[T](implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): List[T] =
    super.injectAllOfType[T](injector, tt, nn)

  override def injectAllOfType[T](identifiers: Identifier*)(implicit injector: Injector, tt: TypeTag[T], nn: NotNothing[T]): List[T] =
    super.injectAllOfType[T](identifiers: _*)(injector, tt, nn)

  override def injectAll(identifiers: Identifier*)(implicit injector: Injector): List[Any] =
    super.injectAll(identifiers: _*)(injector)

  override def injectWithDefault[T](injector: Injector, default: => T)(identifiers: List[Identifier]) =
    super.injectWithDefault[T](injector, default)(identifiers)

  override def injectWithConstructorDefault[T, C](paramName: String)(implicit injector: Injector, tt: TypeTag[T], ct: TypeTag[C]): T =
    super.injectWithConstructorDefault[T, C](paramName)(injector, tt, ct)

  override def noBindingFound(identifiers: List[Identifier]) = super.noBindingFound(identifiers)

  override implicit def canBeIdentifiedToConstraints[T: CanBeIdentifier](target: T): InjectConstraints[Nothing] =
    super.canBeIdentifiedToConstraints[T](target)

  override val identified = new IdentifiedWord

  override val by = new ByWord
}

/**
 * Injectable trait is completely stateless, so it can be declared as an Object
 */
object Injectable extends OpenInjectable

/**
 * Helper class to make injection by identifier more readable
 *
 * @param default default value for injection
 * @param initialIdentifiers identifiers
 * @tparam T class of the default parameter
 */
class IdentifiedWord[+T](default: Option[() => T] = None, initialIdentifiers: List[Identifier] = Nil) {
  def by[I: CanBeIdentifier](target: I*) = new InjectConstraints(default, initialIdentifiers ++ (target map implicitly[CanBeIdentifier[I]].toIdentifier))
}

/**
 * Helper class to make injection with default value more readable
 *
 * @param initialIdentifiers identifiers
 */
class ByWord(initialIdentifiers: List[Identifier] = Nil) {
  def default[T](fn: => T) = new InjectConstraints[T](Some(() => fn), initialIdentifiers)
}

/**
 * A wrapper for a default value for the injection and for its identifiers
 * It is here to make injection constraints look like natural language
 *
 * @param default default value for injection
 * @param initialIdentifiers identifiers
 * @tparam T class of the default parameter
 */
case class InjectConstraints[+T](default: Option[() => T] = None, initialIdentifiers: List[Identifier] = Nil) {
  var identifiers : List[Identifier] = initialIdentifiers

  /**
   * Appends identifier to a current list of identifiers
   *
   * @param ids one or many other identifiers
   */
  def and(ids: Identifier*) = {
    identifiers = identifiers ++ ids
    this
  }

  /**
   * Helper method to define a default value for injection
   * Example:
   * `inject [Database] ('database and by default defaultDb)`
   *
   * @param by ByWord that follows the "and"
   */
  def and(by: ByWord) = new ByWord(identifiers)

  /**
   * Alias to "and"
   * Example:
   * `inject [Database] ('remote which by default defaultDb)`
   *
   * @param by ByWord that follows the "which"
   */
  def which(by: ByWord) = and(by)

  /**
   * Alias to "and"
   * Example:
   * `inject [Database] ('remote that by default defaultDb)`
   *
   * @param by ByWord that follows the "that"
   */
  def that(by: ByWord) = and(by)

  /**
   * Alias to "and"
   * Example:
   * `inject [Database] ('remote is by default defaultDb)`
   *
   * @param by ByWord that follows the "is"
   */
  def is(by: ByWord) = and(by)

  /**
   * Helper method to make possible that "identified" follows "and" word
   * Example:
   * `inject [Database] (by default defaultDb and identified by 'remote)`
   *
   * @param by IdentifiedWord that follows the "and"
   */
  def and(by: IdentifiedWord[_]) = new IdentifiedWord[T](default, identifiers)
}
