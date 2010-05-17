package org.am.scaldi.lift.examples.snippet

import org.am.scaldi.core.Provider
import xml.NodeSeq
import org.am.scaldi.core.example.service.{UserService, ArticleService}
import net.liftweb.util.Helpers._
import net.liftweb.common.Box

/**
 * 
 * @author Oleg Ilyenko
 */
trait Users {

    val userLogin: Provider[Box[String]]

    val userService: UserService

    def userInfo(html: NodeSeq): NodeSeq = tryo {
        val user = userService.getUserByLogin(userLogin() openOr "admin")
        bind("user", html, "login" -> user.login, "fullName" -> (user.firstName + " " + user.lastName))
    } openOr {
        <b style="color: red">User with login <i>{userLogin() openOr "None"}</i> not found!</b>
    }
}