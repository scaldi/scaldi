package scaldi

import java.io.{File, FileInputStream, InputStream}
import java.util.Properties

import com.typesafe.config._
import scaldi.util.Util._

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.reflect.runtime.universe.{Type, typeOf}
import scala.sys._
import scala.util.control.NonFatal

/**
 * Module is a place where you declare your bindings.
 * It's also the most common injector that you can use in most cases.
 * It is a mutable injector so it can have a lifecycle and it also provides nice DSL for the bindings.
 *
 *
 * Module is an Injectable instance thanks to that you can inject other dependencies in your bindings.
 */
trait Module extends WordBinder
                with InjectorWithLifecycle[Module]
                with Injectable
                with MutableInjectorUser
                with ShutdownHookLifecycleManager {

  /**
   * @inheritdoc
   */
  def getBindingInternal(identifiers: List[Identifier]) = wordBindings find (_ isDefinedFor identifiers)

  /**
   * @inheritdoc
   */
  def getBindingsInternal(identifiers: List[Identifier]) = wordBindings filter (_ isDefinedFor identifiers)

  /**
   * @inheritdoc
   */
  protected def init(lifecycleManager: LifecycleManager) = initEagerWordBindings(lifecycleManager)
}

@deprecated("StaticModule is deprecated and will be removed soon. As an alternative you can " +
  "use `ImmutableWrapper` injector to define an immutability boundary in composition or create " +
  "your own injector that is marked as `ImmutableInjector`.", "0.5")
trait StaticModule extends ReflectionBinder
                      with ImmutableInjector
                      with Injectable {
  /**
   * @inheritdoc
   */
  def getBinding(identifiers: List[Identifier]) = reflectiveBindings find (_ isDefinedFor identifiers)

  /**
   * @inheritdoc
   */
  def getBindings(identifiers: List[Identifier]) = reflectiveBindings filter (_ isDefinedFor identifiers)

  implicit val injector: Injector = this
}

/**
 * ??? The inside of this class is the same as in Module ???
 * Same as module, but with accessible `Injectable` methods.
 */
class DynamicModule extends WordBinder
                       with InjectorWithLifecycle[DynamicModule]
                       with OpenInjectable
                       with MutableInjectorUser
                       with ShutdownHookLifecycleManager {

  /**
   * @inheritdoc
   */
  def getBindingInternal(identifiers: List[Identifier]) = wordBindings find (_ isDefinedFor identifiers)

  /**
   * @inheritdoc
   */
  def getBindingsInternal(identifiers: List[Identifier]) = wordBindings filter (_ isDefinedFor identifiers)

  /**
   * @inheritdoc
   */
  protected def init(lifecycleManager: LifecycleManager) = initEagerWordBindings(lifecycleManager)
}

/**
 * `DynamicModule`'s companion object.
 */
object DynamicModule {
  /**
   * Standard factory method that accepts function that may initialize DinamicModule apon creation.
   * @param initBindingsFn function initializing DynamicModule apon creation
   * @return initialized DynamicModule
   */
  def apply(initBindingsFn: DynamicModule => Unit): Injector = new DynamicModule <| initBindingsFn
}

/**
 * Factory to initialize binding defined by `'args` in a readable way in a new `DynamicModule`.
 */
object Args {
  /**
   * Factory method used to initialize new `DynamicModule` with `'args` binded to supplied array.
   * @param args array that will be binded to `'args` identifier
   * @return new `DinamicModule` with a binding defined by `'args`
   */
  def apply(args: Array[String]): Injector = DynamicModule(m => m.bind [Array[String]] identifiedBy 'args toNonLazy args)
}

/**
 * Empty injector, used for injector combination or as a filler where injector is required,
 * but there is no injection.
 */
object NilInjector extends ImmutableInjector {
  /**
   * @inheritdoc
   */
  def getBinding(identifiers: List[Identifier]) = None

  /**
   * @inheritdoc
   */
  def getBindings(identifiers: List[Identifier]) = Nil
}

/**
 * Used to look for simple bindings in system properties.
 */
object SystemPropertiesInjector extends RawInjector {
  /**
   * @inheritdoc
   * @param name system property's name
   * @return system property defined by `name`
   */
  def getRawValue(name: String) = props get name
}

/**
 * Used to look for simple bindings in supplied properties (hash table)
 * @param properties hash table with properties
 */
class PropertiesInjector private (properties: Properties) extends RawInjector {
  /**
   * @inheritdoc
   * @param name property's name
   * @return property defined by `name` as instance of String
   */
  def getRawValue(name: String) = Option(properties get name).map(_.asInstanceOf[String])
}

/**
 * Companion object with factories to handle different type of properties' source.
 */
object PropertiesInjector {
  /**
   * Factory method to retrieve properties from file.
   * @param fileName name of the file with properties
   * @return an instance of `PropertiesInjector` with properties from file with supplied file name
   */
  def apply(fileName: String): PropertiesInjector = apply(new File(fileName))

