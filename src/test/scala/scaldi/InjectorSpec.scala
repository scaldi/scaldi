package scaldi

import org.scalatest.{Matchers, WordSpec}
import java.util.Properties

class InjectorSpec extends WordSpec with Matchers {
  "Injector composition" should {
    "produce real injector when composed with NilInjector or produce NilInjector when both of them are NilInjectors" in {
      val realModule = new Test2Module

      NilInjector :: NilInjector should be theSameInstanceAs (NilInjector)
      realModule :: NilInjector should be theSameInstanceAs (realModule)
      NilInjector :: realModule should be theSameInstanceAs (realModule)
    }

    "produce mutable aggregation if at least one of the injectors is mutable" in {
      val mutable = new Test2Module
      val immutable = new StaticModule {}

      (mutable ++ mutable).getClass should equal (classOf[MutableInjectorAggregation])
      (mutable ++ immutable).getClass should equal (classOf[MutableInjectorAggregation])
      (immutable ++ mutable).getClass should equal (classOf[MutableInjectorAggregation])

      ((if (true) immutable else NilInjector) :: immutable).getClass should equal (classOf[ImmutableInjectorAggregation])
      ((if (false) immutable else NilInjector) :: immutable).getClass should equal (classOf[ImmutableInjectorAggregation])
      ((if (false) mutable else NilInjector) :: immutable).getClass should equal (classOf[MutableInjectorAggregation])
    }

    "produce Immutable aggreagation if both injectors are not mutable" in {
      val immutable = new StaticModule {}

      (immutable ++ immutable).getClass should equal (classOf[ImmutableInjectorAggregation])
    }

    "compose injectors so, that injectors that come first should override bindings of later injectors" in {
      import scaldi.Injectable._

      {
        implicit val injector = new Test2Module ++ new Test1Module ++ new AppModule
        inject [Server] should equal (HttpServer("test2", 8080))
      }

      {
        implicit val injector = new Test2Module :: new Test1Module :: new AppModule
        inject [Server] should equal (HttpServer("test2", 8080))
      }
    }
  }

  "StaticModule" should {
    "provide implicit injector and be Injectable" in {
      implicit val module = new StaticModule {
        lazy val server = new TcpServer
        lazy val otherServer = HttpServer(inject [String] ("httpHost"), inject [Int] ('httpPort))

        val tcpHost = "tcp-test"
        val tcpPort = 1234

        val httpHost = "localhost"
        val httpPort = 4321
      }

      val server = Injectable.inject [TcpServer]

      server.host should equal ("tcp-test")
      server.port should equal (1234)
      server.getConnection.welcomeMessage should equal ("Hi")

      Injectable.inject [HttpServer] should equal (HttpServer("localhost", 4321))
    }
  }

  "DynamicModule" should {
    "provide implicit injector and be Injectable" in {
      implicit val module = new DynamicModule {
        binding to new TcpServer
        binding to HttpServer(inject [String] ("httpHost"), inject [Int] ('httpPort))

        bind [String] as 'tcpHost to "tcp-test"
        bind [Int] as 'tcpPort to 1234

        binding identifiedBy 'httpHost to "localhost"
        binding identifiedBy 'httpPort to 4321
      }

      val server = Injectable.inject [TcpServer]

      server.host should equal ("tcp-test")
      server.port should equal (1234)
      server.getConnection.welcomeMessage should equal ("Hi")

      Injectable.inject [HttpServer] should equal (HttpServer("localhost", 4321))
    }
  }

  "RawInjector" should {
    "convert simple types" in {
      import scala.concurrent.duration._
      import java.io.File
      val injector = new RawInjector {
        override def getRawValue(name: String): Option[String] = Some(name)
      }
      def blindlyGetBindingFor[T: scala.reflect.runtime.universe.TypeTag](value: String): T = {
        val mbBinding = injector.getBinding(List(TypeTagIdentifier.typeId[T], StringIdentifier(value)))
        mbBinding shouldNot be(None)
        val mbVal = mbBinding.get.get
        mbVal shouldNot be(None)
        mbVal.get.asInstanceOf[T]
      }
      blindlyGetBindingFor[Int]("-1") should be(-1)
      blindlyGetBindingFor[Long]("987654321") should be(987654321L)
      blindlyGetBindingFor[Duration]("10 seconds") should be(10 seconds)
      blindlyGetBindingFor[File](".") should equal(new File("."))
    }
  }

  "SystemPropertiesInjector" should {
    "look for simple bindings in system properties and convert them to required type" in {
      implicit val injector = SystemPropertiesInjector :: new AppModule
      import scaldi.Injectable._

      try {
        System.setProperty("host", "test-sys")
        System.setProperty("port", "12345")

        inject[Server] should equal (HttpServer("test-sys", 12345))
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

      Injectable.inject[Server] should equal (HttpServer("test-prop", 54321))
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