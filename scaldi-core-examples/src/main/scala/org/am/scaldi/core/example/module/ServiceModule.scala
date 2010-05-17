package org.am.scaldi.core.example.module

import org.am.scaldi.core.example.dao.UserDao
import org.am.scaldi.core.example.service.{ArticleServiceImpl, UserServiceImpl}
import org.am.scaldi.core.Module

/**
 * 
 * @author Oleg Ilyenko
 */
trait ServiceModule extends Module {
    dao: DaoModule =>

    lazy val userService = new UserServiceImpl(dao.userDao)

    lazy val anotherUserService = new UserServiceImpl(byType[UserDao])

    lazy val articleService = new ArticleServiceImpl(dao.articleDao)
}