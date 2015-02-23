package scaldi.jsr330

import javax.inject.Inject

import org.scalatest.{Matchers, WordSpec}
import scaldi.{InjectException, Injectable, Injector, Module}
import scaldi.Injectable._

class JSR330QualifierSpec extends WordSpec with Matchers {
  "JSR 330" should {
    "support complex qualifiers with attributes" in {
      implicit val inj = new Module {
        binding to annotated [AnnotatedTest1]

        binding identifiedBy annotation(TestQualifierImpl.of("dep1")) to AnnotatedDep("hello")
        binding identifiedBy annotation(TestQualifierImpl.of("dep2")) to AnnotatedDep("world")
      }

      val test = inject [AnnotatedTest1]

      test.dep1.name should be ("hello")
      test.dep2.name should be ("world")
    }

    "allow binding qualifier to override all more concrete bindings with `annotation` identifiers" in {
      implicit val inj = new Module {
        binding to annotated [AnnotatedTest1]

        binding identifiedBy annotation(TestQualifierImpl.of("dep1")) to AnnotatedDep("hello")
        binding identifiedBy annotation(TestQualifierImpl.of("dep2")) to AnnotatedDep("world")

        binding identifiedBy qualifier[TestQualifier] to AnnotatedDep("generic")
      }

      val test = inject [AnnotatedTest1]

      test.dep1.name should be ("generic")
      test.dep2.name should be ("generic")
    }

    "annotation identifier without actual annotation should not match identifiers with concrete annotation object specified" in {
      implicit val inj = new Module {
        binding to new AnnotatedTest2

        binding identifiedBy annotation(TestQualifierImpl.of("dep1")) to AnnotatedDep("hello")
        binding identifiedBy annotation(TestQualifierImpl.of("dep2")) to AnnotatedDep("world")
      }

      an [InjectException] should be thrownBy (inject [AnnotatedTest2])
    }

    "allow usage annotation identifiers in standard injection DSL" in {
      implicit val inj = new Module {
        binding to new AnnotatedTest2

        binding identifiedBy qualifier[TestQualifier] to AnnotatedDep("generic")

        binding identifiedBy annotation(TestQualifierImpl.of("dep1")) to AnnotatedDep("hello")
        binding identifiedBy annotation(TestQualifierImpl.of("dep2")) to AnnotatedDep("world")
      }

      val test = inject [AnnotatedTest2]

      test.dep1.name should be ("hello")
      test.dep2.name should be ("world")
      test.dep.name should be ("generic")
    }
  }
}

class AnnotatedTest1 @Inject() (@TestQualifier("dep1") val dep1: AnnotatedDep, @TestQualifier("dep2") val dep2: AnnotatedDep)
class AnnotatedTest2(implicit inj: Injector) extends Injectable {
  val dep1 = inject [AnnotatedDep] (identified by annotation(TestQualifierImpl.of("dep1")))
  val dep2 = inject [AnnotatedDep] (identified by annotation(TestQualifierImpl.of("dep2")))

  val dep = inject [AnnotatedDep] (identified by qualifier[TestQualifier])
}

case class AnnotatedDep(name: String)
