package org.am.scaldi.lift.snippet

import xml.NodeSeq
import net.liftweb.http.DispatchSnippet
import org.angelsmasterpiece.scala.essentials.reflection.ReflectionHelper._

/**
 * 
 * @author Oleg Ilyenko
 */
class WrappedReflectiveSnippet(snippet: Object) extends DispatchSnippet {
    def dispatch = {
        case name if snippet.getClass.getMatchingMethod(name, classOf[NodeSeq], classOf[NodeSeq]).isDefined =>
            val method = snippet.getClass.getMatchingMethod(name, classOf[NodeSeq], classOf[NodeSeq]).get
            (ns: NodeSeq) => method.invoke(snippet, ns).asInstanceOf[NodeSeq]
        case name if snippet.getValValue[Render](name).isDefined =>
            snippet.getValValue[Render](name).get.render _
    }
}