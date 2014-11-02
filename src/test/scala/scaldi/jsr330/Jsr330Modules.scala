package scaldi.jsr330

import org.junit.Test
import org.atinject.tck.auto._

import scaldi._
import org.atinject.tck.auto.accessories.SpareTire

class Jsr330Modules extends Injectable {
  lazy val CarModule = new Module {
    bind [Car] to annotated [Convertible]
    bind [Engine] to annotated [V8Engine]
    bind [Seat] identifiedBy qualifier [Drivers] to annotated [DriversSeat]
    bind [Tire] identifiedBy required('spare) to annotated [SpareTire]
  }

  def injectCar = {
    implicit val injector = CarModule :: new OnDemandAnnotationInjector

    inject [Car]
  }
}
