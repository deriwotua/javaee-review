package tk.deriwotua.dp.D13_Iterator.v5;

/**
 * 如何通用对容器遍历呢？
 *      各容器自己实现自己的迭代方法
 * 用一种统一的遍历方式，要求每一个容器都要提供Iterator的实现类
 *    作业：实现LinkedList的Iterator
 */

public class Main {
    public static void main(String[] args) {
        Collection_ list = new ArrayList_();
        for(int i=0; i<15; i++) {
            list.add(new String("s" + i));
        }
        System.out.println(list.size());

        //这个接口的调用方式：
        Iterator_ it = list.iterator();
        while(it.hasNext()) {
            Object o = it.next();
            System.out.println(o);
        }
    }
}


