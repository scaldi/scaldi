package scaldi

import scaldi.util.ReflectionHelper

import language.{existentials, implicitConversions}

import scala.reflect.runtime.universe.{TypeTag, Type}
import annotation.implicitNotFound

/**
  * Used to identify a binding.
  * If you have two bindings of the same type (like `String` bindings), the identifiers
  * may be used to identify and to inject the right one
  */
trait Identifier {
  /**
    * Determines if one  `Identifier` is equal to another `Identifier`
    * @param other other `Identifier`
    * @return true if both identifiers are the same, false otherwise
    */
  def sameAs(other: Identifier): Boolean

  /**
    * `Identifier` may be marked as required, in which case the identifier must be used
    * in order to get the binding
    * @return true if `Identifier` is required, false otherwise
    */
  def required: Boolean = false
}

object Identifier {
  /**
    * Implicitly casts a type that can be `Identifier` to an identifier
    * @param target value to be casted to `Identifier`
    * @tparam T class that can be casted to an `Identifier`
    * @return resulting `Identifier`
    */
  implicit def toIdentifier[T : CanBeIdentifier](target: T): Identifier = implicitly[CanBeIdentifier[T]].toIdentifier(target)

  /**
    * Compares two lists of identifiers, true if actual lists at least contains
    * all the identifiers in the desired list.
    * Also checks if all the required identifiers in the actual list are matched in the desired list
    * @param actual list of actual identifiers to compare
    * @param desired list of desired identifiers to compare
    * @return true if actual list contains at least all the identifiers in the desired
    *         list and the required actual identifiers are in desired list, false otherwise
    */
  def sameAs(actual: List[Identifier], desired: List[Identifier]) = {
    val matching = desired.map(d => actual filter (_ sameAs d))

    !matching.contains(Nil) && {
      val flatMatching = matching.flatten

      actual.filter(_.required).forall(a => flatMatching exists a.sameAs)
    }
  }
}

/**
  * Type class to implicitly cast `T` class to an `Identifier`
  * @tparam T type that should be casted to an Identifier
  */
@implicitNotFound(msg = "${T} can't be treated as Identifier. Please consider defining CanBeIdentifier for it.")
trait CanBeIdentifier[T] {
  def toIdentifier(target: T): Identifier
}

object CanBeIdentifier {

  /**
    * Type class implementation to cast a `String` to an `Identifier`.
    * Has priority over other type class implementations
    */
  implicit object StringCanBeIdentifier extends CanBeIdentifier[String] {
    def toIdentifier(str: String) = StringIdentifier(str)
  }

  /**
    * Type class implementation to cast a `Symbol` to an `Identifier`
    */
  implicit object SymbolCanBeIdentifier extends CanBeIdentifier[Symbol] {
    def toIdentifier(sym: Symbol) = StringIdentifier(sym.name)
  }

  /**
    * Type class implementation to cast a ??? to an `Identifier`
    */
  implicit def ClassCanBeIdentifier[T: TypeTag] = new CanBeIdentifier[Class[T]] {
    def toIdentifier(c: Class[T]) = TypeTagIdentifier.typeId[T]
  }

  /**
    * Type class implementation to cast a ??? to an `Identifier`
    */
  implicit def TypeTagCanBeIdentifier[T: TypeTag] = new CanBeIdentifier[TypeTag[T]] {
    def toIdentifier(typeTag: TypeTag[T]) = TypeTagIdentifier(typeTag.tpe)
  }

  /**
    * Type class implementation to cast a ??? to an `Identifier`
    */
  implicit object TypeCanBeIdentifier extends CanBeIdentifier[Type] {
    def toIdentifier(tpe: Type) = TypeTagIdentifier(tpe)
  }

  /**
    * Type class implementation to cast a `Identifier` implementation to an `Identifier`
    */
  implicit def identifierCanBeIdentifier[I <: Identifier] = new CanBeIdentifier[I] {
    def toIdentifier(id: I) = id
  }
}

/**
  * Identifier created from any Scala type
  * @param tpe instance that will be defined as in `Identifier`
  */
case class TypeTagIdentifier(tpe: Type) extends Identifier {
  /**
    * @inheritdoc
    */
  def sameAs(other: Identifier) =
    other match {
      case TypeTagIdentifier(otherTpe) if ReflectionHelper.isAssignableFrom(otherTpe, tpe) => true
      case _ => false
    }
}

object TypeTagIdentifier {
  /**
    * ???
    * @tparam T ???
    * @return ???
    */
  def typeId[T: TypeTag] = TypeTagIdentifier(implicitly[TypeTag[T]].tpe)
}

/**
  * Defines an identifier from a `String`
  * @param str `String` that will be the identifier
  */
case class StringIdentifier(str: String) extends Identifier {
  /**
    * @inheritdoc
    */
  def sameAs(other: Identifier) = other match {
    case StringIdentifier(`str`) => true
    case _ => false
  }
}

/**
  * Wraps a delegated `Identifier` as a required or not required
  * @param delegate delegated `Identifier`
  * @param isRequired true if the wrapped identifier should be required
  */
case class RequiredIdentifier(delegate: Identifier, isRequired: Boolean) extends Identifier {
  /**
    * @inheritdoc
    */
  def sameAs(other: Identifier) = other match {
    case r: RequiredIdentifier => delegate sameAs r.delegate
    case _ => delegate sameAs other
  }

  /**
    * @inheritdoc
    */
  override def required = isRequired
}