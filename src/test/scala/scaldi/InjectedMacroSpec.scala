package scaldi

import org.scalatest.{Matchers, WordSpec}
import Injectable.inject
import java.util.Date

class InjectedMacroSpec extends WordSpec with Matchers {
  "`injected` macro" should {
    "support basic injection" in {
      implicit val inj = new Module {
        bind [DepSimple] to injected [DepSimple]

        binding to "Test"
        bind [Server] to new HttpServer("localhost", 80)
      }

      inject [DepSimple] should equal (new DepSimple("Test", new HttpServer("localhost", 80) ))
    }

    "support multiple argument lists" in {
      implicit val inj = new Module {
        bind [DepMultiArgList] to injected [DepMultiArgList]

        binding to "Test"
        bind [Server] to new HttpServer("localhost", 80)
        bind [Long] to 100L
        bind [List[Int]] to List(1, 2, 3)
      }

      inject [DepMultiArgList] should equal (new DepMultiArgList("Test", new HttpServer("localhost", 80))(100L)(100L, List(1, 2, 3)))
    }

    "support default arguments in the first argument list" in {
      implicit val inj = new Module {
        bind [DepWithDefaults] to injected [DepWithDefaults]

        bind [Server] to new HttpServer("localhost", 80)
      }

      inject [DepWithDefaults] should equal (new DepWithDefaults(d2 = new HttpServer("localhost", 80)))
    }

    "support overrides" in {
      implicit val inj = new Module {
        bind [DepWithDefaults] to injected [DepWithDefaults] (Symbol("d1") -> Dep1("Override"), Symbol("d3") -> Dep2("Another override"))

        bind [Server] to new HttpServer("localhost", 80)
        binding to Dep1("Defined in module")
      }

      inject [DepWithDefaults] should equal (new DepWithDefaults(Dep1("Override"), new HttpServer("localhost", 80), Dep2("Another override")))
    }

    "inject implicit injector" in {
      implicit val inj = new ImplicitArgumentsModule

      inject [ImplicitArguments] should equal (new ImplicitArguments(dep1 = Dep1("foo"), Dep2("foo"), Dep3("foo")))
    }
  }
}

class ImplicitArgumentsModule extends Module {
  bind to injected [ImplicitArguments] (Symbol("dep1") -> inject [Dep1] (identified by Symbol("foo")))

  bind [Dep1] identifiedBy Symbol("foo") to Dep1("foo")
  bind [Dep1] identifiedBy Symbol("bar") to Dep1("bar")

  binding to Dep2("foo")
  binding to Dep3("foo")
}

case class ImplicitArguments(dep1: Dep1, dep2: Dep2, dep3: Dep3)(implicit inj: Injector)

class DepSimple(a: String, s: Server) extends Debug(Symbol("a") -> a, Symbol("s") -> s)
class DepWithDefaults(d1: Dep1 = Dep1("Default Value"), d2: Server, d3: Dep2 = Dep2("123")) extends Debug(Symbol("d1") -> d1, Symbol("d2") -> d2, Symbol("d3") -> d3)
class DepMultiArgList(a: String, s: Server)(l: Long)(l1: Long, c: List[Int]) extends Debug(Symbol("a") -> a, Symbol("s") -> s, Symbol("l") -> l, Symbol("l1") -> l1, Symbol("c") -> c)

class Debug(val props: (Symbol, Any)*) {
  // hashCode is not overridden because I will use only equals in the test
  override def equals(obj: scala.Any) =
    obj.getClass == this.getClass && obj.asInstanceOf[Debug].props == this.props

  override def toString =
    props.toString()
}
