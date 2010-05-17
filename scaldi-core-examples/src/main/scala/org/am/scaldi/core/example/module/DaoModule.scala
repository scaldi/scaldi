package org.am.scaldi.core.example.module

import org.am.scaldi.core.example.model.User
import org.am.scaldi.core.example.dao.{DatabaseArticleDao, UserDaoImpl}
import org.am.scaldi.core.Module
import org.angelsmasterpiece.scala.essentials.config.Configuration


/**
 * 
 * @author Oleg Ilyenko
 */
trait DaoModule extends Module {
    self: ConfigurationModule =>

    lazy val dummyUserList = List(User("admin", "System", "Administrator"))

    lazy val userDao = new UserDaoImpl(dummyUserList)

    lazy val articleDao = new DatabaseArticleDao(
        driverClassName = config.database.driver(),
        host = config.database.host.is,
        port = config.database.port()
    )
}