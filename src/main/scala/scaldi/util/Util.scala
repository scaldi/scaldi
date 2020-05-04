package scaldi.util

import language.implicitConversions

object Util {
  implicit class WorkflowHelper[T](val target: T) extends AnyVal {
    def |>[R](fn: T => R): R = fn(target)
    def <|(fn: T => Unit): T = {
      fn(target)
      target
    }
  }

  type Cache[K, V] = collection.mutable.Map[K, V]

  def createCache[K, V]: Cache[K, V] = new collection.mutable.HashMap[K, V]()

  implicit def toCacheUtils[K, V](cache: Cache[K, V]): CacheUtils[K, V] = new CacheUtils(cache)

  class CacheUtils[K, V](private val cache: Cache[K, V]) extends AnyVal {
    def caching(key: K)(fn: => V): V = cache.synchronized {
      cache.getOrElseUpdate(key, fn)
    }
  }

  def timed[R](metricsName: String)(fn: => R): R = {
    val startTime = System.currentTimeMillis
    val res = fn
    val endTime = System.currentTimeMillis()

    println(metricsName + ": " + (endTime - startTime) + " ms")

    res
  }

  def ntimed[R](times:Int, metricsName: String)(fn: => R): R = (1 to times).map(x => timed(metricsName)(fn)).last
}