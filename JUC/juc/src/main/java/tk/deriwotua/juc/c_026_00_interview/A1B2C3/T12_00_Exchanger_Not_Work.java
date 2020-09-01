package tk.deriwotua.juc.c_026_00_interview.A1B2C3;

import java.util.concurrent.Exchanger;

/**
 * Exchanger 两个线程间互相交换数据
 *  无法确保交换后打印顺序所以这个也不能实现
 */
public class T12_00_Exchanger_Not_Work {
    private static Exchanger<String> exchanger = new Exchanger<>();

    public static void main(String[] args) {
        char[] aI = "1234567".toCharArray();
        char[] aC = "ABCDEFG".toCharArray();

        /**
         * 交换后无法确保打印顺序
         */
        new Thread(()->{
            for(int i=0; i<aI.length; i++) {
                System.out.print(aI[i]);
                try {
                    exchanger.exchange("T1");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(()->{
            for(int i=0; i<aC.length; i++) {
                try {
                    exchanger.exchange("T2");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.print(aC[i]);
            }
        }).start();
    }
}
