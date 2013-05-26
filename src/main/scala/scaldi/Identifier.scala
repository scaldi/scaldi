package scaldi

import language.{existentials, implicitConversions}

import annotation.implicitNotFound

trait Identifier {
  def sameAs(other: Identifier): Boolean
}

object Identifier {
  implicit def toIdentifier[T : CanBeIdentifier](target: T): Identifier = implicitly[CanBeIdentifier[T]].toIdentifier(target)
}

@implicitNotFound(msg = "${T} can't be treated as Identifier. Please consider defining CanBeIdentifier for it.")
trait CanBeIdentifier[T] {
  def toIdentifier(target: T): Identifier
}

object CanBeIdentifier {
  implicit val stringCanBeIdentifier = new CanBeIdentifier[String] {
    def toIdentifier(str: String) = StringIdentifier(str)
  }

  implicit val symbolCanBeIdentifier = new CanBeIdentifier[Symbol] {
    def toIdentifier(sym: Symbol) = StringIdentifier(sym.name)
  }

  implicit def classCanBeIdentifier[T] = new CanBeIdentifier[Class[T]] {
    def toIdentifier(c: Class[T]) = ClassIdentifier(c)
  }
}

case class ClassIdentifier(clazz: Class[_]) extends Identifier {
  def sameAs(other: Identifier) = other match {
    case ClassIdentifier(c) if c.isAssignableFrom(clazz) => true
    case _ => false
  }
}

case class StringIdentifier(str: String) extends Identifier {
  def sameAs(other: Identifier) = other match {
    case StringIdentifier(`str`) => true
    case _ => false
  }
}