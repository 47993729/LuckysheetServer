package com.xc.luckysheet.xlsutils.poiutil;

import com.mongodb.DBObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.*;

/**
 * sheet操作
 * @author Administrator
 */
@Slf4j
public class XlsSheetUtil {
    /**
     * 导出sheet
     * @param wb
     * @param sheetNum
     * @param dbObject
     */
    public static void exportSheet(Workbook wb, int sheetNum, DBObject dbObject){
        Sheet sheet=wb.createSheet();

        //设置sheet位置，名称
        if(dbObject.containsField("name")&&dbObject.get("name")!=null){
            wb.setSheetName(sheetNum,dbObject.get("name").toString());
        }else{
            wb.setSheetName(sheetNum,"sheet"+sheetNum);
        }
        //是否隐藏
        if(dbObject.containsField("hide") && dbObject.get("hide").toString().equals("1")){
            wb.setSheetHidden(sheetNum,true);
        }
        //是否当前选中页
        if(dbObject.containsField("status") && dbObject.get("status").toString().equals("1")){
            sheet.setSelected(true);
        }


        //循环数据
        if(dbObject.containsField("celldata")&&dbObject.get("celldata")!=null){
            //取到所有单元格集合
            List<DBObject> cells_json = ( List<DBObject> )dbObject.get("celldata");
            Map<Integer,List<DBObject>> cellMap=cellGroup(cells_json);
            //循环每一行
            for(Integer r:cellMap.keySet()){
                Row row=sheet.createRow(r);
                //循环每一列
                for(DBObject col:cellMap.get(r)){
                    createCell(wb,row,col);
                }
            }
        }

    }

    /**
     * 每一个单元格
     * @param row
     * @param dbObject
     */
    private static void createCell(Workbook wb,Row row,DBObject dbObject){
        if(dbObject.containsField("c")) {
            Integer c = getStrToInt(dbObject.get("c"));
            if (c != null) {
                Cell cell=row.createCell(c);
                //取单元格中的v_json
                if(dbObject.containsField("v")) {
                    //获取v对象
                    Object obj = dbObject.get("v");
                    if (obj == null) {
                        //没有内容
                        return;
                    }
                    //如果v对象直接是字符串
                    if(obj instanceof String){
                        if(((String) obj).length()>0){
                            cell.setCellValue(obj.toString());
                        }
                        return;
                    }

                    //转换v为对象(v是一个对象)
                    DBObject v_json = (DBObject)obj;
                    //样式
                    CellStyle style= wb.createCellStyle();
                    cell.setCellStyle(style);

                    //bs 边框样式 //bc 边框颜色
                    setBorderStyle(style,v_json,"bs","bc");
                    //bs_t 上边框样式   bc_t  上边框颜色
                    setBorderStyle(style,v_json,"bs_t","bc_t");
                    //bs_b 下边框样式   bc_b  下边框颜色
                    setBorderStyle(style,v_json,"bs_b","bc_b");
                    //bs_l 左边框样式   bc_l  左边框颜色
                    setBorderStyle(style,v_json,"bs_l","bc_l");
                    //bs_r 右边框样式   bc_r  右边框颜色
                    setBorderStyle(style,v_json,"bs_r","bc_r");


                    //mc 合并单元格
//                    if(v_json.containsField("mc")){
//                        //是合并的单元格
//                        DBObject mc=(DBObject)v_json.get("mc");
//                        if(mc.containsField("rs") && mc.containsField("cs")){
//                            //合并的第一个单元格
//                            if(mc.containsField("r") && mc.containsField("c")){
//                                Integer rs=getIntByDBObject(mc,"rs");
//                                Integer cs=getIntByDBObject(mc,"cs");
//                                Integer r=getIntByDBObject(mc,"r");
//                                Integer c=getIntByDBObject(mc,"c");
//                                cells.merge(r,c,rs,cs);
//                            }
//                        }else{
//                            //不是合并的第一个单元格
//                            cell.setStyle(style);
//                            return;
//                        }
//                    }


                    //取v值 (在数据类型中处理)
                    //ct 单元格值格式 (fa,t)
                    setFormatByCt(wb,cell,style,v_json);

                    //font设置
                    setCellStyleFont(wb,style,v_json);

                    //bg 背景颜色
                    if(v_json.containsField("bg")){
                        String _v=getByDBObject(v_json,"bg");
                        style.setFillBackgroundColor(ColorUtil.getColorByStr(_v));
                    }

                    //vt 垂直对齐    垂直对齐方式（0=居中，1=上，2=下）
                    if(v_json.containsField("vt")){
                        Integer _v=getIntByDBObject(v_json, "vt");
                        if(_v!=null && _v>=0 && _v<=2){
                            style.setVerticalAlignment(ConstantUtil.getVerticalType(_v));
                        }
                    }

                    //ht 水平对齐   水平对齐方式（0=居中，1=左对齐，2=右对齐）
                    if(v_json.containsField("ht")){
                        Integer _v=getIntByDBObject(v_json,"ht");
                        if(_v!=null && _v>=0 && _v<=2){
                            style.setAlignment(ConstantUtil.getHorizontaltype(_v));
                        }
                    }

                    //tr 文字旋转 文字旋转角度（0=0,1=45，2=-45，3=竖排文字，4=90，5=-90）
                    if(v_json.containsField("tr")){
                        Integer _v=getIntByDBObject(v_json, "tr");
                        if(_v!=null){
                            style.setRotation(ConstantUtil.getRotation(_v));
                        }
                    }

                    //tb  文本换行    0 截断、1溢出、2 自动换行
                    //   2：setTextWrapped     0和1：IsTextWrapped = true
                    if(v_json.containsField("tb")){
                        Integer _v=getIntByDBObject(v_json,"tb");
                        if(_v!=null){
                            if(_v>=0 && _v<=1){
                                style.setWrapText(false);
                            }else{
                                style.setWrapText(true);
                            }
                        }
                    }

                    //f  公式
                    if(v_json.containsField("f")){
                        String _v=getByDBObject(v_json,"f");
                        if(_v.length()>0){
                            cell.setCellFormula(_v);
                        }
                    }


                }

            }
        }
    }

