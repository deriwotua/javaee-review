package tk.deriwotua.dp.D13_Iterator.v3;

/**
 * 添加容器的共同接口，实现容器的替换
 */

public class Main {
    public static void main(String[] args) {
        Collection_ list = new ArrayList_();
        for(int i=0; i<15; i++) {
            list.add(new String("s" + i));
        }
        System.out.println(list.size());
    }
}


