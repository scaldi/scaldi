package scaldi.jsr330

import org.junit.Test
import org.atinject.tck.auto.{Convertible, Car}
import scaldi.{Injectable, Module}

class Jsr330Modules extends Injectable {
  def createInjector =
    new Module {
      bind [Car] to {
        // FIXME: just mocking up the instantiation
        val c = classOf[Convertible].getDeclaredConstructors()(0)
        c.setAccessible(true)
        c.newInstance(null, null, null, null, null, null, null, null).asInstanceOf[Car]
      }
    }

  def injectCar = {
    implicit val injector = createInjector

    inject [Car]
  }
}
