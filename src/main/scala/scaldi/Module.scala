package scaldi

import scaldi.util.Util._
import sys._

import java.util.Properties
import java.io.{InputStream, FileInputStream, File}
import scala.reflect.runtime.universe.{TypeTag, Type, typeOf}
import scaldi.util.ReflectionHelper._

/**
 * Standard application module
 *
 * @author Oleg Ilyenko
 */
trait Module extends ReflectionBinder
                with WordBinder
                with InjectorWithLifecycle[Module]
                with Injectable
                with MutableInjectorUser
                with ShutdownHookLifecycleManager {
  lazy val bindings = wordBindings ++ (reflectiveBindings map BindingWithLifecycle.apply)

  def getBindingInternal(identifiers: List[Identifier]) = bindings find (_ isDefinedFor identifiers)
  def getBindingsInternal(identifiers: List[Identifier]) = bindings filter (_ isDefinedFor identifiers)

  protected def init(lifecycleManager: LifecycleManager) = initNonLazyWordBindings(lifecycleManager)
}

trait StaticModule extends ReflectionBinder with ImmutableInjector with Injectable {
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

  protected def init(lifecycleManager: LifecycleManager) = initNonLazyWordBindings(lifecycleManager)
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

  private def discoverBinding(name: String, tpe: Type, ids: List[Identifier] = Nil): Option[Binding] =
    getRawValue(name) flatMap (convert(_, tpe)) map(RawBinding(_, ids))

  private def convert(value: String, tpe: Type): Option[Any] =
    try {
      if (tpe =:= typeOf[Int]) Some(value.toInt)
      else if (tpe =:= typeOf[Float]) Some(value.toFloat)
      else if (tpe =:= typeOf[Double]) Some(value.toDouble)
      else if (tpe =:= typeOf[Boolean]) Some(value.toBoolean)
      else if (tpe =:= typeOf[File]) Some(new File(value))
      else if (tpe =:= typeOf[String]) Some(value)
      else None
    } catch {
      case e: Exception => None
    }

  case class RawBinding(value: Any, identifiers: List[Identifier]) extends Binding {
    val condition = None
    override def get = Some(value)
  }
}