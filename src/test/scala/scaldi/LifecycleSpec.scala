package scaldi

import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.{Matchers, WordSpec}
import scaldi.util.JvmTestUtil

class LifecycleSpec extends WordSpec with Matchers {

  "BindingLifecycle" should {
    "allow to define init function which should be called one time for lazy and non-lazy bindings" in {
      val module1 = new Module {
        bind [Server] as 'serverLazy to new LifecycleServer initWith (_.init())
      }

      val module2 = new Module {
        bind [Server] as 'serverNonLazy toNonLazy new LifecycleServer initWith (_.init())
      }

      implicit val moduleAggregation = module1 :: module2

      import Injectable._

      (1 to 20) foreach { _ =>
        inject[Server]('serverLazy)
        inject[Server]('serverNonLazy)
      }

      inject[Server]('serverLazy).asInstanceOf[LifecycleServer].initializedCount should equal (1)
      inject[Server]('serverNonLazy).asInstanceOf[LifecycleServer].initializedCount should equal (1)
    }

    "allow to define init function for provider bindings which should be called one time for each created object" in {
      var instanceCount = 0

      implicit val module = new Module {
        bind [Server] as 'server toProvider {instanceCount = instanceCount + 1; new LifecycleServer} initWith (_.init())
      }

      import Injectable._

      val instances = (1 to 20).toList map (_ => inject[Server]('server))

      instanceCount should be (20)
      instances foreach (_.asInstanceOf[LifecycleServer].initializedCount should equal (1))
    }

    "allow to define destroy function which should be called one time for every created instance" in {
      val module1 = new Module {
        bind [Server] as 'serverLazy to new LifecycleServer destroyWith (_.terminate())
      }

      val module2 = new Module {
        bind [Server] as 'serverNonLazy toNonLazy  new LifecycleServer destroyWith (_.terminate())
        bind [Server] as 'serverProvider toProvider new LifecycleServer destroyWith (_.terminate())
      }

      implicit val moduleAggregation = module1 :: module2

      import Injectable._

      val serverLazy = inject[Server] ('serverLazy)
      val serverNonLazy = inject[Server] ('serverNonLazy)
      val serverProvider = inject[Server] ('serverProvider)

      1 to 20 foreach (_ => moduleAggregation.destroy())

      serverLazy.asInstanceOf[LifecycleServer].destroyedCount should equal (1)
      serverNonLazy.asInstanceOf[LifecycleServer].destroyedCount should equal (1)
      serverProvider.asInstanceOf[LifecycleServer].destroyedCount should equal (1)
    }
  }

  "ShutdownHookLifecycleManager" should {
    "not allow to add destroyable after it was already destroyed" in {
      val countOfShutdownHooksBefore = JvmTestUtil.shutdownHookCount

      implicit val module = new Module {
        bind [Server] as 'server to new LifecycleServer initWith (_.init()) destroyWith (_.terminate())
      }

      import Injectable._

      inject[Server]('server).asInstanceOf[LifecycleServer].initializedCount should be (1)

      val destroyedCount = new AtomicInteger(0)

      module.addDestroyable(() => destroyedCount.incrementAndGet())

      JvmTestUtil.shutdownHookCount should be (countOfShutdownHooksBefore + 1)

      module.destroy()

      destroyedCount.get should be (1)
      JvmTestUtil.shutdownHookCount should be (countOfShutdownHooksBefore)

      an [IllegalStateException] should be thrownBy
          module.addDestroyable(() => destroyedCount.incrementAndGet())
    }
  }
}
