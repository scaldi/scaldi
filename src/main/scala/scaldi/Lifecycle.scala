package scaldi

import java.util.concurrent.atomic.AtomicBoolean

import scaldi.util.Util._
import scala.util.control.NonFatal
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
  def empty[T]: BindingLifecycle[T] = BindingLifecycle[T]()
}

trait LifecycleManager {
  val IgnoringErrorHandler: Throwable => Boolean =
    (e: Throwable) => {e.printStackTrace(); true}

  def addDestroyable(fn: () => Unit): Unit

  def destroy(errorHandler: Throwable => Boolean = IgnoringErrorHandler): Unit
}

trait ShutdownHookLifecycleManager extends LifecycleManager {
  private var toDestroy: List[() => Unit] = Nil
  private val destroyed = new AtomicBoolean(false)
  private var hookThread: Option[Thread] = None

  def addDestroyable(fn: () => Unit): Unit = this.synchronized {
    if (destroyed.get()) throw new IllegalStateException("Can't add more destroyable callbacks because the Injector is already destroyed.")
    if (toDestroy.isEmpty) addShutdownHook()

    toDestroy = toDestroy :+ fn
  }

  private def addShutdownHook(): Unit = {
    hookThread = Some(sys.addShutdownHook(doDestroyAll(IgnoringErrorHandler)))
  }

  def destroy(errorHandler: Throwable => Boolean = IgnoringErrorHandler): Unit =
    doDestroyAll(errorHandler, manual = true)

  private def doDestroyAll(errorHandler: Throwable => Boolean, manual: Boolean = false): Unit = this.synchronized {

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

    if (manual)
      try {
        hookThread foreach Runtime.getRuntime.removeShutdownHook
      } catch {
        case NonFatal(e) => // do nothing
      }

    toDestroy = Nil
  }
}
