package scaldi

import scala.reflect.runtime.universe.{Type, runtimeMirror, typeTag}

object Jsr330

/**
 * Binding for JSR 330 compliant types.
 */
case class AnnotationBinding(
  tpe: Type,
  additionalIdentifiers: List[Identifier] = Nil,
  condition: Option[() => Condition] = None,
  lifecycle: BindingLifecycle[Any] = BindingLifecycle.empty
) extends BindingWithLifecycle {
  require(ownIdentifiers.nonEmpty,
    s"Unable to create an annotation binding for class `$tpe`, because it's not JSR 330 compliant.")

  override lazy val identifiers = ownIdentifiers ++ additionalIdentifiers
  lazy val ownIdentifiers = AnnotationBinding.extractIdentifiers(tpe)

  override def get(lifecycleManager: LifecycleManager) = {
    val (instance, isNew) = getInstance()

    for {
      d <- lifecycle.destroy
      if isNew
    } lifecycleManager addDestroyable (() => d(instance))

    instance
  }

  private def getInstance() = {
    (null, true) // TODO just stub
  }
}

object AnnotationBinding {

  /**
   * Extracts a list of identifiers from JSR 330 compliant type
   */
  def extractIdentifiers(tpe: Type): List[Identifier] = Nil // TODO
}

/**
 * Injector that creates JSR 330 compliant bindings on-demand (when they are injected)
 */
class OnDemandAnnotationInjector extends MutableInjectorUser with InjectorWithLifecycle[OnDemandAnnotationInjector] with ShutdownHookLifecycleManager {
  private var bindings: List[BindingWithLifecycle] = Nil
  private var lifecycleManager: Option[LifecycleManager] = None

  def getBindingInternal(identifiers: List[Identifier]) =
    identifiers
      .collect {case TypeTagIdentifier(tpe) => tpe}
      .map (tpe => tpe -> AnnotationBinding.extractIdentifiers(tpe)) match {
        case (tpe, resultingIdentifiers) :: Nil if Identifier.sameAs(resultingIdentifiers, identifiers) =>
          bindings.find(_ isDefinedFor identifiers) orElse {
            this.synchronized {
              bindings.find(_ isDefinedFor identifiers) orElse {
                val binding = new AnnotationBinding(tpe)

                if (binding.isEager) {
                  lifecycleManager map binding.get getOrElse (
                    throw new InjectException("Attempt to inject binding before OnDemandAnnotationInjector was initialized"))
                }

                bindings = bindings :+ binding

                Some(binding)
              }
            }
          }
        case _ => None
      }

  def getBindingsInternal(identifiers: List[Identifier]) = getBindingInternal(identifiers).toList

  protected def init(lifecycleManager: LifecycleManager) = {
    this.lifecycleManager = Some(lifecycleManager)

    () => ()
  }
}
