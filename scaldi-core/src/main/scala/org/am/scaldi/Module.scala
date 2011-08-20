package org.am.scaldi

import util.CreationHelper
import org.am.scaldi.util.Util._
import sys._

import java.util.Properties
import java.io.{InputStream, FileInputStream, File}

/**
 * Standard application module
 *
 * @author Oleg Ilyenko
 */
trait Module extends ReflectionBinder with WordBinder with InitializeableInjector[Module] with Injectable with MutableInjectorUser with CreationHelper {
  lazy val bindings = wordBindings ++ reflectiveBindings

  def getBindingInternal(identifiers: List[Identifier]) = bindings find (_ isDefinedFor identifiers)
  def getBindingsInternal(identifiers: List[Identifier]) = bindings filter (_ isDefinedFor identifiers)

  protected def init() = initiNonLazyWordBindings()
}

trait StaticModule extends ReflectionBinder with Injector with Injectable with CreationHelper {
  def getBinding(identifiers: List[Identifier]) = reflectiveBindings find (_ isDefinedFor identifiers)
  def getBindings(identifiers: List[Identifier]) = reflectiveBindings filter (_ isDefinedFor identifiers)

  implicit val injector: Injector = this
}

class DynamicModule extends WordBinder with InitializeableInjector[DynamicModule] with OpenInjectable with MutableInjectorUser with CreationHelper {
  def getBindingInternal(identifiers: List[Identifier]) = wordBindings find (_ isDefinedFor identifiers)
  def getBindingsInternal(identifiers: List[Identifier]) = wordBindings filter (_ isDefinedFor identifiers)

  protected def init() = initiNonLazyWordBindings()
}

object DynamicModule {
  def apply(initBindingsFn: DynamicModule => Unit): Injector = new DynamicModule ~ initBindingsFn
}

object Args {
  def apply(args: Array[String]): Injector = DynamicModule(m => m.bind [Array[String]] identifiedBy 'args toNonLazy args)
}

object NilInjector extends Injector {
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
  def apply(stream: InputStream): PropertiesInjector = apply(new Properties ~ (_ load stream))
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
        case ClassIdentifier(c) :: StringIdentifier(name)  :: Nil => discoverBinding(name, c, ids)
        case StringIdentifier(name) :: ClassIdentifier(c) :: Nil => discoverBinding(name, c, ids)
        case _ => None
      }) match {
        case res @ Some(binding) =>
          bindingCache = bindingCache :+ binding
          res
        case None => None
      }
    }

  private def discoverBinding(name: String, clazz: Class[_], ids: List[Identifier] = Nil): Option[Binding] =
    getRawValue(name) flatMap (convert(_, clazz)) map(RawBinding(_, ids))

  private def convert(value: String, clazz: Class[_]): Option[Any] =
    try {
      if (clazz == classOf[Int]) Some(value.toInt)
      else if (clazz == classOf[Float]) Some(value.toFloat)
      else if (clazz == classOf[Double]) Some(value.toDouble)
      else if (clazz == classOf[Boolean]) Some(value.toBoolean)
      else if (clazz == classOf[File]) Some(new File(value))
      else if (clazz == classOf[String]) Some(value)
      else None
    } catch {
      case e: Exception => None
    }

  case class RawBinding(value: Any, identifiers: List[Identifier]) extends Binding {
    protected val condition = None
    def get = Some(value)
  }
}