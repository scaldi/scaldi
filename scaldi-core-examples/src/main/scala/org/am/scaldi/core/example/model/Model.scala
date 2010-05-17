package org.am.scaldi.core.example.model

import java.awt.Color

/**
 * 
 * @author Oleg Ilyenko
 */
case class User(val login: String, val firstName: String, val lastName: String)

case class Article(val id: Long, val title: String, text:String, author: User, comments: List[Comment] = Nil)

case class Comment(val text: String, author: User, inReplayTo: Option[Comment] = None)

case class Apple(color: Color) {
    private var eaten: Boolean = false

    def eat(): Unit =
        if (!eaten) {
            println("Eating apple of color: " + color)
            eaten = true
        } else throw new IllegalStateException("You can't eat already eaten apple!!!")
}