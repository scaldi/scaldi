package scaldi.util

import language.{postfixOps, implicitConversions}
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.{TypeTag, runtimeMirror, TermName, MethodSymbol, Symbol}
import scala.reflect.internal.{Names, StdNames}
import java.lang.reflect.{Method, Constructor}

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

  private def getPackage(s: Symbol): Symbol = if (s.isPackage) s else getPackage(s.owner)

  // Dirty tricks to support JSR 330

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

   jmethod.getParameterAnnotations.toList map (_.toList)
  }
}