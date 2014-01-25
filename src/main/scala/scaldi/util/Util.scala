package scaldi.util

import language.implicitConversions

object Util {
  implicit class WorkflowHelper[T](val target: T) extends AnyVal {
    def |>[R](fn: T => R) = fn(target)
    def <|[R](fn: T => Unit) = {
      fn(target)
      target
    }
  }

  type Cache[K, V] = collection.mutable.Map[K, V]

  def createCache[K, V]: Cache[K, V] = new collection.mutable.HashMap[K, V]()

  implicit def toCacheUtils[K, V](cache: Cache[K, V]) = new CacheUtils(cache)

  class CacheUtils[K, V](cache: Cache[K, V]) {
    def caching(key: K)(fn: => V): V = cache.synchronized {
      cache get key getOrElse {
        val result = fn
        cache += key -> result
        result
      }
    }
  }

  def timed[R](metricsName: String)(fn: => R) = {
    val startTime = System.currentTimeMillis
    val res = fn
    val endTime = System.currentTimeMillis()

    println(metricsName + ": " + (endTime - startTime) + " ms")

    res
  }

  def ntimed[R](times:Int, metricsName: String)(fn: => R) = (1 to times).map(x => timed(metricsName)(fn)).last
}