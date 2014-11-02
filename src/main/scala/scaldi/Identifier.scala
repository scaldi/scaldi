package scaldi

import language.{existentials, implicitConversions}

import scala.reflect.runtime.universe.{TypeTag, Type}
import annotation.implicitNotFound

trait Identifier {
  def sameAs(other: Identifier): Boolean
  def required: Boolean = false
}

object Identifier {
  implicit def toIdentifier[T : CanBeIdentifier](target: T): Identifier = implicitly[CanBeIdentifier[T]].toIdentifier(target)

  def sameAs(actual: List[Identifier], desired: List[Identifier]) = {
    val matching = desired.map(d => actual filter (_ sameAs d))

    !matching.contains(Nil) && {
      val flatMatching = matching.flatten

      actual.filter(_.required).forall(a => flatMatching exists a.sameAs)
    }
  }
}

@implicitNotFound(msg = "${T} can't be treated as Identifier. Please consider defining CanBeIdentifier for it.")
trait CanBeIdentifier[T] {
  def toIdentifier(target: T): Identifier
}

object CanBeIdentifier {
  implicit object StringCanBeIdentifier extends CanBeIdentifier[String] {
    def toIdentifier(str: String) = StringIdentifier(str)
  }

  implicit object SymbolCanBeIdentifier extends CanBeIdentifier[Symbol] {
    def toIdentifier(sym: Symbol) = StringIdentifier(sym.name)
  }

  implicit def ClassCanBeIdentifier[T: TypeTag] = new CanBeIdentifier[Class[T]] {
    def toIdentifier(c: Class[T]) = TypeTagIdentifier.typeId[T]
  }

  implicit def TypeTagCanBeIdentifier[T: TypeTag] = new CanBeIdentifier[TypeTag[T]] {
    def toIdentifier(typeTag: TypeTag[T]) = TypeTagIdentifier(typeTag.tpe)
  }

  implicit object TypeCanBeIdentifier extends CanBeIdentifier[Type] {
    def toIdentifier(tpe: Type) = TypeTagIdentifier(tpe)
  }
}

case class TypeTagIdentifier(tpe: Type) extends Identifier {
  def sameAs(other: Identifier) =
    other match {
      case TypeTagIdentifier(otherTpe) if tpe <:< otherTpe => true
      case _ => false
    }
}

object TypeTagIdentifier {
  def typeId[T: TypeTag] = TypeTagIdentifier(implicitly[TypeTag[T]].tpe)
}

case class StringIdentifier(str: String) extends Identifier {
  def sameAs(other: Identifier) = other match {
    case StringIdentifier(`str`) => true
    case _ => false
  }
}

case class RequiredIdentifier(delegate: Identifier, isRequired: Boolean) extends Identifier {
  def sameAs(other: Identifier) = other match {
    case r: RequiredIdentifier => delegate sameAs r.delegate
    case _ => delegate sameAs other
  }
  override def required = isRequired
}