package bootstrap.liftweb

import _root_.net.liftweb.common._
import _root_.net.liftweb.util._
import _root_.net.liftweb.http._
import _root_.net.liftweb.sitemap._
import java.lang.String

import org.am.scaldi.lift.examples.WebModule
import org.am.scaldi.lift.ScaldiRules

import Helpers._
import org.am.scaldi.lift.examples.snippet.Users


class Boot {

    def boot {
        LiftRules.addToPackages("org.am.scaldi.lift.examples")

        LiftRules.setSiteMap(SiteMap(
            Menu("Home") / "index",
            Menu("Users") / "users",
            Menu("Articles") / "articles"
        ))

        // Instantiate main module
        val module = new WebModule() registerToLiftRules

        manualSnippetsDeclaration(module)
    }

    def manualSnippetsDeclaration(module: WebModule) {
        import ScaldiRules._

        managedSnippets.append(
            "ManualUsers" ~> new Users {
                val userService = module.userService
                val userLogin = module.userLoginParam
            }
        )
    }

}

