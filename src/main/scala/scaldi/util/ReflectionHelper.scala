package scaldi.util

import language.{postfixOps, implicitConversions}
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.internal.{Names, StdNames}

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
}