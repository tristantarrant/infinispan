package org.infinispan.server.hotrod.event;

import static org.infinispan.server.hotrod.OperationStatus.Success;
import static org.infinispan.server.hotrod.test.HotRodTestingUtil.assertStatus;
import static org.infinispan.server.hotrod.test.HotRodTestingUtil.withClientListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.infinispan.server.hotrod.HotRodSingleNodeTest;
import org.infinispan.server.hotrod.test.HotRodClient;
import org.infinispan.testing.TestResourceTracker;
import org.testng.annotations.Test;

/**
 * Verifies that concurrent put operations on the same key complete successfully
 * when a client listener is registered, without triggering lock timeout errors (ISPN000299).
 *
 * @see <a href="https://github.com/infinispan/infinispan/issues/17270">ISPN-17270</a>
 */
@Test(groups = "functional", testName = "server.hotrod.event.HotRodEventBackPressureTest")
public class HotRodEventBackPressureTest extends HotRodSingleNodeTest {

   public void testConcurrentPutWithListenerDoesNotTimeout(Method m) {
      EventLogListener eventListener = new EventLogListener();
      withClientListener(client(), eventListener, Optional.empty(), Optional.empty(), () -> {
         int numThreads = 10;
         int opsPerThread = 100;
         CyclicBarrier barrier = new CyclicBarrier(numThreads + 1);
         List<Future<Void>> futures = new ArrayList<>();
         List<HotRodClient> clients = new ArrayList<>();

         try {
            for (int i = 0; i < numThreads; i++) {
               HotRodClient threadClient = connectClient();
               clients.add(threadClient);
               final int threadId = i;
               futures.add(fork(() -> {
                  TestResourceTracker.testThreadStarted(getTestName());
                  barrier.await(30, TimeUnit.SECONDS);
                  try {
                     byte[] key = "same-key".getBytes();
                     for (int j = 0; j < opsPerThread; j++) {
                        byte[] value = ("v-" + threadId + "-" + j).getBytes();
                        assertStatus(threadClient.put(key, 0, 0, value), Success);
                     }
                  } finally {
                     barrier.await(60, TimeUnit.SECONDS);
                  }
                  return null;
               }));
            }

            barrier.await(30, TimeUnit.SECONDS);
            barrier.await(120, TimeUnit.SECONDS);

            for (Future<Void> f : futures) {
               f.get(5, TimeUnit.SECONDS);
            }
         } catch (Exception e) {
            throw new RuntimeException(e);
         } finally {
            for (HotRodClient c : clients) {
               c.stop();
            }
         }
      });
   }
}
