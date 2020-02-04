package scaldi.util

object JvmTestUtil {

  // It is not pretty at all, but if you know better way to get JVM shutdown hook count, I would be happy to use it :)
  def shutdownHookCount: Int = {
    val hooksField = Class.forName("java.lang.ApplicationShutdownHooks").getDeclaredField("hooks")

    if (!hooksField.isAccessible) hooksField.setAccessible(true)

    hooksField.get(null).asInstanceOf[java.util.Map[_, _]].size
  }
}
