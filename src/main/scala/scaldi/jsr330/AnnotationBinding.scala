package scaldi.jsr330

import scala.reflect.runtime.universe._
import javax.inject.{Provider => JProvider, Scope, Named, Qualifier, Inject, Singleton}
import scaldi.util.ReflectionHelper
import java.lang.reflect.Constructor
import java.lang.annotation.Annotation

import scaldi._

/**
 * Binding for JSR 330 compliant types.
 */
case class AnnotationBinding(
  instanceOrType: Either[AnyRef, Type],
  injector: () => Injector,
  identifiers: List[Identifier] = Nil,
  condition: Option[() => Condition] = None,
  lifecycle: BindingLifecycle[Any] = BindingLifecycle.empty,
  eager: Boolean = false,
  forcedScope: Option[Type] = None,
  bindingConverter: Option[AnyRef => AnyRef] = None
) extends BindingWithLifecycle {
  import AnnotationBinding._

  private val tpe =
    instanceOrType match {
      case Left(inst) =>
        ReflectionHelper.mirror.classSymbol(inst.getClass).toType
      case Right(t) => t
    }

  private val creator = {
    if (tpe.typeSymbol.isAbstract || !tpe.typeSymbol.isClass)
      throw new InjectException(s"Type `$tpe` should be non-abstract class.")

    instanceOrType match {
      case Left(inst) => (_: Injector) => inst
      case Right(t) =>
        findConstructor(tpe) map (c => createNewInstance(c) _) getOrElse (
          throw new InjectException(s"Type `$tpe` should either define default no-arg constructor or one constructor marked with javax.inject.Inject annotation."))
    }
  }

  private val fieldsAndMethods = {
    val all = tpe.baseClasses.map(_.asType.toType).reverse.foldLeft(List[(Iterable[Symbol], Iterable[Symbol], Iterable[Symbol], Iterable[Symbol])]()) {
      case (acc, t) =>
        val allFields = t.decls.filter(f => f.isTerm && f.asTerm.isVar)
        val allMethods = t.decls.filter(m => m.isMethod && !m.isConstructor)

        acc :+ (allFields, allFields filter isInjected, allMethods, allMethods filter isInjected)
    }

    val fieldOverrides = all.flatMap(_._1).toSet flatMap ReflectionHelper.overrides
    val methodOverrides = all.flatMap(_._3).toSet flatMap ReflectionHelper.overrides

    all map {case (af, f, am, m) => (f filterNot fieldOverrides.contains, m filterNot methodOverrides.contains)}
  }

  private val scopes = {
    val typeScopes = tpe.typeSymbol.annotations
      .map(_.tree.tpe)
      .filter(_.typeSymbol.annotations.exists(_.tree.tpe =:= typeOf[Scope]))

    val allScopes = typeScopes ++ forcedScope.toList
    val customScopes = allScopes.filterNot(_ =:= typeOf[Singleton])

    if (customScopes.nonEmpty)
      throw new BindingException(s"Type `$tpe` contains custom JSR 330 scopes: ${customScopes mkString ", "}. Only `Singleton` scope is supported.")

    allScopes
  }

  private val singleton = scopes exists (_ =:= typeOf[Singleton])

  private var instance: Option[AnyRef] = None

  override def isCacheable = singleton && condition.isEmpty

  override def get(lifecycleManager: LifecycleManager) = {
    val (instance, isNew) = getInstance()

    for {
      d <- lifecycle.destroy
      if isNew
    } lifecycleManager addDestroyable (() => d(instance))

    instance
  }

  override def isEager = eager

  private def getInstance() = {
    if (singleton)
      if (instance.isDefined) instance -> false
      else {
        this.synchronized {
          if (instance.isDefined) instance -> false
          else {
            val inst = initNewInstance()
            instance = Some(bindingConverter map (_(inst)) getOrElse inst)
            instance -> true
          }
        }
      }
    else {
      val inst = initNewInstance()

      Some(bindingConverter map (_(inst)) getOrElse inst) -> true
    }
  }

  def initNewInstance() = {
    val inj = injector()
    val inst = creator(inj)

    fieldsAndMethods foreach { case (fields, methods) =>
      fields foreach (injectField(inj, inst, _))
      methods foreach (injectMethod(inj, inst, _))
    }

    inst
  }

  private def createNewInstance(jConstructor: Constructor[_])(inj: Injector): AnyRef = {
    val constructor = ReflectionHelper.constructorSymbol(jConstructor)
    val annotations = jConstructor.getParameterAnnotations.toList map (_.toList)
    val params = constructor.typeSignature.paramLists.head.zip(annotations).map(injectSymbol(inj))

    val actualParams = params.map {
      case ref: AnyRef => ref
      case _ => throw new InjectException("JSR 330 integration does not support injection of `AnyVal`.")
    }

    if (!jConstructor.isAccessible) jConstructor.setAccessible(true) // :(

    jConstructor.newInstance(actualParams: _*).asInstanceOf[AnyRef]
  }

  private def injectField(inj: Injector, instance: AnyRef, field: Symbol) =
    ReflectionHelper.mirror.reflect(instance).reflectField(field.asTerm) set injectSymbol(inj)(field -> Nil)

  private def injectMethod(inj: Injector, instance: AnyRef, method: Symbol) = {
    val annotations = ReflectionHelper.methodParamsAnnotations(method.asMethod)
    val params = method.typeSignature.paramLists.head.zip(annotations).map(injectSymbol(inj))
    val reflection = ReflectionHelper.mirror.reflect(instance)

    reflection.reflectMethod(method.asMethod).apply(params: _*)
  }

  private def injectSymbol(inj: Injector)(symbolWithAnnotations: (Symbol, List[Annotation])) = {
    val (s, annotations) = symbolWithAnnotations
    val it = s.typeSignature.resultType

    if (it <:< typeOf[JProvider[_]]) {
      val actualType = it.typeArgs(0)
      val identifiers = TypeTagIdentifier(actualType) :: (symbolIds(s) ++ annotationIds(annotations))

      ScaldiProvider(() => inj.getBinding(identifiers) flatMap (_.get) getOrElse Injectable.noBindingFound(identifiers))
    } else {
      val identifiers = TypeTagIdentifier(it) :: (symbolIds(s) ++ annotationIds(annotations))

      inj.getBinding(identifiers) flatMap (_.get) getOrElse Injectable.noBindingFound(identifiers)
    }
  }

  private def symbolIds(s: Symbol) =
    s.annotations map (a => (a.tree.tpe, a.tree.tpe.typeSymbol.annotations, a.tree.children.tail)) collect {
      case (at, aa, args) if at =:= typeOf[Named] =>
        val value = args
          .collect {case AssignOrNamedArg(Ident(TermName("value")), Literal(Constant(b))) => b.asInstanceOf[String]}
          .head

        StringIdentifier(value)
      case (at, aa, args) if aa.exists(_.tree.tpe =:= typeOf[Qualifier]) =>
        AnnotationIdentifier(at)
    }

  private def annotationIds(annotations: List[Annotation]) =
    annotations collect {
      case named: Named =>
        StringIdentifier(named.value())
      case a if a.annotationType().getAnnotation(classOf[Qualifier]) != null =>
        AnnotationIdentifier(ReflectionHelper.mirror.classSymbol(a.annotationType()).toType)
    }
}

