package scaldi

import Injectable._
import java.io.File
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import com.typesafe.config.{Config, ConfigList, ConfigObject, ConfigValue}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** @author
  *   Oleg Ilyenko
  */
class TypesafeConfigInjectorSpec extends AnyWordSpec with Matchers {
  "TypesafeConfigInjector" should {
    implicit val inj = TypesafeConfigInjector()

    "inject numbers" in {
      inject[Int]("numbersTest.port") should equal(123)
      inject[Long]("numbersTest.port") should equal(123L)
      inject[List[Int]]("numbersTest.listOfNumbers") should equal(List(5, 6, 7))
      inject[List[Double]]("numbersTest.some.doubles") should equal(List(5.55, 6.76))
    }

    "inject bools" in {
      inject[Boolean]("bools.enabled") should equal(true)
      inject[List[Boolean]]("bools.mess") should equal(List(true, false, false, true, false))
    }

    "inject files" in {
      inject[File]("other.someFile") should equal(new File("/var/log/foo.log"))
      inject[List[File]]("other.someFiles") should equal(
        List(new File("/var/log/foo1.log"), new File("/var/log/foo2.log"))
      )
    }

    "inject durations" in {
      inject[Duration]("other.timeout") should equal(Duration(10, TimeUnit.SECONDS))
      inject[List[Duration]]("other.timeouts") should equal(
        List(Duration(1, TimeUnit.SECONDS), Duration(2000, TimeUnit.MILLISECONDS))
      )
    }

    "inject strings" in {
      inject[String]("other.boringString") should equal("abc")
      inject[List[String]]("other.someStrings") should equal(List("a", "123", "f"))
    }

    "inject config" in {
      val config = inject[Config]("numbersTest")

      config.getInt("port") should equal(123)
    }

    "inject config lists" in {
      val configs = inject[List[Config]]("complexList")

      configs should have size 2

      configs(0).getString("foo") should equal("aa")
      configs(1).getString("foo") should equal("bb")
    }

    "inject config value" in {
      inject[ConfigValue]("numbersTest.port").render should equal("123")
    }

    "inject ConfigList" in {
      val list = inject[ConfigList]("numbersTest.listOfNumbers")

      list should have size 3

      list.get(0).render should equal("5")
      list.get(1).render should equal("6")
      list.get(2).render should equal("7")
    }

    "inject config object" in {
      inject[ConfigObject]("bools").toConfig.getBoolean("enabled") should equal(true)
    }

    "inject config object list" in {
      val list = inject[List[ConfigObject]]("complexList")

      list should have size 2

      list(0).toConfig.getString("foo") should equal("aa")
      list(1).toConfig.getString("foo") should equal("bb")
    }
  }
}
