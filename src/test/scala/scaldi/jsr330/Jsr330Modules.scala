package scaldi.jsr330

import org.junit.Test
import org.atinject.tck.auto.{Convertible, Car}
import scaldi.{OnDemandAnnotationInjector, Injectable, Module}

class Jsr330Modules extends Injectable {
  lazy val CarModule = new Module {
    bind [Car] to annotated [Convertible]
  }

  def injectCar = {
    implicit val injector = CarModule :: new OnDemandAnnotationInjector

    inject [Car]
  }
}
