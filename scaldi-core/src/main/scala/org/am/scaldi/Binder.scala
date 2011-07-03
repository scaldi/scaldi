package org.am.scaldi

import org.am.scaldi.util.ReflectionHelper._
import org.am.scaldi.util.Util._
import java.lang.reflect.InvocationTargetException

trait WordBinder {
  private var bindingsInProgress: List[BindHelper] = Nil
  private var bindings: List[BoundHelper] = Nil

  def binding = {
    val helper = new BindHelper({ (bind, bound) =>
      bindingsInProgress = bindingsInProgress - bind
      bindings = bindings :+ bound
    })
    bindingsInProgress = bindingsInProgress :+ helper
    helper
  }

  def bind[T : Manifest] = binding identifiedBy manifest[T].erasure

  lazy val wordBindings: List[Binding] = {
    if (bindingsInProgress nonEmpty) {
      throw new BindingException(
          bindingsInProgress
              .map(b => "\tBinding with identifiers: " + (b.identifiers mkString ", "))
              .mkString("Following bindings are not bound to anything (please use 'to', 'toProvider' or 'toNonLazy']):\n","\n", "")
      )
    }

    bindings map (_ getBinding) reverse
  }
}

trait CanBeIdentified[R] { this: R =>
  var identifiers : List[Identifier] = Nil

  def identifiedBy(ids: Identifier*): R = {
    identifiers = identifiers ++ ids
    this
  }

  def and(ids: Identifier*): R = identifiedBy(ids: _*)
}

class BindHelper(onBound: (BindHelper, BoundHelper) => Unit) extends CanBeIdentified[BindHelper] {
  var createFn: Option[Option[() => Any]] = None

  def to(none: None.type) = bindNone(LazyBinding(None, _))
  def to[T : Manifest](fn: => T) = bind(LazyBinding(Some(() => fn), _))
  def in[T : Manifest](fn: => T) = to(fn)

  def toNonLazy(none: None.type) = bindNone(NonLazyBinding(None, _))
  def toNonLazy[T : Manifest](fn: => T) = bind(NonLazyBinding(Some(() => fn), _))
  def inNonLazy[T : Manifest](fn: => T) = to(fn)

  def toProvider[T : Manifest](fn: => T) = bind(ProviderBinding(() => fn, _))
  def inProvider[T : Manifest](fn: => T) = to(fn)

  private def bind[T : Manifest](bindingFn: List[Identifier] => Binding) = {
    val bound = new BoundHelper(bindingFn, identifiers, Some(manifest[T].erasure))
    onBound(this, bound)
    bound
  }

  private def bindNone(bindingFn: List[Identifier] => Binding) = {
    val bound = new BoundHelper(bindingFn, identifiers, None)
    onBound(this, bound)
    bound
  }
}

class BoundHelper(
       bindingFn: List[Identifier] => Binding,
       initialIdentifiers: List[Identifier],
       bindingType: Option[Class[_]])
    extends CanBeIdentified[BoundHelper] {
  def getBinding = bindingFn((initialIdentifiers ++ identifiers, bindingType) match {
    case (ids, _) if ids.exists(_.isInstanceOf[ClassIdentifier]) => ids
    case (ids, Some(t)) => ids :+ ClassIdentifier(t)
    case (ids, None) => ids
  })
}

trait ReflectionBinder {
  private var reflectiveBindingCache: List[Binding] = Nil

  protected def discoverBinding(ids: List[Identifier]): Option[Binding] =
    reflectiveBindingCache find (_ hasIdentifiers ids) orElse {
      (ids match {
        case ClassIdentifier(c) :: StringIdentifier(name)  :: Nil => discoverBinding(c, ids, Some(name))
        case StringIdentifier(name) :: ClassIdentifier(c) :: Nil => discoverBinding(c, ids, Some(name))
        case ClassIdentifier(c) :: Nil => discoverBinding(c, ids)
        case _ => None
      }) match {
        case res @ Some(binding) =>
          reflectiveBindingCache = reflectiveBindingCache :+ binding
          res
        case None => None
      }
    }

  private def discoverBinding(clazz: Class[_], ids: List[Identifier] = Nil, name: Option[String] = None): Option[Binding] =
    wrapReflection {
      this.getClass
          .getMatchingMethod (name, clazz)
          .map (m => ReflectiveBinding(() => wrapReflection(m.invoke(this)), ids))
    }


  private def wrapReflection[T](fn: => T): T = {
    try {
      fn
    } catch {
      case e: InvocationTargetException =>
        throw new BindingException("Exceprion during reflectivg binding discovery", e.getCause)
      case e: Exception =>
        throw new BindingException("Exceprion during reflectivg binding discovery", e)
    }
  }

  case class ReflectiveBinding(fn: () => Any, identifiers: List[Identifier]) extends Binding {
    def get = Some(fn())
  }
}

class BindingException(message: String, cause: Throwable) extends RuntimeException(message,cause) {
  def this(message: String) = this(message, null)
}