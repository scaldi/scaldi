package org.am.scaldi.lift.snippet

import xml.NodeSeq

/**
 * 
 * @author Oleg Ilyenko
 */
trait Render {
    def render(html: NodeSeq): NodeSeq
} 