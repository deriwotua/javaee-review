package tk.deriwotua.dp.D04_abstractfactory.computer.amd;

import tk.deriwotua.dp.D04_abstractfactory.computer.Cpu;

public class AmdCpu implements Cpu {
    /**
     * CPU的针脚数
     */
    private int pins = 0;

    public AmdCpu(int pins) {
        this.pins = pins;
    }

    @Override
    public void calculate() {
        System.out.println("AMD CPU的针脚数：" + pins);
    }
}