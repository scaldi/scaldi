package org.am.scaldi.util

object Util extends CreationHelper {
  implicit def toWorkflowHelper[T](any: T) = new WorkflowHelper[T](any)

  class WorkflowHelper[T](target: T) {
    def |>[R](fn: T => R) = fn(target)
  }
}