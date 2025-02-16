package tk.deriwotua.dp.D03_factorymethod.factorymethod;

/**
 * 具体工厂
 */
public class ExportPdfFactory implements ExportFactory {

    @Override
    public ExportFile factory(String type) {
        if("standard".equals(type)){

            return new ExportStandardPdfFile();

        }else if("financial".equals(type)){

            return new ExportFinancialPdfFile();

        }else{
            throw new RuntimeException("没有找到对象");
        }
    }

}