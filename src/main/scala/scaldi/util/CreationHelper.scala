package scaldi.util

import language.implicitConversions

trait CreationHelper {
  implicit def anyToCreationWrapper[T](obj: T) = new CreationWrapper(obj)

  class CreationWrapper[T](obj: T) {
    def ~(fn: T => Unit) = pass(fn)
    def ~![R](fn: T => R) = printPass(fn)

    def pass(fn: T => Unit): T = {
      fn(obj)
      obj
    }

    def printPass[R](fn: T => R): T = {
      println(fn(obj))
      obj
    }

  }
}