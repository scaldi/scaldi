package org.am.scaldi.core.example.module

import org.am.scaldi.core.Module
import org.am.scala.essentials.option._
import org.am.scala.essentials.config._
import org.am.scala.essentials.helper.Conversions._

/**
 *
 * @author Oleg Ilyenko
 */
trait ConfigurationModule extends Module {

    val configurationSource: ConfigurationSource  

    lazy val config = new Configuration {
        implicit val implicitConfigurationSource = configurationSource


        val appName = Property[String]("appName", "This application name", default = "Scaldi Examples")

        val database = new Configuration {
            val driver = Property[String]("databaseDriverName", default = "com.mysql.jdbc.Driver")
            val host = Property[String]("databaseHost", default = "localhost")
            val port = Property[Int]("databasePort", default = 3306)
        }

    } ~ (_ validate)
}