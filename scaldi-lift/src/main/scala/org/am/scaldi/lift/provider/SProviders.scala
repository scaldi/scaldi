package org.am.scaldi.lift.provider

import net.liftweb.common.Box
import net.liftweb.http.S
import org.am.scaldi.core.Provider

/**
 * 
 * @author Oleg Ilyenko
 */
case class Param(name: String) extends Provider[Box[String]] {
    val providerFn = () => S.param(name)
    override def toString = name
}

case class Params(name: String) extends Provider[List[String]] {
    val providerFn = () => S.params(name)
    override def toString = name
}