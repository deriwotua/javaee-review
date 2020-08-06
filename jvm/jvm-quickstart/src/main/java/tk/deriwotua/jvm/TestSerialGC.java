package tk.deriwotua.jvm;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/**
 * 测试SerialGC串行收集器
 */
public class TestSerialGC {
    /**
     * 不断产生新的对象，在随机废弃对象
     */
    public static void main(String[] args) throws Exception {
        List<Object> list = new ArrayList<Object>();
        while (true) {
            // 随机休眠
            int sleep = new Random().nextInt(100);
            if (System.currentTimeMillis() % 2 == 0) {
                // 时间戳偶数时废弃
                list.clear();
            } else {
                //否则产生新对象
                for (int i = 0; i < 10000; i++) {
                    Properties properties = new Properties();
                    properties.put("key_" + i, "value_" + System.currentTimeMillis() + i);
                    list.add(properties);
                }
            }
            // System.out.println("list大小为：" + list.size());
            Thread.sleep(sleep);
        }
    }

    /**
     * 直接执行是没有任何效果的 需要配置启动参数
     * - 指定年轻代和老年代都使用串行垃圾收集器 `-XX:+UseSerialGC`
     * - 打印垃圾回收的详细信息 `-XX:+PrintGCDetails`
     * - 为了方便测试启动时把初始堆内存和最大堆内存设置小一点
     *
     * -XX:+UseSerialGC -XX:+PrintGCDetails -Xms16m -Xmx16m
     */
}
