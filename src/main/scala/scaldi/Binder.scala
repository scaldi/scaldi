package scaldi

import language.postfixOps

import scaldi.util.Util._
import scala.reflect.runtime.universe.{TypeTag, Type, MethodSymbol, typeTag}

trait WordBinder {
  private var bindingsInProgress: List[BindHelper[_]] = Nil
  private var bindings: List[BoundHelper[_]] = Nil

  lazy val wordBindings: List[BindingWithLifecycle] = {
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
  def bind[T : TypeTag] = createBinding[T](Some(typeTag[T]))

  private def createBinding[T](mainType: Option[TypeTag[_]]) = {
    val helper = new BindHelper[T]({ (bind, bound) =>
      bindingsInProgress = bindingsInProgress filterNot (bind ==)
      bindings = bindings :+ bound
    })
    bindingsInProgress = bindingsInProgress :+ helper
    mainType foreach (helper identifiedBy _)
    helper
  }

  protected def initNonLazyWordBindings(lifecycleManager: LifecycleManager): () => Unit =
    wordBindings |> (b => () => b.collect{case binding: NonLazyBinding => binding}.foreach(_ get lifecycleManager))
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
  var condition: Option[() => Condition] = None

  def when(cond: => Condition) = {
    condition = Some(() => cond)
    this
  }
}

trait CanHaveLifecycle[H, D] { this: H =>
  var lifecycle = BindingLifecycle.empty[D]

  def initWith(initFn: (D) => Unit) = {
    lifecycle = lifecycle.copy(initialize = Some(initFn))
    this
  }

  def destroyWith(destroyFn: (D) => Unit) = {
    lifecycle = lifecycle.copy(destroy = Some(destroyFn))
    this
  }
}

class BindHelper[R](onBound: (BindHelper[R], BoundHelper[_]) => Unit)
    extends CanBeIdentified[BindHelper[R]] with CanBeConditional[BindHelper[R]] {
  var createFn: Option[Option[() => Any]] = None

  def to(none: None.type) = bindNone[R](LazyBinding(None, _, _))
  def to[T <: R : TypeTag](fn: => T) = bind(LazyBinding(Some(() => fn), _, _))
  def in[T <: R : TypeTag](fn: => T) = to(fn)

  def toNonLazy[T <: R : TypeTag](fn: => T) = bind(NonLazyBinding(Some(() => fn), _, _))
  def inNonLazy[T <: R : TypeTag](fn: => T) = toNonLazy(fn)

  def toProvider[T <: R : TypeTag](fn: => T) = bind(ProviderBinding(() => fn, _, _))
  def inProvider[T <: R : TypeTag](fn: => T) = toProvider(fn)

  private def bind[T : TypeTag](bindingFn: (List[Identifier], Option[() => Condition]) => BindingWithLifecycle) = {
    val bound = new BoundHelper[T](bindingFn, identifiers, condition, Some(typeTag[T].tpe))
    onBound(this, bound)
    bound
  }

  private def bindNone[D](bindingFn: (List[Identifier], Option[() => Condition]) => BindingWithLifecycle) = {
    val bound = new BoundHelper[D](bindingFn, identifiers, condition, None)
    onBound(this, bound)
    bound
  }
}

class BoundHelper[D](
   bindingFn: (List[Identifier], Option[() => Condition]) => BindingWithLifecycle,
   initialIdentifiers: List[Identifier],
   initialCondition: Option[() => Condition],
   bindingType: Option[Type]
) extends CanBeIdentified[BoundHelper[D]] with CanBeConditional[BoundHelper[D]] with CanHaveLifecycle[BoundHelper[D], D] {
  def getBinding = bindingFn (
    (initialIdentifiers ++ identifiers, bindingType) match {
      case (ids, _) if ids.exists(_.isInstanceOf[TypeTagIdentifier]) => ids
      case (ids, Some(t)) => ids :+ TypeTagIdentifier(t)
      case (ids, None) => ids
    },
    condition orElse initialCondition
  )
}

trait ReflectionBinder {
  lazy val reflectiveBindings: List[Binding] = {
    import scala.reflect.runtime.universe._

    val mirror = runtimeMirror(this.getClass.getClassLoader)
    val reflection = mirror.reflect(this)

    // TODO: filter even more - all library, Scala and JDK methods should be somehow filtered!
    mirror.classSymbol(this.getClass).toType
      .members
      .filter(_.isPublic)
      .filter(_.isMethod)
      .map(_.asMethod)
      .map { m =>
        if (m.returnType <:< typeOf[BindingProvider])
          reflection.reflectMethod(m).apply().asInstanceOf[BindingProvider].getBinding(m.name.decoded, m.returnType)
        else
          ReflectiveBinding(() => Some(reflection.reflectMethod(m).apply()), List(m.returnType, m.name.decoded))
      }
      .toList
  }

  case class ReflectiveBinding(fn: () => Option[Any], identifiers: List[Identifier]) extends Binding {
    val condition = None
    override def get = fn()
  }
}

trait BindingProvider {
  def getBinding(name: String, tpe: Type): Binding
}

class BindingException(message: String, cause: Throwable) extends RuntimeException(message,cause) {
  def this(message: String) = this(message, null)
}