    //设置行高
    private static void setRowHeight(Sheet sheet){
        Row row=sheet.getRow(0);
        row.setHeightInPoints(30);
    }

    /**
     * 设置列宽
     * 第一个参数代表列id(从0开始),第2个参数代表宽度值  参考 ："2012-08-10"的宽度为2500
     * @param sheet
     */
    private static void setColumnWidth(Sheet sheet){
        sheet.setColumnWidth(0,MSExcelUtil.pixel2WidthUnits(160));
    }

    /**
     * 单元格字体相关样式
     * @param wb
     * @param style
     * @param dbObject
     */
    private static void setCellStyleFont(Workbook wb,CellStyle style,DBObject dbObject){
        Font font = wb.createFont();
        style.setFont(font);

        //ff 字体
        if(dbObject.containsField("ff")){
            Integer _v=getIntByDBObject(dbObject,"ff");
            if(_v!=null && ConstantUtil.ff_IntegerToName.containsKey(_v)){
                font.setFontName(ConstantUtil.ff_IntegerToName.get(_v));
            }
        }
        //fc 字体颜色
        if(dbObject.containsField("fc")){
            String _v=getByDBObject(dbObject,"fc");
            font.setColor(ColorUtil.getColorByStr(_v));
        }
        //bl 粗体
        if(dbObject.containsField("bl")){
            Integer _v=getIntByDBObject(dbObject,"bl");
            if(_v!=null){
                if(_v.equals(1)) {
                    //是否粗体显示
                    font.setBold(true);
                }else{
                    font.setBold(false);
                }
            }
        }
        //it 斜体
        if(dbObject.containsField("it")){
            Integer _v=getIntByDBObject(dbObject,"it");
            if(_v!=null){
                if(_v.equals(1)) {
                    font.setItalic(true);
                }else{
                    font.setItalic(false);
                }
            }
        }
        //fs 字体大小
        if(dbObject.containsField("fs")){
            Integer _v=getStrToInt(getObjectByDBObject(dbObject,"fs"));
            if(_v!=null){
                font.setFontHeightInPoints(_v.shortValue());
            }
        }
        //cl 删除线 (导入没有)   0 常规 、 1 删除线
        if(dbObject.containsField("cl")){
            Integer _v=getIntByDBObject(dbObject,"cl");
            if(_v!=null){
                if(_v.equals(1)) {
                    font.setStrikeout(true);
                }
            }
        }
        //ul 下划线
        if(dbObject.containsField("ul")){
            Integer _v=getIntByDBObject(dbObject,"ul");
            if(_v!=null){
                if(_v.equals(1)) {
                    font.setUnderline(Font.U_SINGLE);
                }else{
                    font.setUnderline(Font.U_NONE);
                }
            }
        }
        //合并单元格
        //参数1：起始行 参数2：终止行 参数3：起始列 参数4：终止列
        //CellRangeAddress region1 = new CellRangeAddress(rowNumber, rowNumber, (short) 0, (short) 11);
    }

