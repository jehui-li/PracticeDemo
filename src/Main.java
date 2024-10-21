import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    static ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<String>();
    static int maxCount = 100;
    // 公平锁，消费者不会等太久才消费
    static Lock lock = new ReentrantLock(true);
    static Condition condition = lock.newCondition();

    static class Producer implements Runnable {
        @Override
        public void run() {
            lock.lock();
            try {
                for (int i = 0; i < 10; i++) {
                    while (queue.size() >= maxCount) {
                        condition.await();
                    }
                    String e = Thread.currentThread().getId() + "：" + System.currentTimeMillis();
                    queue.offer(e);
                    System.out.println("生产 " + e);
                    // 在耗时任务前唤醒别的线程
                    condition.signal();
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }
    }

    static class Consumer  implements Runnable {
        @Override
        public void run() {
            lock.lock();
            try {
                for (int i = 0; i < 10; i++) {
                    while (queue.isEmpty()) {
                        condition.await();
                    }
                    String e = queue.poll();
                    System.out.println(Thread.currentThread().getId() + " 消费 " + e);
                    // 在耗时任务前唤醒别的线程
                    condition.signal();
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }

        }
    }

    public static void main(String[] args) {
        int maxThreads = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(maxThreads);
        for (int i = 0; i < maxThreads / 2; i++) {
            executorService.execute(new Consumer());
            executorService.execute(new Producer());
        }
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        executorService.shutdownNow();
    }
}
