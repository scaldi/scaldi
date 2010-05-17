package org.am.scaldi.lift.examples.snippet

import org.am.scaldi.core.example.service.ArticleService
import xml.NodeSeq
import net.liftweb.util.Helpers._
import org.am.scaldi.lift.snippet.Render
import org.am.scaldi.core.Provider
import net.liftweb.common.{Full, Box}

/**
 * 
 * @author Oleg Ilyenko
 */
abstract class Articles(articleService: ArticleService, articleUrl: Long => String) {
    def list(html: NodeSeq) =
        <xml:group>{
            articleService.getAllArticles.map { a =>
                bind("article", html,
                    "title" -> a.title,
                    "text" -> a.text,
                    "author" -> (a.author.firstName + " " + a.author.lastName),
                    AttrBindParam("url", articleUrl(a.id), "href"))
            }
        }</xml:group>

    def info: Info

    trait Info extends Render {
        val articleId: Provider[Box[String]]

        def render(html: NodeSeq) = articleId() match {
            case Full(id) =>
                val a = articleService.getArticleById(id.toLong)
                bind("article", html,
                    "title" -> a.title,
                    "text" -> a.text,
                    "author" -> (a.author.firstName + " " + a.author.lastName))
            case _ => Nil
        }
    }
}