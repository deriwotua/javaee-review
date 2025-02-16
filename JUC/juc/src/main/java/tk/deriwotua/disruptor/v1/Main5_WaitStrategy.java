package tk.deriwotua.disruptor.v1;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Disruptor 等待策略
 */
public class Main5_WaitStrategy {
    public static void main(String[] args) {

        /**
         * 工厂类
         */
        LongEventFactory factory = new LongEventFactory();

        /**
         * 环形队列长度
         * must be power of 2
         */
        int ringBufferSize = 1024;

        /**
         * 指定单线程模式
         *  ProducerType有两种模式
         *      Producer.MULTI
         *      Producer.SINGLE
         *    默认是MULTI，表示在多线程模式下产生sequence
         *    如果确认是单线程生产者，那么可以指定SINGLE，效率会提升
         *      多线程下访问队列会加锁会消耗CPU
         *  最后一个参数指定等待策略
         *      BlockingWaitStrategy(常用）：通过线程阻塞的方式，等待生产者唤醒，被唤醒后，再循环检查依赖的sequence是否已经消费。
         *      BusySpinWaitStrategy：线程一直自旋等待，可能比较耗cpu
         *      LiteBlockingWaitStrategy：线程阻塞等待生产者唤醒
         *          与BlockingWaitStrategy相比，区别在signalNeeded.getAndSet,
         *          如果两个线程同时访问一个访问waitfor,一个访问signalAll时，可以减少lock加锁次数.
         *      LiteTimeoutBlockingWaitStrategy：与LiteBlockingWaitStrategy相比，设置了阻塞时间，超过时间后抛异常。
         *      PhasedBackoffWaitStrategy：根据时间参数和传入的等待策略来决定使用哪种等待策略
         *      TimeoutBlockingWaitStrategy：相对于BlockingWaitStrategy来说，设置了等待时间，超过后抛异常
         *      YieldingWaitStrategy(常用)：尝试100次，然后Thread.yield()让出cpu
         *      SleepingWaitStrategy(常用): sleep
         */
        Disruptor<LongEvent> disruptor = new Disruptor<LongEvent>(factory, ringBufferSize,
                Executors.defaultThreadFactory(), ProducerType.MULTI, new BlockingWaitStrategy());

        /**
         * 指定消息处理handler
         */
        disruptor.handleEventsWith(new LongEventHandler());
        // 启动
        disruptor.start();

        /**
         * 生产者
         *  如果是多个生产者（多线程），但模式指定为SINGLE，会出什么问题呢？
         *      实际生产元素个数一定小于预期值
         *      下面模拟50个线程每个线程生产100个元素最后统计下
         *          会发现由于模式指定为SINGLE所以实际生产的元素个数永远小于5000
         */
        RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();
        // 定义50个线程
        final int threadCount = 50;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ExecutorService service = Executors.newCachedThreadPool();
        for (long i = 0; i < threadCount; i++){
            final long threadNum = i;
            service.submit(() -> {
                System.out.printf("Thread %s ready to start!\n", threadNum);
                try {
                    barrier.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                /**
                 * 每个线程生产100个元素
                 */
                for (int j = 0; j < 100; j++){
                    ringBuffer.publishEvent((event, sequence) -> {
                        event.setValue(threadNum);
                        System.out.println("生产了：" + threadNum);
                    });
                }
            });
        }

        service.shutdown();
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(LongEventHandler.count);
    }
}
