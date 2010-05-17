package org.am.scaldi.core.example

import module.ApplicationModule
import service.UserService

/**
 * 
 * @author Oleg Ilyenko
 */
object Main {
    def main(args: Array[String]) {
        val module = new ApplicationModule(args)

        println("------------------------ Farm lunch")
        module.farm.makeLunch()

        println("------------------------ Users")
        println(module.userService.getUserByLogin("admin"))
        println(module.byName[UserService]("userService").getUserByLogin("admin"))
        println(module.anotherUserService.getUserByLogin("admin"))

        println("------------------------ Articles")
        println(module.articleService.getAllArticles())
    }
}