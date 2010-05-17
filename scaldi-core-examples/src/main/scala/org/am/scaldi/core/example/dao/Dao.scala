package org.am.scaldi.core.example.dao

import java.lang.String
import org.am.scaldi.core.example.model.{Article, User}

/**
 * 
 * @author Oleg Ilyenko
 */
trait UserDao {
    def getUserByLogin(login: String): Option[User]
}

class UserDaoImpl(val userDatabase: List[User]) extends UserDao {
    def getUserByLogin(login: String) = userDatabase.find(_.login == login)
}

trait ArticleDao {
    def getAllArticles(): List[Article]
    def getArticleById(id: Long): Option[Article]
}

class DatabaseArticleDao(driverClassName: String, host: String, port: Int) extends ArticleDao {

    // just dummy list
    private val articles = List(
        Article(1, "Article 1", "One upon a time....", User("some", "Some", "Unimportant user")),
        Article(2, "Article 2", "Some article text", User("some1", "Some", "Another user"))
    )

    def getAllArticles() = {
        println("Loadfing article list from database with class name '" + driverClassName + "' at " + host + ":" + port)
        articles
    }

    def getArticleById(id: Long) = articles.find(_.id == id)
}