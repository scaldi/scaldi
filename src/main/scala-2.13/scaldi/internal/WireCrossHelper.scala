package scaldi.internal

import scala.reflect.macros.blackbox

private[scaldi] object WireCrossHelper {

  /** Helper for creating a named arg tree when cross building. */
  def CrossNamedArg(c: blackbox.Context)(lhs: c.universe.Tree, rhs: c.universe.Tree): c.universe.TermTree =
    c.universe.NamedArg(lhs, rhs)
}
