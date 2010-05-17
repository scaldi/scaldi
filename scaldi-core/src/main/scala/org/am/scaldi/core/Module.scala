package org.am.scaldi.core

import org.angelsmasterpiece.scala.essentials.reflection.ReflectionHelper._
import java.lang.annotation.Annotation

/**
 * Standard application module
 *
 * @author Oleg Ilyenko
 */
trait Module {

    def byName[T](name: String)(implicit m:Manifest[T]): T = findByName(name, m.erasure).asInstanceOf[Option[T]] match {
        case Some(value) => value
        case None => throw new IllegalStateException("Dependency not found with name: " + name)
    }

    def byType[T](implicit m:Manifest[T]): T = findByType(m.erasure).asInstanceOf[List[T]] match {
        case Nil => throw new IllegalStateException("Dependency not found of type: " + m.erasure.getName)
        case xs => xs(0)
    }

    def allByType[T](implicit m:Manifest[T]): List[T] = findByType(m.erasure, true).asInstanceOf[List[T]]

    private def findByType[T](cl: Class[T], allowMoreThanOne: Boolean = false): List[T] = this.getClass.getMatchingMethods(cl) match {
        case xs if xs.size > 1 && !allowMoreThanOne => throw new IllegalStateException("More than one dependency found of type: " + cl.getName)
        case xs => xs.map(_.invoke(this).asInstanceOf[T])
    }

    private def findByName[T](name: String, cl: Class[T]): Option[T] = this.getClass.getMatchingMethod(name, cl) match {
        case Some(method) => Some(method.invoke(this).asInstanceOf[T])
        case _ => None
    }

    implicit def anythingToCreationHelper[T](obj: T) = new CreationHelper(obj)

    class CreationHelper[T](obj: T) {

        def ~(fn: T => Unit) = pass(fn)

        def pass(fn: T => Unit): T = {
            fn(obj)
            obj
        }
    }
}