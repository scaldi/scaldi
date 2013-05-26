package scaldi.util

import language.{postfixOps, implicitConversions}

object ReflectionHelper {
    implicit def classToReflectionWrapper(cl: Class[_]) = new ReflectionWrapper(cl)
    implicit def objectToReflectionObjectWrapper(obj: Object) = new ReflectionObjectWrapper(obj)
}

class ReflectionWrapper(clazz: Class[_]) {
    import ReflectionHelper._

    /**
     * @return first method with provided name that satisfies provided conditions
     */
    def getMatchingMethod(name: Option[String], returnType: Class[_], args: Class[_]*) =
      clazz.getMethods.find { method =>
        (name map (method.getName ==) getOrElse true) &&
        returnType.isAssignableFrom(method.getReturnType) &&
        method.getParameterTypes.length == args.length &&
        !method.getParameterTypes.zipWithIndex.exists {case (cl, idx) => cl != args(idx)}
      }

    /**
     * @return all methods that satisfy provided conditions
     */
    def getMatchingMethods(name: Option[String], returnType: Class[_], args: Class[_]*) =
      clazz.getMethods.filter { method =>
          (name map (method.getName ==) getOrElse true) &&
          returnType.isAssignableFrom(method.getReturnType) &&
          method.getParameterTypes.length == args.length &&
          !method.getParameterTypes.zipWithIndex.exists {case (cl, idx) => cl != args(idx)}
      } toList

    def getAnnotatedMethods(returnType: Class[_], args: Class[_]*) = clazz.getMethods.filter { method =>
        returnType.isAssignableFrom(method.getReturnType) &&
        method.getParameterTypes.length == args.length &&
        !method.getParameterTypes.zipWithIndex.exists {case (cl, idx) => cl != args(idx)}
    } toList
}

class ReflectionObjectWrapper(obj: Object) {

    /**
     * @return Some(value) of the <code>val</code> or None if such val not found
     */
    def getValValue[T](name: String)(implicit m: Manifest[T]): Option[T] =
        obj.getClass.getMethods.find(_.getName == name) match {
            case Some(method) =>
                if (m.runtimeClass.isAssignableFrom(method.getReturnType) && method.getParameterTypes.length == 0)
                    Some(method.invoke(obj).asInstanceOf[T])
                else
                    None
            case None => None
        }
}