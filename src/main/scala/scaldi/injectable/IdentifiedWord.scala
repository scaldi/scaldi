package scaldi.injectable

import scaldi.{CanBeIdentifier, Identifier}

class IdentifiedWord[+T](default: Option[() => T] = None, initialIdentifiers: List[Identifier] = Nil) {
  def by[I: CanBeIdentifier](target: I*) = new InjectConstraints(default, initialIdentifiers ++ (target map implicitly[CanBeIdentifier[I]].toIdentifier))
}

