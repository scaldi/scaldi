package org.am.scaldi.core.example.service

import java.lang.String
import java.awt.Color
import org.am.scaldi.core.{Provider1, Provider}
import org.am.scaldi.core.example.dao.{ArticleDao, UserDao}
import org.am.scaldi.core.example.model.{Article, Comment, Apple, User}

/**
 * 
 * @author Oleg Ilyenko
 */
trait UserService {
    def getUserByLogin(login: String): User
}

class UserServiceImpl(val userDao: UserDao) extends UserService {
    def getUserByLogin(login: String) = userDao.getUserByLogin(login) match {
        case Some(user) => user
        case _ => throw new IllegalArgumentException("User not found: " + login)
    }
}

trait ArticleService {
    def getAllArticles(): List[Article]
    def getArticleById(id: Long): Article
}

class ArticleServiceImpl(val articleDao: ArticleDao) extends ArticleService {
    def getAllArticles() = articleDao.getAllArticles().zipWithIndex.map { case (article, idx) =>
        article.copy(comments = List(Comment("Commnet for article " + idx, article.author))) // just fill some dummy comments (ideally should be taken from non-existing-yet CommentDao)
    }

    def getArticleById(id: Long) = articleDao.getArticleById(id) match {
        case Some(article) => article
        case _ => throw new IllegalArgumentException("Article not found: " + id)
    }
}

trait AppleEater {
    def eatSomeApple(): Unit
}

class Granny(appleProvider: Provider[Apple]) extends AppleEater {
    def eatSomeApple() {
        println("Granny eating apple");
        appleProvider().eat()
    }
}

class FastidiousGranny(belovedColor: Color, appleProvider: Provider1[Apple, Color]) extends AppleEater {
    def eatSomeApple() {
        println("Fastidious granny eating some special apple"); 
        appleProvider(belovedColor).eat()
    }
}

trait Farm {
    def makeLunch(): Unit
}

class AppleFarm(appleEaters: List[AppleEater]) extends Farm {
    def makeLunch() = for (i <- 1 to 5; eater <- appleEaters) eater.eatSomeApple()
}