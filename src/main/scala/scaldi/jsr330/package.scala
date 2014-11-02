package scaldi

import java.lang.annotation.Annotation
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.ClassTag

package object jsr330 {
  def qualifier[T <: Annotation : TypeTag] = AnnotationIdentifier.qualifier[T]
}