  /**
   * Factory method to retrieve properties from file.
   * @param file file with properties
   * @return an instance of `PropertiesInjector` with properties from supplied file
   */
  def apply(file: File): PropertiesInjector = apply(new FileInputStream(file))

  /**
   * Factory method to retrieve properties from input stream.
   * @param stream input stream which will supply properties
   * @return an instance of `PropertiesInjector` with properties from input stream
   */
  def apply(stream: InputStream): PropertiesInjector = apply(new Properties <| (_ load stream))

  /**
   * Factory method to retrieve properties from a Properties instance.
   * @param properties `Properties` instance with properties
   * @return an instance of `PropertiesInjector` with properties supplied `Properties` instance
   */
  def apply(properties: Properties): PropertiesInjector = new PropertiesInjector(properties)
}

class TypesafeConfigInjector private (config: Config) extends RawInjector {
  /**
   * @inheritdoc
   */
  override protected def discoverBinding(name: String, tpe: Type, ids: List[Identifier]) = {
    val value = try {
      if (tpe =:= typeOf[Int]) Some(config.getInt(name))
      else if (tpe =:= typeOf[List[Int]]) Some(config.getIntList(name).asScala.toList map (_.intValue))
      else if (tpe =:= typeOf[Integer]) Some(config.getInt(name): java.lang.Integer)
      else if (tpe =:= typeOf[List[Integer]]) Some(config.getIntList(name).asScala.toList)
      else if (tpe =:= typeOf[Long]) Some(config.getLong(name))
      else if (tpe =:= typeOf[List[Long]]) Some(config.getLongList(name).asScala.toList map (_.longValue))
      else if (tpe =:= typeOf[java.lang.Long]) Some(config.getLong(name): java.lang.Long)
      else if (tpe =:= typeOf[List[java.lang.Long]]) Some(config.getLongList(name).asScala.toList)
      else if (tpe =:= typeOf[Double]) Some(config.getDouble(name))
      else if (tpe =:= typeOf[List[Double]]) Some(config.getDoubleList(name).asScala.toList map (_.doubleValue))
      else if (tpe =:= typeOf[java.lang.Double]) Some(config.getDouble(name): java.lang.Double)
      else if (tpe =:= typeOf[List[java.lang.Double]]) Some(config.getDoubleList(name).asScala.toList)
      else if (tpe =:= typeOf[Boolean]) Some(config.getBoolean(name))
      else if (tpe =:= typeOf[List[Boolean]]) Some(config.getBooleanList(name).asScala.toList map (_.booleanValue))
      else if (tpe =:= typeOf[java.lang.Boolean]) Some(config.getBoolean(name): java.lang.Boolean)
      else if (tpe =:= typeOf[List[java.lang.Boolean]]) Some(config.getBooleanList(name).asScala.toList)
      else if (tpe =:= typeOf[File]) Some(new File(config.getString(name)))
      else if (tpe =:= typeOf[List[File]]) Some(config.getStringList(name).asScala.toList map (new File(_)))
      else if (tpe =:= typeOf[Duration]) Some(Duration(config.getString(name)))
      else if (tpe =:= typeOf[List[Duration]]) Some(config.getStringList(name).asScala.toList map (Duration(_)))
      else if (tpe =:= typeOf[String]) Some(config.getString(name))
      else if (tpe =:= typeOf[List[String]]) Some(config.getStringList(name).asScala.toList)
      else if (tpe =:= typeOf[Config]) Some(config.getConfig(name))
      else if (tpe =:= typeOf[List[Config]]) Some(config.getConfigList(name).asScala.toList)
      else if (tpe =:= typeOf[ConfigValue]) Some(config.getValue(name))
      else if (tpe =:= typeOf[ConfigList]) Some(config.getList(name))
      else if (tpe =:= typeOf[ConfigObject]) Some(config.getObject(name))
      else if (tpe =:= typeOf[List[ConfigObject]]) Some(config.getObjectList(name).asScala.toList)
      else None
    } catch {
      case NonFatal(e) => None
    }

    value map (RawBinding(_, ids))
  }

  /**
   * @inheritdoc
   */
  def getRawValue(name: String) = throw new IllegalStateException("Should not be used")
}

/**
 * Companion object with factory methods to specify the source of TypesafeConfig.
 */
object TypesafeConfigInjector {
  /**
   * Initializes injector with application's configuration.
   * @return initialized injector
   */
  def apply(): TypesafeConfigInjector = apply(ConfigFactory.load())

