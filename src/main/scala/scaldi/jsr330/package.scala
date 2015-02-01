package scaldi

import java.lang.annotation.Annotation
import scala.reflect.runtime.universe.{TypeTag, typeTag}

package object jsr330 {
  def qualifier[T <: Annotation : TypeTag] = AnnotationIdentifier.qualifier[T]
  def annotated[T : TypeTag](implicit injector: () => Injector) = WordBindingProvider[T](AnnotationBinding(Right(typeTag[T].tpe), injector, _, _, _))
  def annotated[T <: AnyRef : TypeTag](instance: T)(implicit injector: () => Injector) = WordBindingProvider[T](AnnotationBinding(Left(instance), injector, _, _, _))
}
