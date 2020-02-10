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
    * Implicitly transforms a type that can be `Identifier` to an identifier
    * @param target value to be transformed to `Identifier`
    * @tparam T class that can be transformed to an `Identifier`
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
  * Implementation to implicitly transform `T` class to an `Identifier`
  * @tparam T type that should be transformed to an Identifier
  */
@implicitNotFound(msg = "${T} can't be treated as Identifier. Please consider defining CanBeIdentifier for it.")
trait CanBeIdentifier[T] {
  def toIdentifier(target: T): Identifier
}

object CanBeIdentifier {

  /**
    * Implementation to implicitly transform `String` into an `Identifier`.
    * Has priority over other type class implementations
    */
  implicit object StringCanBeIdentifier extends CanBeIdentifier[String] {
    def toIdentifier(str: String) = StringIdentifier(str)
  }

  /**
    * Implementation to implicitly transform `Symbol` into an `Identifier`
    */
  implicit object SymbolCanBeIdentifier extends CanBeIdentifier[Symbol] {
    def toIdentifier(sym: Symbol) = StringIdentifier(sym.name)
  }

  /**
    * Implementation to implicitly transform a Class into an `Identifier`
    */
  implicit def ClassCanBeIdentifier[T: TypeTag] = new CanBeIdentifier[Class[T]] {
    def toIdentifier(c: Class[T]) = TypeTagIdentifier.typeId[T]
  }

  /**
    * Implementation to implicitly transform a Type Parameter into an `Identifier`
    */
  implicit def TypeTagCanBeIdentifier[T: TypeTag] = new CanBeIdentifier[TypeTag[T]] {
    def toIdentifier(typeTag: TypeTag[T]) = TypeTagIdentifier(typeTag.tpe)
  }

  /**
    * Implementation to implicitly transform a Scala Type into an `Identifier`
    */
  implicit object TypeCanBeIdentifier extends CanBeIdentifier[Type] {
    def toIdentifier(tpe: Type) = TypeTagIdentifier(tpe)
  }

  /**
    * Implementation to implicitly transform an `Identifier` implementation into an `Identifier`
    */
  implicit def identifierCanBeIdentifier[I <: Identifier] = new CanBeIdentifier[I] {
    def toIdentifier(id: I) = id
  }
}

/**
  * Identifier created from any Scala type
  * @param tpe instance that will be defined as in `Identifier`
  */
final class TypeTagIdentifier private(val tpe: Type) extends Identifier {
  /**
    * @inheritdoc
    */
  def sameAs(other: Identifier) =
    other match {
      case TypeTagIdentifier(otherTpe) if ReflectionHelper.isAssignableFrom(otherTpe, tpe) => true
      case _ => false
    }

  override def equals(obj: Any): Boolean = obj match {
    case that: TypeTagIdentifier => this.tpe == that.tpe
    case _ => false
  }
  override def hashCode(): Int = tpe.hashCode()
  override def toString: String = s"TypeTagIdentifier($tpe)"
}

object TypeTagIdentifier {
  /**
    * Generates an `Identifier` from a type parameter within TypeTag
    * @tparam T `TypeTag` containing type parameter
    * @return Generated `Identifier`
    */
  def typeId[T: TypeTag]: TypeTagIdentifier = apply(implicitly[TypeTag[T]].tpe)

  def apply(tpe: Type): TypeTagIdentifier =
    new TypeTagIdentifier(tpe.dealias)

  def unapply(that: TypeTagIdentifier): Some[Type] = Some(that.tpe)
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