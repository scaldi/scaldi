package scaldi

import java.text.DateFormat
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InjectableSpec extends AnyWordSpec with Matchers {

  "Injectable" should {

    "be useable within classes that are instantiated within module and have implicit injector" in {
      val module = new TcpModule :: DynamicModule { m =>
        m.binding identifiedBy Symbol("tcpHost") to "test"
        m.binding identifiedBy Symbol("welcome") to "Hello user!"
      }

      val binding = module.getBinding(List(Symbol("tcpServer")))
      binding should be(Symbol("defined"))

      val server = binding.get.get.get.asInstanceOf[TcpServer]
      server.port should equal(1234)
      server.host should equal("test")
      server.getConnection.welcomeMessage should equal("Hello user!")
    }

    "treat binding that return None as non-defined and use default or throw an exception if no default provided" in {
      val module =
        new TcpModule :: DynamicModule(_.bind[String] identifiedBy Symbol("welcome") to None) :: DynamicModule { m =>
          m.binding identifiedBy Symbol("tcpHost") to "test"
          m.binding identifiedBy Symbol("welcome") to "Hello user!"
        }

      val binding = module.getBinding(List(Symbol("tcpServer")))
      binding should be(Symbol("defined"))

      val server = binding.get.get.get.asInstanceOf[TcpServer]
      server.getConnection.welcomeMessage should equal("Hi")
    }

    import scaldi.Injectable._
    val defaultDb = PostgresqlDatabase("default_db")

    "inject by type" in {
      inject[Database] should equal(MysqlDatabase("my_app"))
      inject[Database](classOf[ConnectionProvider]) should equal(MysqlDatabase("my_app"))
    }

    "inject using identifiers" in {
      val results = List(
        inject[Database](Symbol("database") and Symbol("local")),
        inject[Database](identified by Symbol("local") and Symbol("database")),
        inject[Database](by default defaultDb and identified by Symbol("database") and Symbol("local")),
        inject[Database](by default new MysqlDatabase("my_app") and Symbol("database") and Symbol("local")),
        inject[Database](Symbol("database") and "local" and by default defaultDb)
      )

      results should have size 5
      results.distinct should (contain(MysqlDatabase("my_app"): Database) and have size 1)
    }

    "inject default if binding not found" in {
      val results = List[Database](
        inject[Database](identified by Symbol("remote") and by default new PostgresqlDatabase("default_db")),
        inject[Database](identified by Symbol("remote") is by default defaultDb),
        inject[Database](Symbol("remote") is by default defaultDb),
        inject[Database](Symbol("remote") which by default defaultDb),
        inject[Database](Symbol("remote") that by default defaultDb),
        inject[Database](by default defaultDb and identified by Symbol("remote")),
        inject(by default defaultDb),
        inject(by default defaultDb and Symbol("local")),
        inject(by default new PostgresqlDatabase("default_db"))
      )

      results should have size 9
      results.distinct should (contain(defaultDb: Database) and have size 1)
    }

    "correctly inject provider" in {
      var str1Counter = 0
      var str2Counter = 0

      implicit val injector = DynamicModule { m =>
        m.binding identifiedBy Symbol("str1") to {
          str1Counter = str1Counter + 1
          s"str1 $str1Counter"
        }

        m.binding identifiedBy Symbol("str2") toProvider {
          str2Counter = str2Counter + 1
          s"str2 $str2Counter"
        }
      }

      val str1 = injectProvider[String](Symbol("str1"))
      val str2 = injectProvider[String](Symbol("str2"))

      str1() should equal("str1 1")
      str1() should equal("str1 1")

      str2() should equal("str2 1")
      str2() should equal("str2 2")

      str1Counter should equal(1)
      str2Counter should equal(2)
    }

    "throw exception if no default provided and bonding not found" in {
      an[InjectException] should be thrownBy inject[DateFormat]
    }

    "also be available in module, but use resulting (compised) injector" in {
      val server = inject[Server](Symbol("real") and Symbol("http"))
      server should equal(HttpServer("marketing.org", 8081))
    }

    "distinguish generic types" in {
      val intAdder = inject[(Int, Int) => Int]
      intAdder(2, 3) should equal(5)

      val stringAdder = inject[(String, String) => String]
      stringAdder("Hello", "World") should equal("Hello, World")
    }

    "inject all using type parameter" in {
      injectAllOfType[String](Symbol("host")) should
        (contain("www.google.com") and contain("www.yahoo.com") and contain("www.github.com") and have size 3)

      injectAllOfType[HttpServer] should
        (contain(HttpServer("localhost", 80)) and contain(HttpServer("test", 8080)) and have size 2)
    }

    "inject all without type parameter" in {
      injectAll(Symbol("host")).asInstanceOf[List[String]] should
        (contain("www.google.com") and contain("www.yahoo.com") and contain("www.github.com") and have size 3)

      injectAll(Symbol("server")).asInstanceOf[List[HttpServer]] should
        (contain(HttpServer("localhost", 80)) and contain(HttpServer("test", 8080)) and have size 2)
    }

    "inject none if a binding is not found or ambiguous" in {
      injectOpt[Injector] should equal(None)
      injectOpt[String] should equal(None)
    }

    "inject some values identified by names" in {
      injectOpt[String](Symbol("httpHost")) should equal(Some("marketing.org"))
      injectOpt[String](Symbol("host") and Symbol("github")) should equal(Some("www.github.com"))
      injectOpt[Database] should be(Symbol("defined"))
    }

    "inject optional default values" in {
      injectOpt[String](Symbol("nonSpecified") is by default "default value") should equal(Some("default value"))
    }
  }

  implicit lazy val injector: Injector = mainModule :: marketingModule

  val marketingModule = new Module {
    bind[String] identifiedBy Symbol("httpHost") to "marketing.org"
  }

  val mainModule = new Module {
    binding identifiedBy Symbol("host") and Symbol("google") to "www.google.com"
    binding identifiedBy Symbol("host") and Symbol("yahoo") to "www.yahoo.com"
    binding identifiedBy Symbol("host") and Symbol("github") to "www.github.com"

    binding identifiedBy Symbol("server") to HttpServer("localhost", 80)
    binding identifiedBy Symbol("server") to None
    binding identifiedBy Symbol("server") to HttpServer("test", 8080)

    binding identifiedBy Symbol("intAdder") to ((a: Int, b: Int) => a + b)
    binding identifiedBy Symbol("stringAdder") to ((s1: String, s2: String) => s1 + ", " + s2)

    bind[Int] identifiedBy Symbol("httpPort") to 8081

    bind[Server] identifiedBy Symbol("real") and Symbol("http") to HttpServer(
      inject[String](Symbol("httpHost")),
      inject[Int](Symbol("httpPort"))
    )

    binding identifiedBy Symbol("database") and "local" to MysqlDatabase("my_app")
  }
}
