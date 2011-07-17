package org.am.scaldi.util

trait CreationHelper {
  implicit def anyToCreationWrapper[T](obj: T) = new CreationWrapper(obj)

  class CreationWrapper[T](obj: T) {
    def ~(fn: T => Unit) = pass(fn)

    def pass(fn: T => Unit): T = {
      fn(obj)
      obj
    }
  }
}