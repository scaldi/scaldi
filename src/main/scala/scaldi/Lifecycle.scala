package scaldi

import java.util.concurrent.atomic.AtomicBoolean

import scaldi.util.Util._
import scala.util.{Success, Failure, Try}
import scala.util.control.Breaks._

case class BindingLifecycle[-T](
  initialize: Option[T => Unit] = None,
  destroy: Option[T => Unit] = None
) {
  def initializeObject(obj: T): Unit = obj <| (o => initialize foreach (_(o)))
  def destroyObject(obj: T): Unit = obj <| (o => destroy foreach (_(o)))
}

object BindingLifecycle {
  def empty[T] = BindingLifecycle[T]()
}

trait LifecycleManager {
  val IgnoringErrorHandler =
    (e: Throwable) => {e.printStackTrace(); true}

  def addDestroyable(fn: () => Unit): Unit

  def destroy(errorHandler: Throwable => Boolean = IgnoringErrorHandler): Unit
}

trait ShutdownHookLifecycleManager extends LifecycleManager {
  private var toDestroy: List[() => Unit] = Nil
  private val destroyed = new AtomicBoolean(false)
  private var hookThread: Option[Thread] = None

  def addDestroyable(fn: () => Unit) = this.synchronized {
    if (destroyed.get()) throw new IllegalStateException("Can't add more destroyable callbacks because the Injector is already destroyed.")
    if (toDestroy.isEmpty) addShutdownHook()

    toDestroy = toDestroy :+ fn
  }

  private def addShutdownHook() = {
    hookThread = Some(sys.addShutdownHook(doDestroyAll(IgnoringErrorHandler)))
  }

  def destroy(errorHandler: Throwable => Boolean = IgnoringErrorHandler) =
    doDestroyAll(errorHandler)

  private def doDestroyAll(errorHandler: Throwable => Boolean) = this.synchronized {

    breakable {
      toDestroy.reverse.foreach { d =>
        Try(d()) match {
          case Failure(error) =>
            if (!errorHandler(error)) break()
          case Success(_) => // just continue
        }
      }
    }

    destroyed.set(true)
    hookThread foreach Runtime.getRuntime.removeShutdownHook
    toDestroy = Nil
  }
}
