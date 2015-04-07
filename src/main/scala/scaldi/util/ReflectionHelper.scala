package scaldi.util

import java.lang.annotation.Annotation

import language.{postfixOps, implicitConversions}
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.{TypeTag, Type, runtimeMirror, TermName, MethodSymbol, TermSymbol, Symbol}
import scala.reflect.internal.{Names, StdNames}
import java.lang.reflect.{Field, Method, Constructor}

object ReflectionHelper {
  def getDefaultValueOfParam[T, C](paramName: String)(implicit tt: TypeTag[C]) = {
    val tpe = tt.tpe

    tpe.members find (_.isConstructor) map (_.asMethod) match {
      case None =>
        throw new IllegalArgumentException(s"Type $tpe has no constructor.")
      case Some(constructor) =>
        constructor.paramLists.headOption.toList.flatten.zipWithIndex.find (_._1.name.decodedName.toString == paramName) match {
          case Some((param, idx)) if param.isTerm && param.asTerm.isParamWithDefault =>
            import universe._

            val names = universe.asInstanceOf[StdNames with Names]
            val name = names.nme.defaultGetterName(names.nme.CONSTRUCTOR, idx + 1).encodedName.toString
            val mirror = runtimeMirror(this.getClass.getClassLoader)
            val reflection = mirror.reflect(mirror.reflectModule(tpe.typeSymbol.companion.asModule).instance)

            reflection.reflectMethod(tpe.companion.member(TermName(name)).asMethod).apply().asInstanceOf[T]
          case _ =>
            throw new IllegalArgumentException(s"Can't find constructor argument $paramName with default value. Note, that only the first argument list is supported.")
        }
    }
  }

  def mirror = {
    val classLoader =
      if (Thread.currentThread.getContextClassLoader != null)
        Thread.currentThread.getContextClassLoader
      else this.getClass.getClassLoader

    runtimeMirror(classLoader)
  }

  def overrides(method: Symbol) = {
    val origPackage = getPackage(method)

    method.overrides.filter(o => o.isPublic || o.isProtected || (!o.isPrivate && getPackage(o) == origPackage))
  }

  def classToType(clazz: Class[_]) =
    mirror.classSymbol(clazz).toType

  private def getPackage(s: Symbol): Symbol = if (s.isPackage) s else getPackage(s.owner)

  def hasAnnotation[T <: Annotation : TypeTag](a: Annotation): Boolean =
    hasAnnotation[T](classToType(a.getClass))

  def hasAnnotation[T <: Annotation : TypeTag](t: Type): Boolean = {
    val expectedTpe = implicitly[TypeTag[T]].tpe

    t.baseClasses flatMap (_.annotations) exists (_.tree.tpe =:= expectedTpe)
  }

  // Dirty tricks to compensate for scala reflection API missing feature or bugs

  // Workaround for https://issues.scala-lang.org/browse/SI-9177
  // TODO: get rid of this workaround as soon as https://issues.scala-lang.org/browse/SI-9177 is resolved!
  def isAssignableFrom(a: Type, b: Type) =
    try {
      b <:< a
    } catch {
      case e: Throwable if e.getMessage != null && e.getMessage.contains("illegal cyclic reference") =>
        false
    }

  /**
   * Dirty little trick to convert java constructor to scala constructor.
   * (the reason for it is that scala reflection does not list private constructors)
   */
  def constructorSymbol(c: Constructor[_]) = {
    val mirror = ReflectionHelper.mirror
    val constructorConverter = mirror.classSymbol(mirror.getClass).typeSignature.member(TermName("jconstrAsScala")).asMethod

    mirror.reflect(mirror: AnyRef).reflectMethod(constructorConverter).apply(c).asInstanceOf[MethodSymbol]
  }

  /**
   * Dirty little trick to convert get method argument annotations.
   * (the reason for it is that scala reflection does not give this information)
   */
  def methodParamsAnnotations(method: MethodSymbol) = {
    val mirror = ReflectionHelper.mirror
    val methodToJava = mirror.classSymbol(mirror.getClass).typeSignature.member(TermName("methodToJava")).asMethod
    val jmethod = mirror.reflect(mirror: AnyRef).reflectMethod(methodToJava).apply(method).asInstanceOf[Method]

    jmethod.getAnnotations.toList -> jmethod.getParameterAnnotations.toList.map(_.toList)
  }

  def fieldAnnotations(field: TermSymbol) = {
    val mirror = ReflectionHelper.mirror
    val fieldToJava = mirror.classSymbol(mirror.getClass).typeSignature.member(TermName("fieldToJava")).asMethod
    val jfield = mirror.reflect(mirror: AnyRef).reflectMethod(fieldToJava).apply(field).asInstanceOf[Field]

    jfield.getAnnotations.toList
  }
}