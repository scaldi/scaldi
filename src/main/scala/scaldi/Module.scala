package scaldi

import scaldi.util.Util._
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal
import sys._

import java.util.Properties
import java.io.{InputStream, FileInputStream, File}
import scala.reflect.runtime.universe.{TypeTag, Type, typeOf}
import scaldi.util.ReflectionHelper._
import com.typesafe.config._
import scala.collection.JavaConverters._

/**
 * Standard application module
 *
 * @author Oleg Ilyenko
 */
trait Module extends WordBinder
                with InjectorWithLifecycle[Module]
                with Injectable
                with MutableInjectorUser
                with ShutdownHookLifecycleManager {
  lazy val bindings = wordBindings

  def getBindingInternal(identifiers: List[Identifier]) = bindings find (_ isDefinedFor identifiers)
  def getBindingsInternal(identifiers: List[Identifier]) = bindings filter (_ isDefinedFor identifiers)

  protected def init(lifecycleManager: LifecycleManager) = initEagerWordBindings(lifecycleManager)
}

@deprecated("StaticModule is deprecated and will be removed soon. As an alternative you can use `ImmutableWrapper` injector to define an immutability boundary in composition or create your own injector that is marked as `ImmutableInjector`.", "0.5")
trait StaticModule extends ReflectionBinder
                      with ImmutableInjector
                      with Injectable {
  def getBinding(identifiers: List[Identifier]) = reflectiveBindings find (_ isDefinedFor identifiers)
  def getBindings(identifiers: List[Identifier]) = reflectiveBindings filter (_ isDefinedFor identifiers)

  implicit val injector: Injector = this
}

class DynamicModule extends WordBinder
                       with InjectorWithLifecycle[DynamicModule]
                       with OpenInjectable
                       with MutableInjectorUser
                       with ShutdownHookLifecycleManager {
  def getBindingInternal(identifiers: List[Identifier]) = wordBindings find (_ isDefinedFor identifiers)
  def getBindingsInternal(identifiers: List[Identifier]) = wordBindings filter (_ isDefinedFor identifiers)

  protected def init(lifecycleManager: LifecycleManager) = initEagerWordBindings(lifecycleManager)
}

object DynamicModule {
  def apply(initBindingsFn: DynamicModule => Unit): Injector = new DynamicModule <| initBindingsFn
}

object Args {
  def apply(args: Array[String]): Injector = DynamicModule(m => m.bind [Array[String]] identifiedBy 'args toNonLazy args)
}

object NilInjector extends ImmutableInjector {
  def getBinding(identifiers: List[Identifier]) = None
  def getBindings(identifiers: List[Identifier]) = Nil
}

object SystemPropertiesInjector extends RawInjector {
  def getRawValue(name: String) = props get name
}

class PropertiesInjector private (properties: Properties) extends RawInjector {
  def getRawValue(name: String) = Option(properties get name).map(_.asInstanceOf[String])
}

object PropertiesInjector {
  def apply(fileName: String): PropertiesInjector = apply(new File(fileName))
  def apply(file: File): PropertiesInjector = apply(new FileInputStream(file))
  def apply(stream: InputStream): PropertiesInjector = apply(new Properties <| (_ load stream))
  def apply(properties: Properties): PropertiesInjector = new PropertiesInjector(properties)
}

class TypesafeConfigInjector private (config: Config) extends RawInjector {
  override protected def discoverBinding(name: String, tpe: Type, ids: List[Identifier]) = {
    val value = try {
      if (tpe =:= typeOf[Int]) Some(config.getInt(name))
      if (tpe =:= typeOf[List[Int]]) Some(config.getIntList(name).asScala.toList map (_.intValue))
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

  def getRawValue(name: String) = throw new IllegalStateException("Should not be used")
}

object TypesafeConfigInjector {
  def apply(): TypesafeConfigInjector = apply(ConfigFactory.load())
  def apply(baseName: String): TypesafeConfigInjector = apply(ConfigFactory.load(baseName))
  def apply(config: Config): TypesafeConfigInjector = new TypesafeConfigInjector(config)
}

trait RawInjector extends Injector {
  private var bindingCache: List[Binding] = Nil

  def getRawValue(name: String): Option[String]

  def getBinding(identifiers: List[Identifier]) = discoverBinding(identifiers)
  def getBindings(identifiers: List[Identifier]) = discoverBinding(identifiers).toList

  protected def discoverBinding(ids: List[Identifier]): Option[Binding] =
    bindingCache find (_ isDefinedFor ids) orElse {
      (ids match {
        case TypeTagIdentifier(c) :: StringIdentifier(name)  :: Nil => discoverBinding(name, c, ids)
        case StringIdentifier(name) :: TypeTagIdentifier(c) :: Nil => discoverBinding(name, c, ids)
        case _ => None
      }) match {
        case res @ Some(binding) =>
          bindingCache = bindingCache :+ binding
          res
        case None => None
      }
    }

  protected def discoverBinding(name: String, tpe: Type, ids: List[Identifier] = Nil): Option[Binding] =
    getRawValue(name) flatMap (convert(_, tpe)) map (RawBinding(_, ids))

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

case class RawBinding(value: Any, identifiers: List[Identifier]) extends Binding {
  val condition = None
  override def get = Some(value)
}

class SimpleContainerInjector(bindings: Injector => List[BindingWithLifecycle]) extends MutableInjectorUser with InjectorWithLifecycle[SimpleContainerInjector] with ShutdownHookLifecycleManager {
  lazy val preparedBindings = bindings(injector)

  def getBindingInternal(identifiers: List[Identifier]) = preparedBindings find (_ isDefinedFor identifiers)
  def getBindingsInternal(identifiers: List[Identifier]) = preparedBindings filter (_ isDefinedFor identifiers)

  protected def init(lifecycleManager: LifecycleManager) = {
    preparedBindings.foreach { binding =>
      if (binding.isEager) binding.get(lifecycleManager)
    }

    () => ()
  }
}