  /**
   * Initializes injector with the configuration found at `basename`.
   * @param baseName the base name of configuration file
   * @return initialized injector
   */
  def apply(baseName: String): TypesafeConfigInjector = apply(ConfigFactory.load(baseName))

  /**
   * Initializes injector with the supplied config.
   * @param config configuration instance
   * @return initialized injector
   */
  def apply(config: Config): TypesafeConfigInjector = new TypesafeConfigInjector(config)
}

trait RawInjector extends Injector {
  private var bindingCache: List[Binding] = Nil

  /**
   * Used to retrieve value from other places than modules (config, system, etc.).
   * @param name name of the value
   * @return raw value defined by name
   */
  def getRawValue(name: String): Option[String]

  /**
   * @inheritdoc
   */
  def getBinding(identifiers: List[Identifier]) = discoverBinding(identifiers)

  /**
   * @inheritdoc
   */
  def getBindings(identifiers: List[Identifier]) = discoverBinding(identifiers).toList

  /**
   * Retrieves bindings based on supplied identifiers.
   * @param ids identifiers describing binding
   * @return found binding, `None` if binding was not found
   */
  protected def discoverBinding(ids: List[Identifier]): Option[Binding] =
    bindingCache find (_ isDefinedFor ids) orElse {
      (ids match {
        case TypeTagIdentifier(c) :: StringIdentifier(name) :: Nil => discoverBinding(name, c, ids)
        case StringIdentifier(name) :: TypeTagIdentifier(c) :: Nil => discoverBinding(name, c, ids)
        case _ => None
      }) match {
        case res @ Some(binding) =>
          bindingCache = bindingCache :+ binding
          res
        case None => None
      }
    }

  /**
   * Retrieves value by name, converts it to specified type and associates it with supplied identifiers.
   * @param name name of the value to bind
   * @param tpe type of the value to bind
   * @param ids identifiers of the resulting bindng
   * @return option with resulting binding (`None` if binding was not found by name)
   */
  protected def discoverBinding(name: String, tpe: Type, ids: List[Identifier] = Nil): Option[Binding] =
    getRawValue(name) flatMap (convert(_, tpe)) map (RawBinding(_, ids))

  /**
   * Converts value to a specified type.
   * @param value supplied value
   * @param tpe type to convert the value
   * @return option with converted value (`None` if value could not be converted)
   */
  private def convert(value: String, tpe: Type): Option[Any] =
    try {
      if (tpe =:= typeOf[Int]) Some(value.toInt)
      else if (tpe =:= typeOf[Integer]) Some(value.toInt: java.lang.Integer)
      else if (tpe =:= typeOf[Long]) Some(value.toLong)
      else if (tpe =:= typeOf[java.lang.Long]) Some(value.toLong: java.lang.Long)
      else if (tpe =:= typeOf[Double]) Some(value.toDouble)
      else if (tpe =:= typeOf[java.lang.Double]) Some(value.toDouble: java.lang.Double)
      else if (tpe =:= typeOf[Boolean]) Some(value.toBoolean)
      else if (tpe =:= typeOf[java.lang.Boolean]) Some(value.toBoolean: java.lang.Boolean)
      else if (tpe =:= typeOf[File]) Some(new File(value))
      else if (tpe =:= typeOf[Duration]) Some(Duration(value))
      else if (tpe =:= typeOf[String]) Some(value)
      else None
    } catch {
      case NonFatal(e) => None
    }
}

/**
 * Binding holding a raw value (value from system properties, configuration, etc.).
 * @param value value of the binding
 * @param identifiers identifiers defining the binding
 */
case class RawBinding(value: Any, identifiers: List[Identifier]) extends Binding {
  /**
   * @inheritdoc
   */
  val condition = None

  /**
   * @inheritdoc
   */
  override def get = Some(value)

  /**
   * @inheritdoc
   */
  override def isCacheable = true
}

/**
 * ??? I could understand what this is used for ???
 * @param bindings function transforming injector into a list of bindings
 */
class SimpleContainerInjector(bindings: Injector => List[BindingWithLifecycle]) extends MutableInjectorUser with InjectorWithLifecycle[SimpleContainerInjector] with ShutdownHookLifecycleManager {
  /**
   * A list of prepared bindings
   */
  lazy val preparedBindings = bindings(injector)

  /**
   * @inheritdoc
   */
  def getBindingInternal(identifiers: List[Identifier]) = preparedBindings find (_ isDefinedFor identifiers)

  /**
   * @inheritdoc
   */
  def getBindingsInternal(identifiers: List[Identifier]) = preparedBindings filter (_ isDefinedFor identifiers)

  /**
   * @inheritdoc
   */
  protected def init(lifecycleManager: LifecycleManager) =
    preparedBindings |> (b => () => b.filter(_.isEager).foreach(_ get lifecycleManager))
}