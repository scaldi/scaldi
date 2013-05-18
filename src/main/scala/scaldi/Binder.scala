package scaldi

import scaldi.util.Util._
import java.lang.reflect.{Method, InvocationTargetException}

trait WordBinder {
  private var bindingsInProgress: List[BindHelper[_]] = Nil
  private var bindings: List[BoundHelper] = Nil

  lazy val wordBindings: List[Binding] = {
    if (bindingsInProgress nonEmpty) {
      throw new BindingException(
          bindingsInProgress
              .map(b => "\tBinding with identifiers: " + (b.identifiers mkString ", "))
              .mkString("Following bindings are not bound to anything (please use 'to', 'toProvider' or 'toNonLazy']):\n","\n", "")
      )
    }

    bindings.map(_ getBinding).reverse
  }

  def binding = createBinding[Any](None)
  def bind[T : Manifest] = createBinding[T](Some(manifest[T].erasure))

  private def createBinding[T](mainType: Option[Class[_]]) = {
    val helper = new BindHelper[T]({ (bind, bound) =>
      bindingsInProgress = bindingsInProgress filterNot (bind ==)
      bindings = bindings :+ bound
    })
    bindingsInProgress = bindingsInProgress :+ helper
    mainType foreach (helper identifiedBy _)
    helper
  }

  protected def initNonLazyWordBindings(): () => Unit = wordBindings |>
      (b => () => b.collect{case binding @ NonLazyBinding(_, _, _) => binding}.foreach(_.get))
}

trait CanBeIdentified[R] { this: R =>
  var identifiers : List[Identifier] = Nil

  def identifiedBy(ids: Identifier*): R = {
    identifiers = identifiers ++ ids
    this
  }

  def as(ids: Identifier*): R = identifiedBy(ids: _*)

  def and(ids: Identifier*): R = identifiedBy(ids: _*)
}

trait CanBeConditional[R] { this: R =>
  var condition: Option[Condition] = None

  def when(cond: Condition) = {
    condition = Some(cond)
    this
  }
}

class BindHelper[R](onBound: (BindHelper[R], BoundHelper) => Unit)
    extends CanBeIdentified[BindHelper[R]] with CanBeConditional[BindHelper[R]] {
  var createFn: Option[Option[() => Any]] = None

  def to(none: None.type) = bindNone(LazyBinding(None, _, _))
  def to[T <: R : Manifest](fn: => T) = bind(LazyBinding(Some(() => fn), _, _))
  def in[T <: R : Manifest](fn: => T) = to(fn)

  def toNonLazy[T <: R : Manifest](fn: => T) = bind(NonLazyBinding(Some(() => fn), _, _))
  def inNonLazy[T <: R : Manifest](fn: => T) = toNonLazy(fn)

  def toProvider[T <: R : Manifest](fn: => T) = bind(ProviderBinding(() => fn, _, _))
  def inProvider[T <: R : Manifest](fn: => T) = toProvider(fn)

  private def bind[T : Manifest](bindingFn: (List[Identifier], Option[Condition]) => Binding) = {
    val bound = new BoundHelper(bindingFn, identifiers, condition, Some(manifest[T].erasure))
    onBound(this, bound)
    bound
  }

  private def bindNone(bindingFn: (List[Identifier], Option[Condition]) => Binding) = {
    val bound = new BoundHelper(bindingFn, identifiers, condition, None)
    onBound(this, bound)
    bound
  }
}

class BoundHelper(
   bindingFn: (List[Identifier], Option[Condition]) => Binding,
   initialIdentifiers: List[Identifier],
   initialCondition: Option[Condition],
   bindingType: Option[Class[_]]
) extends CanBeIdentified[BoundHelper] with CanBeConditional[BoundHelper] {
  def getBinding = bindingFn (
    (initialIdentifiers ++ identifiers, bindingType) match {
      case (ids, _) if ids.exists(_.isInstanceOf[ClassIdentifier]) => ids
      case (ids, Some(t)) => ids :+ ClassIdentifier(t)
      case (ids, None) => ids
    },
    condition orElse initialCondition
  )
}

trait ReflectionBinder {
  lazy val reflectiveBindings: List[Binding] = wrapReflection {
    this.getClass.getMethods.toList.filter(_.getParameterTypes.length == 0).map { m =>
      if (classOf[BidingProvider].isAssignableFrom(m.getReturnType))
        m.invoke(this).asInstanceOf[BidingProvider].getBinding(m)
      else
        ReflectiveBinding(() => wrapReflection(Some(m.invoke(this))), List(m.getReturnType, m.getName))
    }
  }

  private def wrapReflection[T](fn: => T): T =
    try {
      fn
    } catch {
      case e: InvocationTargetException =>
        throw new BindingException("Exceprion during reflectivg binding discovery", e.getCause)
      case e: Exception =>
        throw new BindingException("Exceprion during reflectivg binding discovery", e)
    }

  case class ReflectiveBinding(fn: () => Option[Any], identifiers: List[Identifier]) extends Binding {
    protected val condition = None
    def get = fn()
  }
}

trait BidingProvider {
  def getBinding(method: Method): Binding
}

class BindingException(message: String, cause: Throwable) extends RuntimeException(message,cause) {
  def this(message: String) = this(message, null)
}