package org.am.scaldi.lift.examples

import org.am.scaldi.lift.module.LiftModule
import org.am.scaldi.core.example.module._
import org.am.scaldi.lift.provider.Param
import org.angelsmasterpiece.scala.essentials.config.SystemConfigurationSource
import snippet.{Articles, Users}
import sun.net.www.content.text.plain

/**
 * 
 * @author Oleg Ilyenko
 */
class WebModule extends LiftModule[WebModule] with DaoModule with ServiceModule with FarmModule with ConfigurationModule {
    m =>

    lazy val configurationSource = new SystemConfigurationSource 

    lazy val userLoginParam = Param("login")
    lazy val articleIdParam = Param("articleId")

    snippet ~> new Articles(articleService, id => "/articles?" + articleIdParam + "=" + id) {
        val info = new Info {
            val articleId = articleIdParam
        }
    }
    
    lazy val users = snippet ~> new Users {
        val userLogin = userLoginParam
        val userService = m.userService
    }

    lazy val usersTestNamed = snippet("UsersNamed") ~> new Users {
        val userLogin = userLoginParam
        val userService = m.userService
    }

    snippet("TopLevelUsers") ~> new Users {
        val userLogin = userLoginParam
        val userService = m.userService
    }
}