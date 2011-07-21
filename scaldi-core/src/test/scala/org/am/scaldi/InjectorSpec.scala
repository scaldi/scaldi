package org.am.scaldi

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import java.util.Properties

class InjectorSpec extends WordSpec with ShouldMatchers {
  "Injector" should {
    "compose with other injectors and the injecttors that come first should override bindings of later injectors" in {
      import org.am.scaldi.Injectable._

      {
        implicit val injector = new Test2Module ++ new Test1Module ++ new AppModule
        inject [Server] should be === HttpServer("test2", 8080)
      }

      {
        implicit val injector = new Test2Module compose new Test1Module compose new AppModule
        inject [Server] should be === HttpServer("test2", 8080)
      }

      {
        implicit val injector = new Test2Module :: new Test1Module :: new AppModule
        inject [Server] should be === HttpServer("test2", 8080)
      }
    }
  }

  "StaticModule" should {
    "provide implicit inlector and be Injectable" in {
      implicit val module = new StaticModule {
        lazy val server = new TcpServer
        lazy val otherServer = HttpServer(inject [String] ("httpHost"), inject [Int] ('httpPort))

        val tcpHost = "tcp-test"
        val tcpPort = 1234

        val httpHost = "localhost"
        val httpPort = 4321
      }

      val server = Injectable.inject [TcpServer]

      server.host should be === "tcp-test"
      server.port should be === 1234
      server.getConnection.welcomeMessage should be === "Hi"

      Injectable.inject [HttpServer] should be === HttpServer("localhost", 4321)
    }
  }

  "DynamicModule" should {
    "provide implicit inlector and be Injectable" in {
      implicit val module = new DynamicModule {
        binding to new TcpServer
        binding to HttpServer(inject [String] ("httpHost"), inject [Int] ('httpPort))

        bind [String] as 'tcpHost to "tcp-test"
        bind [Int] as 'tcpPort to 1234

        binding identifiedBy 'httpHost to "localhost"
        binding identifiedBy 'httpPort to 4321
      }

      val server = Injectable.inject [TcpServer]

      server.host should be === "tcp-test"
      server.port should be === 1234
      server.getConnection.welcomeMessage should be === "Hi"

      Injectable.inject [HttpServer] should be === HttpServer("localhost", 4321)
    }
  }

  "SystemPropertiesInjector" should {
    "look for simple bindings in system properties and convert them to required type" in {
      implicit val injector = SystemPropertiesInjector :: new AppModule
      import org.am.scaldi.Injectable._

      try {
        System.setProperty("host", "test-sys")
        System.setProperty("port", "12345")

        inject[Server] should be === HttpServer("test-sys", 12345)
      } finally {
        System.setProperty("host", "")
        System.setProperty("port", "")
      }
    }
  }

  "PropertiesInjector" should {
    "look for simple bindings in system properties and convert them to required type" in {
      val props = new  Properties()

      props.setProperty("host", "test-prop")
      props.setProperty("port", "54321")

      implicit val injector = PropertiesInjector(props) :: new AppModule

      Injectable.inject[Server] should be === HttpServer("test-prop", 54321)
    }
  }

  class AppModule extends Module {
    bind [Server] to HttpServer(inject [String] ('host), inject [Int] ('port))

    binding identifiedBy 'host to "localhost"
    binding identifiedBy 'port to 80
  }

  class Test1Module extends Module {
    binding identifiedBy 'host to "test"
  }

  class Test2Module extends Module {
    binding identifiedBy 'host to "test2"
    binding identifiedBy 'port to 8080
  }
}