    /**
     * 设置cell边框颜色样式
     * @param style 样式
     * @param dbObject json对象
     * @param bs 样式
     * @param bc 样式
     */
    private static void setBorderStyle(CellStyle style,DBObject dbObject,String bs,String bc ){
        //bs 边框样式
        if(dbObject.containsField(bs)){
            Integer _v=getStrToInt(getByDBObject(dbObject,bs));
            if(_v!=null){
                //边框没有，不作改变
                if(bs.equals("bs") || bs.equals("bs_t")){
                    style.setBorderTop(BorderStyle.valueOf(_v.shortValue()));
                }
                if(bs.equals("bs") || bs.equals("bs_b")){
                    style.setBorderBottom(BorderStyle.valueOf(_v.shortValue()));
                }
                if(bs.equals("bs") || bs.equals("bs_l")){
                    style.setBorderLeft(BorderStyle.valueOf(_v.shortValue()));
                }
                if(bs.equals("bs") || bs.equals("bs_r")){
                    style.setBorderRight(BorderStyle.valueOf(_v.shortValue()));
                }

                //bc 边框颜色
                String _vcolor=getByDBObject(dbObject,bc);
                if(_vcolor!=null){
                    Short _color=ColorUtil.getColorByStr(_vcolor);
                    if(_color!=null){
                        if(bc.equals("bc") || bc.equals("bc_t")){
                            style.setTopBorderColor(_color);
                        }
                        if(bc.equals("bc") || bc.equals("bc_b")){
                            style.setBottomBorderColor(_color);
                        }
                        if(bc.equals("bc") || bc.equals("bc_l")){
                            style.setLeftBorderColor(_color);
                        }
                        if(bc.equals("bc") || bc.equals("bc_r")){
                            style.setRightBorderColor(_color);
                        }
                    }
                }
            }
        }
    }


    /**
     * 设置单元格格式  ct 单元格值格式 (fa,t)
     * @param cell
     * @param style
     * @param dbObject
     */
    private static void setFormatByCt(Workbook wb,Cell cell,CellStyle style,DBObject dbObject){

        //String v = "";  //初始化
        if(dbObject.containsField("v")){
            //v = v_json.get("v").toString();
            //取到v后，存到poi单元格对象
            //设置该单元格值
            //cell.setValue(v);

            //String v=getByDBObject(v_json,"v");
            //cell.setValue(v);
            Object obj=getObjectByDBObject(dbObject,"v");
            if(obj instanceof Double){
                cell.setCellValue((Double) obj);
            }else if(obj instanceof Date){
                cell.setCellValue((Date)obj);
            }else if(obj instanceof Calendar){
                cell.setCellValue((Calendar) obj);
            }else if(obj instanceof RichTextString){
                cell.setCellValue((RichTextString) obj);
            }else if(obj instanceof String){
                cell.setCellValue((String) obj);
            }

        }

        if(dbObject.containsField("ct")){
            DBObject ct=(DBObject)dbObject.get("ct");
            if(ct.containsField("fa") && ct.containsField("t")){
                //t 0=bool，1=datetime，2=error，3=null，4=numeric，5=string，6=unknown
                String fa=getByDBObject(ct,"fa"); //单元格格式format定义串
                String t=getByDBObject(ct,"t"); //单元格格式type类型

                Integer _i=ConstantUtil.getNumberFormatMap(fa);
                switch(t){
                    case "s":{
                        //字符串
                        if(_i>=0){
                            style.setDataFormat(_i.shortValue());
                        }else{
                            style.setDataFormat((short)0);
                        }
                        cell.setCellType(CellType.STRING);
                        break;
                    }
                    case "d":{
                        //日期
                        Date _d=null;
                        String v=getByDBObject(dbObject,"m");
                        if(v.length()==0){
                            v=getByDBObject(dbObject,"v");
                        }
                        if(v.length()>0){
                            if(v.indexOf("-")>-1){
                                if(v.indexOf(":")>-1){
                                    _d= ConstantUtil.stringToDateTime(v);
                                }else{
                                    _d= ConstantUtil.stringToDate(v);
                                }
                            }else{
                                _d= ConstantUtil.toDate(v);
                            }
                        }
                        if(_d!=null){
                            //能转换为日期
                            cell.setCellValue(_d);
                            DataFormat format= wb.createDataFormat();
                            style.setDataFormat(format.getFormat(fa));

                        }else{
                            //不能转换为日期
                            if(_i>=0){
                                style.setDataFormat(_i.shortValue());
                            }else{
                                style.setDataFormat((short)0);
                            }
                        }
                        break;
                    }
                    case "b":{
                        //逻辑
                        cell.setCellType(CellType.BOOLEAN);
                        if(_i>=0){
                            style.setDataFormat(_i.shortValue());
                        }else{
                            DataFormat format= wb.createDataFormat();
                            style.setDataFormat(format.getFormat(fa));
                        }
                        break;
                    }
                    case "n":{
                        //数值
                        cell.setCellType(CellType.NUMERIC);
                        if(_i>=0){
                            style.setDataFormat(_i.shortValue());
                        }else{
                            DataFormat format= wb.createDataFormat();
                            style.setDataFormat(format.getFormat(fa));
                        }
                        break;
                    }
                    case "u":
                    case "g":{
                        //general 自动类型
                        //cell.setCellType(CellType._NONE);
                        if(_i>=0){
                            style.setDataFormat(_i.shortValue());
                        }else{
                            DataFormat format= wb.createDataFormat();
                            style.setDataFormat(format.getFormat(fa));
                        }
                        break;
                    }
                    case "e":{
                        //错误
                        cell.setCellType(CellType.ERROR);
                        if(_i>=0){
                            style.setDataFormat(_i.shortValue());
                        }else{
                            DataFormat format= wb.createDataFormat();
                            style.setDataFormat(format.getFormat(fa));
                        }
                        break;
                    }

                }

            }

        }
    }

