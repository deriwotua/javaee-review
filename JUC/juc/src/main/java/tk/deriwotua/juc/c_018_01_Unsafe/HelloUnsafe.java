package tk.deriwotua.juc.c_018_01_Unsafe;

//import sun.misc.*;

import sun.misc.Unsafe;

public class HelloUnsafe {
    static class M {
        private M() {}

        int i =0;
    }

   public static void main(String[] args) throws InstantiationException {
        // 通过 Unsafe 直接操作内存
        Unsafe unsafe = Unsafe.getUnsafe();
        M m = (M)unsafe.allocateInstance(M.class);
        m.i = 9;
        System.out.println(m.i);
    }
}


