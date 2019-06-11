package scaldi

trait Server
case class HttpServer(host: String, port: Int) extends Server
class LifecycleServer extends Server {
  var initializedCount = 0
  var destroyedCount = 0

  def init() =
    initializedCount = initializedCount + 1

  def terminate() =
    destroyedCount = destroyedCount + 1
}

trait Database
trait ConnectionProvider
case class MysqlDatabase(name: String) extends Database with ConnectionProvider
case class PostgresqlDatabase(name: String) extends Database with ConnectionProvider

class TcpModule extends Module {
  binding identifiedBy Symbol("tcpServer") to new TcpServer
}

class TcpServer(implicit inj: Injector) extends Server with Injectable  {
  val port = inject [Int] (Symbol("tcpPort") is by default 1234)
  val host = inject [String] (Symbol("tcpHost"))

  def getConnection = new TcpConnection
}

import scaldi.Injectable._

class TcpConnection(implicit inj: Injector) {
  val welcomeMessage = inject [String] (Symbol("welcome") is by default "Hi")
}

case class Dep1(name: String)
case class Dep2(name: String)
case class Dep3(name: String)