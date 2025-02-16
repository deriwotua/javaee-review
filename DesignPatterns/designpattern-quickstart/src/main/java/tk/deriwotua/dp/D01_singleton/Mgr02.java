package tk.deriwotua.dp.D01_singleton;

/**
 * 跟01是一个意思
 */
public class Mgr02 {
    private static final Mgr02 INSTANCE;
    static {
        INSTANCE = new Mgr02();
    }

    /**
     * 私有默认构造方法
     */
    private Mgr02() {};

    /**
     * 静态工厂方法
     */
    public static Mgr02 getInstance() {
        return INSTANCE;
    }

    public void m() {
        System.out.println("m");
    }

    public static void main(String[] args) {
        Mgr02 m1 = Mgr02.getInstance();
        Mgr02 m2 = Mgr02.getInstance();
        System.out.println(m1 == m2);
    }
}
