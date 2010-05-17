package org.am.scaldi.lift.module

import org.am.scaldi.core.Module

import org.am.scaldi.lift.ScaldiRules._
import net.liftweb.common.{Full, Empty, Box}

/**
 * 
 * @author Oleg Ilyenko
 */
trait LiftModule[T] extends Module with SnippetHelper {

    def registerToLiftRules(): T  = {
        registerSnippets()

        selfReference
    }

    def selfReference: T = this.asInstanceOf[T]
}

trait SnippetHelper {
    self: Module =>

    private var allDeclaredSnippets: List[NamedSnippet] = Nil

    protected def registerSnippets() {
        touchDeclaredSnippets()
        managedSnippets.append(allDeclaredSnippets: _*)
    }

    private def touchDeclaredSnippets(): List[NamedSnippet] = allByType[NamedSnippet]

    protected def snippet = new PrefixSnippetBuilder(None)

    protected def snippet(name: String) = new PrefixSnippetBuilder(Some(name))

    class PrefixSnippetBuilder(name: Option[String]) {
        private def addToSnippets(s: NamedSnippet) = {
            allDeclaredSnippets ::= s
            s
        }

        def ~>(obj: Object): NamedSnippet = name match {
            case Some(n) => addToSnippets(n ~> obj)
            case None => addToSnippets(obj)
        }
    }
}