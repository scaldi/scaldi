package org.am.scaldi.core.example.module

import org.am.scaldi.core.Module
import org.angelsmasterpiece.scala.essentials.option.{Argument, FlagArgument, CommandLine}
import org.angelsmasterpiece.scala.essentials.config.{SystemConfigurationSource, CommandLineConfigurationSource, AggregatingConfigurationSource}

/**
 * 
 * @author Oleg Ilyenko
 */
class ApplicationModule(args: Array[String]) extends Module with DaoModule with ServiceModule with FarmModule with ConfigurationModule {

    lazy val configurationSource = AggregatingConfigurationSource(CommandLineConfigurationSource(commandLine), new SystemConfigurationSource)

    lazy val commandLine = new CommandLine {

        val help = FlagArgument("help", "h", description = "Show this help")
        val databaseHost = Argument[String]("databaseHost", "host", optional = true)
        val databasePort = Argument[Int]("databasePort", "port", optional = true)

    } ~ (_ parseOrExit args)
}