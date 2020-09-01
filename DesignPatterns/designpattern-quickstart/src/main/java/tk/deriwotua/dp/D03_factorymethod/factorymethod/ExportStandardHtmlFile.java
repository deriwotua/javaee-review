package tk.deriwotua.dp.D03_factorymethod.factorymethod;

/**
 * 具体导出类
 */
public class ExportStandardHtmlFile implements ExportFile{

    @Override
    public boolean export(String data) {
        /**
         * 业务逻辑
         */
        System.out.println("导出标准HTML文件");
        return true;
    }

}