    /**
     * 内容按行分组
     * @param cells
     * @return
     */
    private static Map<Integer,List<DBObject>> cellGroup(List<DBObject> cells){
        Map<Integer,List<DBObject>> cellMap=new HashMap<>(100);
        for(DBObject dbObject:cells){
            //行号
            if(dbObject.containsField("r")){
                Integer r =getStrToInt(dbObject.get("r"));
                if(r!=null){
                    if(cellMap.containsKey(r)){
                        cellMap.get(r).add(dbObject);
                    }else{
                        List<DBObject> list=new ArrayList<>(10);
                        list.add(dbObject);
                        cellMap.put(r,list);
                    }
                }
            }

        }
        return cellMap;
    }


    /**
     * 获取一个k的值
     * @param b
     * @param k
     * @return
     */
    public static String getByDBObject(DBObject b,String k){
        if(b.containsField(k)){
            if(b.get(k)!=null&&b.get(k)instanceof String){
                return b.get(k).toString();
            }
        }
        return null;
    }

    /**
     * 获取一个k的值
     * @param b
     * @param k
     * @return
     */
    public static Object getObjectByDBObject(DBObject b,String k){
        if(b.containsField(k)){
            if(b.get(k)!=null){
                return b.get(k);
            }
        }
        return "";
    }

    /**
     * 没有/无法转换 返回null
     * @param b
     * @param k
     * @return
     */
    public static Integer getIntByDBObject(DBObject b,String k){
        if(b.containsField(k)){
            if(b.get(k)!=null){
                try{
                    String _s=b.get(k).toString().replace("px", "");
                    Double _d=Double.parseDouble(_s);
                    return _d.intValue();
                }catch (Exception ex){
                    System.out.println(ex.toString());
                    return null;
                }
            }
        }
        return null;
    }
    /**
     * 转int
     * @param str
     * @return
     */
    private static Integer getStrToInt(Object str){
        try{
            if(str!=null) {
                return Integer.parseInt(str.toString());
            }
            return null;
        }catch (Exception ex){
            log.error("String:{};Error:{}",str,ex.getMessage());
            return null;
        }
    }
    private static Short getStrToShort(Object str){
        try{
            if(str!=null) {
                return Short.parseShort(str.toString());
            }
            return null;
        }catch (Exception ex){
            log.error("String:{};Error:{}",str,ex.getMessage());
            return null;
        }
    }
}
