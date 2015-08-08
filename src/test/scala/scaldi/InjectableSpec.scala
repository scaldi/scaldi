package scaldi

import org.scalatest.{Matchers, WordSpec}
import java.text.DateFormat

class InjectableSpec extends WordSpec with Matchers {

  "Injectable" should {

    "be useable within classes that are instantiated within module and have implicit injector" in {
      val module = new TcpModule :: DynamicModule({ m =>
        m.binding identifiedBy 'tcpHost to "test"
        m.binding identifiedBy 'welcome to "Hello user!"
      })

      val binding = module.getBinding(List('tcpServer))
      binding should be ('defined)

      val server = binding.get.get.get.asInstanceOf[TcpServer]
      server.port should equal (1234)
      server.host should equal ("test")
      server.getConnection.welcomeMessage should equal ("Hello user!")
    }

    "treat binding that return None as non-defined and use default or throw an exception if no default provided" in {
      val module = new TcpModule :: DynamicModule(_.bind [String] identifiedBy 'welcome to None) :: DynamicModule({ m =>
        m.binding identifiedBy 'tcpHost to "test"
        m.binding identifiedBy 'welcome to "Hello user!"
      })

      val binding = module.getBinding(List('tcpServer))
      binding should be ('defined)

      val server = binding.get.get.get.asInstanceOf[TcpServer]
      server.getConnection.welcomeMessage should equal ("Hi")
    }

    import scaldi.Injectable._
    val defaultDb = PostgresqlDatabase("default_db")

    "inject by type" in {
      inject [Database] should equal (MysqlDatabase("my_app"))
      inject [Database] (classOf[ConnectionProvider]) should equal (MysqlDatabase("my_app"))
    }

    "inject using identifiers" in {
      val results = List (
        inject [Database] ('database and 'local),
        inject [Database] (identified by 'local and 'database),
        inject [Database] (by default defaultDb and identified by 'database and 'local),
        inject [Database] (by default new MysqlDatabase("my_app") and 'database and 'local),
        inject [Database] ('database and "local" and by default defaultDb)
      )

      results should have size 5
      results.distinct should (contain(MysqlDatabase("my_app"): Database) and have size (1))
    }

    "inject default if binding not found" in {
      val results = List [Database] (
        inject [Database] (identified by 'remote and by default new PostgresqlDatabase("default_db")),
        inject [Database] (identified by 'remote is by default defaultDb),
        inject [Database] ('remote is by default defaultDb),
        inject [Database] ('remote which by default defaultDb),
        inject [Database] ('remote that by default defaultDb),
        inject [Database] (by default defaultDb and identified by 'remote),
        inject (by default defaultDb),
        inject (by default defaultDb and 'local),
        inject (by default new PostgresqlDatabase("default_db"))
      )

      results should have size 9
      results.distinct should (contain(defaultDb: Database) and have size 1)
    }

    "correctly inject provider" in {
      var str1Counter = 0
      var str2Counter = 0

      implicit val injector = DynamicModule({ m =>
        m.binding identifiedBy 'str1 to {
          str1Counter = str1Counter + 1
          s"str1 $str1Counter"
        }

        m.binding identifiedBy 'str2 toProvider {
          str2Counter = str2Counter + 1
          s"str2 $str2Counter"
        }
      })

      val str1 = injectProvider[String]('str1)
      val str2 = injectProvider[String]('str2)

      str1() should equal ("str1 1")
      str1() should equal ("str1 1")

      str2() should equal ("str2 1")
      str2() should equal ("str2 2")

      str1Counter should equal (1)
      str2Counter should equal (2)
    }

    "throw exception if no default provided and bonding not found" in {
      an [InjectException] should be thrownBy inject [DateFormat]
    }

    "also be available in module, but use resulting (compised) injector" in {
      val server = inject [Server] ('real and 'http)
      server should equal (HttpServer("marketing.org", 8081))
    }

    "distinguish generic types" in {
      val intAdder = inject [(Int, Int) => Int]
      intAdder(2, 3) should equal (5)

      val stringAdder = inject [(String, String) => String]
      stringAdder("Hello", "World") should equal ("Hello, World")
    }

    "inject all using type parameter" in {
      injectAllOfType [String] ('host) should
          (contain("www.google.com") and contain("www.yahoo.com") and contain("www.github.com") and have size 3)

      injectAllOfType [HttpServer] should
          (contain(HttpServer("localhost", 80)) and contain(HttpServer("test", 8080)) and have size 2)
    }

    "inject all without type parameter" in {
      injectAll('host).asInstanceOf[List[String]] should
          (contain("www.google.com") and contain("www.yahoo.com") and contain("www.github.com") and have size 3)

      injectAll('server).asInstanceOf[List[HttpServer]] should
          (contain(HttpServer("localhost", 80)) and contain(HttpServer("test", 8080)) and have size 2)
    }
  }

  implicit lazy val injector: Injector = mainModule :: marketingModule

  val marketingModule = new Module {
    bind [String] identifiedBy 'httpHost to "marketing.org"
  }

  val mainModule = new Module {
    binding identifiedBy 'host and 'google to "www.google.com"
    binding identifiedBy 'host and 'yahoo to "www.yahoo.com"
    binding identifiedBy 'host and 'github to "www.github.com"

    binding identifiedBy 'server to HttpServer("localhost", 80)
    binding identifiedBy 'server to None
    binding identifiedBy 'server to HttpServer("test", 8080)

    binding identifiedBy 'intAdder to ((a: Int, b: Int) => a + b)
    binding identifiedBy 'stringAdder to ((s1: String, s2: String) => s1 + ", " + s2)

    bind [Int] identifiedBy 'httpPort to 8081

    bind [Server] identifiedBy 'real and 'http to HttpServer(inject [String] ('httpHost), inject [Int] ('httpPort))

    binding identifiedBy 'database and "local" to MysqlDatabase("my_app")
  }
}