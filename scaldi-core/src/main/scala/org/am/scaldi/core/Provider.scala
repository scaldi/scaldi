package org.am.scaldi.core

/**
 * Provides some resource according to the defined function
 * 
 * @author Oleg Ilyenko
 */
trait Provider[R] {
    protected val providerFn: () => R
    def provide() = providerFn()
    def apply() = provide()
}

trait Provider1[R, P]  {
    protected val providerFn: P => R
    def provide(p: P) = providerFn(p)
    def apply(p: P) = provide(p)
}

trait Provider2[R, P1, P2]  {
    protected val providerFn: (P1, P2) => R
    def provide(p1: P1, p2: P2) = providerFn(p1, p2)
    def apply(p1: P1, p2: P2) = provide(p1, p2)
}

trait Provider3[R, P1, P2, P3]  {
    protected val providerFn: (P1, P2, P3) => R
    def provide(p1: P1, p2: P2, p3: P3) = providerFn(p1, p2, p3)
    def apply(p1: P1, p2: P2, p3: P3) = provide(p1, p2, p3)
}

object Provider {
    def apply[R](fn: => R) = new SimpleProvider(fn _)
    def apply[R, P](fn: P => R) = new SimpleProvider1(fn)
    def apply[R, P1, P2](fn: (P1, P2) => R) = new SimpleProvider2(fn)
    def apply[R, P1, P2, P3](fn: (P1, P2, P3) => R) = new SimpleProvider3(fn)
}

class SimpleProvider[R](val fn: () => R) extends Provider[R] {
    val providerFn = fn
}

class SimpleProvider1[R, P](val fn: (P) => R) extends Provider1[R, P] {
    val providerFn = fn
}

class SimpleProvider2[R, P1, P2](val fn: (P1, P2) => R) extends Provider2[R, P1, P2] {
    val providerFn = fn
}

class SimpleProvider3[R, P1, P2, P3](val fn: (P1, P2, P3) => R) extends Provider3[R, P1, P2, P3] {
    val providerFn = fn
}
