/**
 * synchronized�ؼ���
 * ��ĳ���������
 * @author mashibing
 */

package tk.deriwotua.juc.c_001;

public class Synchronized_Object {
	
	private int count = 10;
	// 锁
	private Object o = new Object();
	
	public void m() {
		// 同步代码块
		synchronized(o) { //�κ��߳�Ҫִ������Ĵ��룬�������õ�o����
			count--;
			System.out.println(Thread.currentThread().getName() + " count = " + count);
		}
	}
	
}

