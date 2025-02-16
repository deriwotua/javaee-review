package tk.deriwotua.dp.D11_flyweight.multiflyweight;

import java.util.HashMap;
import java.util.Map;

/**
 * 复合享元对象是由单纯享元对象通过复合而成的因此它提供了add()这样的聚集管理方法。
 *
 * 由于一个复合享元对象具有不同的聚集元素，这些聚集元素在复合享元对象被创建之后加入，这本身就意味着复合享元对象的状态是会改变的，因此复合享元对象是不能共享的。
 *
 * 复合享元对象的所有单纯享元对象元素的外蕴状态都是与复合享元对象的外蕴状态相等的
 * 复合享元对象所含有的单纯享元对象的内蕴状态一般是不相等的
 */
public class ConcreteCompositeFlyweight implements Flyweight {

    /**
     * 聚集元素
     */
    private Map<Character, Flyweight> files = new HashMap<Character, Flyweight>();

    /**
     * 增加一个新的单纯享元对象到聚集中
     * @param key
     * @param fly
     */
    public void add(Character key, Flyweight fly) {
        files.put(key, fly);
    }

    /**
     * 外蕴状态作为参数传入到方法中
     *
     * @param state 外蕴状态
     */
    @Override
    public void operation(String state) {
        Flyweight fly = null;
        for (Object o : files.keySet()) {
            fly = files.get(o);
            fly.operation(state);
        }
    }
}