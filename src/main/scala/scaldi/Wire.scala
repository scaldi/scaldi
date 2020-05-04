package scaldi

import scaldi.internal.WireCrossHelper._

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

trait Wire {
  def injected[T]: T = macro WireBuilder.wireNoArgImpl[T]

  def injected[T](overrides: (Symbol, _)*): T = macro WireBuilder.wireImpl[T]

  def injected[T](overrides: (String, _)*): T = macro WireBuilder.wireImpl[T]
}

class WireBuilder {
  def build[T: c.WeakTypeTag](c: blackbox.Context)(overrides: Seq[c.Tree]): c.Expr[T] = {
    val validatedOverrides = overrides.map(o => extractProperty(c)(o))

    validatedOverrides find (_.isLeft) match {
      case Some(Left(errorMsg)) => error(c, errorMsg)
      case _ => wireType(c)(Map(validatedOverrides collect {case Right(v) => v}: _*))
    }
  }

  def extractPropertyName(c: blackbox.Context)(tree: c.Tree): String = {
    import c.universe._

    tree match {
      case q"$symbolType.apply($symbol)" =>
        val Literal(Constant(name: String)) = symbol
        name
      case Literal(Constant(name: String)) =>
        name
    }
  }

  def extractProperty(c: blackbox.Context)(tree: c.Tree): Either[String, (String, c.Tree)] = {
    import c.universe._

    tree match {
      // support for arrow syntax: 'prop -> inject [X]
      case q"$arrowAssoc($lhs).->[$rhsType]($expr)" =>
        Right(extractPropertyName(c)(lhs) -> expr)

      // normal tuple syntax: ('prop, inject [X])
      case q"($lhs, $expr)" =>
        Right(extractPropertyName(c)(lhs) -> expr)

      case t =>
        Left(s"Unsupported syntax for the overrides: `${c.universe.showCode(t)}`. Supported syntax is either tuple `('name, inject[X])` or arrow `'name -> inject [X]`.")
    }
  }

  def wireParam(c: blackbox.Context)(param: c.Symbol, wiredType: c.Type, defaultSupported: Boolean, validOverrides: Map[String, c.Tree]): Either[String, c.Tree] = {
    import c.universe._

    val name = param.name.decodedName.toString
    val tpe = param.typeSignature
    val hasDefault = param.asTerm.isParamWithDefault

    if (!defaultSupported && hasDefault)
      Left(s"Argument $name has a default value, but default values are only supported for the first argument list.")
    else
      Right(CrossNamedArg(c)(
        Ident(TermName(name)),
        validOverrides.getOrElse(name, {
          if (hasDefault) q"injectWithConstructorDefault[$tpe, $wiredType]($name)"
          else q"inject[$tpe]"
        })
      ))
  }

  def wireParamList(c: blackbox.Context)(paramList: List[c.Symbol], wiredType: c.Type, defaultSupported: Boolean, validOverrides: Map[String, c.Tree]): List[Either[String, c.Tree]] =
    paramList map (param => wireParam(c)(param, wiredType, defaultSupported, validOverrides))

  def wireType[T: c.WeakTypeTag](c: blackbox.Context)(validOverrides: Map[String, c.Tree]): c.Expr[T] = {
    import c.universe._

    val tpe = implicitly[c.WeakTypeTag[T]].tpe

    tpe.members find (_.isConstructor) map (_.asMethod) match {
      case None =>
        error(c, s"Type $tpe has no constructor.")
      case Some(constructor) =>
        val availableNames = constructor.paramLists.flatten map (p => p.name.decodedName.toString)
        val overriddenNames = validOverrides.keySet
        val nonExistingNames = overriddenNames filterNot availableNames.contains

        if (nonExistingNames.nonEmpty) {
          error(c, s"$tpe constructor does not have arguments: ${nonExistingNames mkString ", "}. Available arguments are: ${availableNames mkString ", "}")
        } else {
          val paramLists = constructor.paramLists.zipWithIndex map {
            case (list, idx) => wireParamList(c)(list, tpe, idx == 0, validOverrides)
          }

          paramLists.flatten find (_.isLeft) match {
            case Some(Left(errorMsg)) =>
              error(c, errorMsg)
            case _ =>
              val wiredType = paramLists.foldLeft (Select(New(Ident(tpe.typeSymbol)), termNames.CONSTRUCTOR): c.Tree) {
                case (acc, l) => Apply(acc, l collect {case Right(v) => v})
              }

//              c.info(c.enclosingPosition, "Wired: " + c.universe.show(wiredType), false)
              c.Expr[T](wiredType)
          }
        }
    }
  }

  def error[T](c: blackbox.Context, message: String): c.Expr[T] = {
    import c.universe._

    c.error(c.enclosingPosition, message)
    c.Expr[T](q"null")
  }
}

object WireBuilder {
  def wireNoArgImpl[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[T] =
    wireImpl[T](c)()

  def wireImpl[T: c.WeakTypeTag](c: blackbox.Context)(overrides: c.Tree*): c.Expr[T] =
    new WireBuilder().build[T](c)(overrides)
}