package tk.deriwotua.zookeeper.api;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ZKGetChid {
    String IP = "127.0.0.1:2181";
    ZooKeeper zooKeeper;

    @Before
    public void before() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        /**
         * arg1:zookeeper服务器的ip地址和端口号
         * arg2:连接的超时时间  以毫秒为单位
         * arg3:监听器对象
         */
        zooKeeper = new ZooKeeper(IP, 5000, new Watcher() {
            public void process(WatchedEvent event) {
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("连接创建成功!");
                    countDownLatch.countDown();
                }
            }
        });
        // 使主线程阻塞等待
        countDownLatch.await();
    }

    @After
    public void after() throws Exception {
        zooKeeper.close();
    }

    /**
     * 获取子节点
     * @throws Exception
     */
    @Test
    public void get1() throws Exception {
        // arg1:节点的路径
        List<String> list = zooKeeper.getChildren("/get", false);
        for (String str : list) {
            System.out.println(str);
        }
    }

    /**
     * 异步获取子节点
     * @throws Exception
     */
    @Test
    public void get2() throws Exception {
        zooKeeper.getChildren("/get", false, new AsyncCallback.ChildrenCallback() {
            public void processResult(int rc, String path, Object ctx, List<String> children) {
                // 0代表读取成功
                System.out.println(rc);
                // 节点的路径
                System.out.println(path);
                // 上下文参数对象
                System.out.println(ctx);
                // 子节点信息
                for (String str : children) {
                    System.out.println(str);
                }
            }
        }, "I am Context");
        Thread.sleep(10000);
        System.out.println("结束");
    }
}