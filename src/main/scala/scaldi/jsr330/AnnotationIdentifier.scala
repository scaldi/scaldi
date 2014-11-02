package scaldi.jsr330

import scaldi.{InjectException, Identifier}
import scala.reflect.runtime.universe.{Type, TypeTag, typeOf}
import javax.inject.Qualifier
import java.lang.annotation.Annotation

case class AnnotationIdentifier(tpe: Type) extends Identifier {
  def sameAs(other: Identifier) =
    other match {
      case AnnotationIdentifier(otherTpe) if tpe <:< otherTpe => true
      case _ => false
    }

  override def required = true
}

object AnnotationIdentifier {
  def qualifier[T <: Annotation : TypeTag] = {
    val tpe = implicitly[TypeTag[T]].tpe

    if (!tpe.typeSymbol.annotations.exists(_.tree.tpe =:= typeOf[Qualifier]))
      throw new InjectException(s"Annotation `$tpe` is not a qualifier annotation.")

    AnnotationIdentifier(tpe)
  }
}