object AnnotationBinding {

  /**
   * Extracts a list of identifiers from JSR 330 compliant type
   */
  def extractIdentifiers(tpe: Type): List[Identifier] =
    findConstructor(tpe) map (_ =>  TypeTagIdentifier(tpe) :: Nil) getOrElse Nil

  /**
   * Finds suitable constructor. Unfortunately using java reflection at this point,
   * because scala reflection does not lists package protected constructors :(
   */
  private def findConstructor(t: Type) =
    if (t.typeSymbol.isAbstract || !t.typeSymbol.isClass) None
    else {
      val clazz = ReflectionHelper.mirror.runtimeClass(t.typeSymbol.asClass)

      val allConstructors = clazz.getDeclaredConstructors
      val injectedConstructors = allConstructors filter isInjected
      def defaultConstructor = allConstructors find (_.getParameterTypes.isEmpty)

      if (injectedConstructors.size > 1)
        throw new InjectException(s"Type `$t` defines more than one injected constructor.")

      injectedConstructors.headOption orElse defaultConstructor
    }

  private def isInjected(s: Symbol) =
    s.annotations.exists(_.tree.tpe =:= typeOf[Inject])

  private def isInjected(c: Constructor[_]) = c.getAnnotations.exists(classOf[Inject] isAssignableFrom _.getClass)
}

case class ScaldiProvider[T](impl: () => T) extends JProvider[T] {
  def get() = impl()
}