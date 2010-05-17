package org.am.scaldi.lift

import net.liftweb.http.{RulesSeq, DispatchSnippet, LiftRules}
import snippet.WrappedReflectiveSnippet
import org.angelsmasterpiece.scala.essentials.reflection.ReflectionHelper._

/**
 * 
 * @author Oleg Ilyenko
 */
object ScaldiRules {
    val managedSnippets = new ManagedSnippetsRulesSeq()

    def registerAtLiftRules() {
        def matchCriteria(name: String)(ns: NamedSnippet) = ns.names.exists(_ == name)

        LiftRules.snippetDispatch.append {
            case name if managedSnippets.ruleSeq.toList.exists(matchCriteria(name)) =>
                managedSnippets.ruleSeq.toList.find(matchCriteria(name)).get match {
                    case NamedSnippet(_, snippet: DispatchSnippet) => snippet
                    case NamedSnippet(_, snippet) => new WrappedReflectiveSnippet(snippet)
                }
        }
    }

    registerAtLiftRules()

    case class NamedSnippet(val names: List[String], val snippet: Object)

    class ManagedSnippetsRulesSeq {
        val ruleSeq = RulesSeq[NamedSnippet]

        def append(snippets: NamedSnippet*): Unit = snippets.foreach(ruleSeq.append)
        def prepend(snippets: NamedSnippet*): Unit = snippets.foreach(ruleSeq.prepend)
    }

    implicit def objectToNamedSnippet(snippet: Object) = new NamedSnippet(snippet.getClass.allClassNames, snippet)
    implicit def stringToNamedSnippetHelper(name: String) = new NamedSnippetHelper(name)

    class NamedSnippetHelper(name: String) {
        def ~>(snippet: Object) = NamedSnippet(List(name), snippet)
    }
}