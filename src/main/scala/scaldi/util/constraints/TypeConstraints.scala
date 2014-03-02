package scaldi.util.constraints

import scala.annotation.implicitNotFound

sealed trait Existence
trait Exists extends Existence
trait NotExists extends Existence

trait IsTypeClassExists[TypeClass, Answer]

object IsTypeClassExists extends LowPriorityIsTypeClassExists {
  implicit def typeClassExistsEv[TypeClass, Answer](implicit a: TypeClass) =
    evidence.asInstanceOf[IsTypeClassExists[TypeClass, Exists]]
}

trait LowPriorityIsTypeClassExists {
  protected val evidence: IsTypeClassExists[Any, Any] =
    new Object with IsTypeClassExists[Any, Any]

  implicit def typeClassNotExistsEv[TypeClass, Answer] =
    evidence.asInstanceOf[IsTypeClassExists[TypeClass, NotExists]]
}

@implicitNotFound("Argument does not satisfy constraints: Not ${T}")
trait Not[T]

object Not {
  private val evidence: Not[Any] = new Object with Not[Any]

  implicit def notEv[T, Answer](implicit a: IsTypeClassExists[T, Answer], ne: Answer =:= NotExists) =
    evidence.asInstanceOf[Not[T]]
}

@implicitNotFound("Argument does not satisfy constraints: ${A} And ${B}")
trait And[A, B]

object And {
  private val evidence: And[Any, Any] = new Object with And[Any, Any]

  implicit def bothExistEv[A, B](implicit a: A, b: B) =
    evidence.asInstanceOf[And[A, B]]
}

@implicitNotFound("Argument does not satisfy constraints: ${A} Or ${B}")
trait Or[A, B] {
  def get: Either[A, B]
}

object Or {
  implicit def aExistsEv[A, B](implicit a: A) = new Or[A, B] {
    def get = Left(a)
  }

  implicit def bExistsEv[A, B](implicit b: B) = new Or[A, B] {
    def get = Right(b)
  }
}

@implicitNotFound("Type inference was unable to figure out the type. You need to provide it explicitly.")
trait NotNothing[T]

object NotNothing {
  private val evidence: NotNothing[Any] = new Object with NotNothing[Any]

  implicit def notNothingEvidence[T](implicit n: T =:= T): NotNothing[T] =
    evidence.asInstanceOf[NotNothing[T]]
}