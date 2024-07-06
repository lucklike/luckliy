package com.luckyframework.common;

import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import org.springframework.lang.NonNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 表格类，将数据格式化为一张表格
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/29 00:18
 */
public class Table {

    private final List<String> header = new ArrayList<>();
    private final List<List<Object>> dataRows = new ArrayList<>();

    /**
     * 组成表头的元素
     */
    private String headerEntry;

    /**
     * 表头上半部分的分隔符
     */
    private String onHeaderSep;
    /**
     * 表头上半部分的开始符
     */
    private String onHeaderStartSep;
    /**
     * 表头上半部分的结束符
     */
    private String onHeaderEndSep;


    /**
     * 表头下半部分的分隔符
     */
    private String underHeaderSep;
    /**
     * 表头下半部分的开始符
     */
    private String underHeaderStartSep;
    /**
     * 表头下半部分的结束符
     */
    private String underHeaderEndSep;

    /**
     * 表脚部分的分隔符
     */
    private String footSep;
    /**
     * 表脚部分的开始符
     */
    private String footStartSep;
    /**
     * 表脚部分的结束符
     */
    private String footEndSep;

    /**
     * 数据填充符
     */
    private String dataFiller;
    /**
     * 数据分隔符
     */
    private String dataSep;
    /**
     * 数据开始符
     */
    private String dataStartSep;
    /**
     * 数据结束符
     */
    private String dataEndSep;

    public Table() {
        styleOne();
    }


    /**
     * 样式1:
     * +-----+------+-----+
     * | id  | name | age |
     * +-----+------+-----+
     * | 1   | Jack | 23  |
     * | 22  | Lucy | 18  |
     * | 333 | Tom  | 35  |
     * +-----+------+-----+
     */
    public void styleOne() {
        this.headerEntry = "-";
        this.onHeaderSep = "+";
        this.onHeaderStartSep = "+";
        this.onHeaderEndSep = "+";
        this.underHeaderSep = "+";
        this.underHeaderStartSep = "+";
        this.underHeaderEndSep = "+";
        this.footSep = "+";
        this.footStartSep = "+";
        this.footEndSep = "+";
        this.dataFiller = " ";
        this.dataSep = "|";
        this.dataStartSep = "|";
        this.dataEndSep = "|";
    }

    /**
     * 样式2:
     * ┏━━━━┳━━━━━━┳━━━━━┓
     * ┃ ID ┃ NAME ┃ AGE ┃
     * ┣━━━━╋━━━━━━╋━━━━━┫
     * ┃ 1  ┃ Jack ┃ 23  ┃
     * ┃ 2  ┃ Lucy ┃ 18  ┃
     * ┃ 3  ┃ Tom  ┃ 35  ┃
     * ┗━━━━┻━━━━━━┻━━━━━┛
     */
    public void styleTwo() {
        this.headerEntry = "━";
        this.onHeaderSep = "┳";
        this.onHeaderStartSep = "┏";
        this.onHeaderEndSep = "┓";
        this.underHeaderSep = "╋";
        this.underHeaderStartSep = "┣";
        this.underHeaderEndSep = "┫";
        this.footSep = "┻";
        this.footStartSep = "┗";
        this.footEndSep = "┛";
        this.dataFiller = " ";
        this.dataSep = "┃";
        this.dataStartSep = "┃";
        this.dataEndSep = "┃";

    }

    /**
     * 样式3:
     * -------------------
     * ID   NAME   AGE
     * -------------------
     * 1    Jack   23
     * 2    Lucy   18
     * 3    Tom    35
     * -------------------
     */
    public void styleThree() {
        this.headerEntry = "-";
        this.onHeaderSep = "-";
        this.onHeaderStartSep = "-";
        this.onHeaderEndSep = "-";
        this.underHeaderSep = "-";
        this.underHeaderStartSep = "-";
        this.underHeaderEndSep = "-";
        this.footSep = "-";
        this.footStartSep = "-";
        this.footEndSep = "-";
        this.dataFiller = " ";
        this.dataSep = " ";
        this.dataStartSep = " ";
        this.dataEndSep = " ";
    }

    /**
     * 样式4:
     * ╔════╦══════╦═════╗
     * ║ ID ║ NAME ║ AGE ║
     * ╠════╬══════╬═════╣
     * ║ 1  ║ Jack ║ 23  ║
     * ║ 2  ║ Lucy ║ 18  ║
     * ║ 3  ║ Tom  ║ 35  ║
     * ╚════╩══════╩═════╝
     */
    public void styleFour() {
        this.headerEntry = "═";
        this.onHeaderSep = "╦";
        this.onHeaderStartSep = "╔";
        this.onHeaderEndSep = "╗";
        this.underHeaderSep = "╬";
        this.underHeaderStartSep = "╠";
        this.underHeaderEndSep = "╣";
        this.footSep = "╩";
        this.footStartSep = "╚";
        this.footEndSep = "╝";
        this.dataFiller = " ";
        this.dataSep = "║";
        this.dataStartSep = "║";
        this.dataEndSep = "║";

    }

    /**
     * 样式5:
     * -------------------
     * | ID | NAME | AGE |
     * -------------------
     * | 1  | Jack | 23  |
     * | 2  | Lucy | 18  |
     * | 3  | Tom  | 35  |
     * -------------------
     */
    public void styleFive() {
        this.headerEntry = "-";
        this.onHeaderSep = "-";
        this.onHeaderStartSep = "-";
        this.onHeaderEndSep = "-";
        this.underHeaderSep = "-";
        this.underHeaderStartSep = "-";
        this.underHeaderEndSep = "-";
        this.footSep = "-";
        this.footStartSep = "-";
        this.footEndSep = "-";
        this.dataFiller = " ";
        this.dataSep = "|";
        this.dataStartSep = "|";
        this.dataEndSep = "|";

    }

    /**
     * 样式6:
     * ID  NAME    AGE
     * 1   Jack    23
     * 2   Lucy    18
     * 3   Tom     35
     */
    public void styleSix() {
        this.headerEntry = "";
        this.onHeaderSep = "";
        this.onHeaderStartSep = "";
        this.onHeaderEndSep = "";
        this.underHeaderSep = "";
        this.underHeaderStartSep = "";
        this.underHeaderEndSep = "";
        this.footSep = "";
        this.footStartSep = "";
        this.footEndSep = "";
        this.dataFiller = " ";
        this.dataSep = "";
        this.dataStartSep = "";
        this.dataEndSep = "";

    }


    /**
     * 样式7:
     * ╭───────────────────╮
     * │ ID   NAME     AGE │
     * │───────────────────│
     * │ 1    Jack\n   23  │
     * │ 2    Lucy     18  │
     * │ 3    Tom      35  │
     * ╰───────────────────╯
     */
    public void styleSeven() {
        this.headerEntry = "─";
        this.onHeaderSep = "─";
        this.onHeaderStartSep = "╭";
        this.onHeaderEndSep = "╮";
        this.underHeaderSep = "─";
        this.underHeaderStartSep = "│";
        this.underHeaderEndSep = "│";
        this.footSep = "─";
        this.footStartSep = "╰";
        this.footEndSep = "╯";
        this.dataFiller = " ";
        this.dataSep = " ";
        this.dataStartSep = "│";
        this.dataEndSep = "│";

    }


    public void setHeaderEntry(String headerEntry) {
        this.headerEntry = headerEntry;
    }

    public void setOnHeaderSep(String onHeaderSep) {
        this.onHeaderSep = onHeaderSep;
    }

    public void setOnHeaderStartSep(String onHeaderStartSep) {
        this.onHeaderStartSep = onHeaderStartSep;
    }

    public void setOnHeaderEndSep(String onHeaderEndSep) {
        this.onHeaderEndSep = onHeaderEndSep;
    }

    public void setUnderHeaderSep(String underHeaderSep) {
        this.underHeaderSep = underHeaderSep;
    }

    public void setUnderHeaderStartSep(String underHeaderStartSep) {
        this.underHeaderStartSep = underHeaderStartSep;
    }

    public void setUnderHeaderEndSep(String underHeaderEndSep) {
        this.underHeaderEndSep = underHeaderEndSep;
    }

    public void setFootSep(String footSep) {
        this.footSep = footSep;
    }

    public void setFootStartSep(String footStartSep) {
        this.footStartSep = footStartSep;
    }

    public void setFootEndSep(String footEndSep) {
        this.footEndSep = footEndSep;
    }

    public void setDataSep(String dataSep) {
        this.dataSep = dataSep;
    }

    public void setDataFiller(String dataFiller) {
        this.dataFiller = dataFiller;
    }

    public void setDataStartSep(String dataStartSep) {
        this.dataStartSep = dataStartSep;
    }

    public void setDataEndSep(String dataEndSep) {
        this.dataEndSep = dataEndSep;
    }

    public void setHeader(List<String> tableHeaders) {
        header.clear();
        header.addAll(tableHeaders);
    }

    public void setHeader(String... headerNames) {
        header.clear();
        header.addAll(Arrays.asList(headerNames));
    }

    public void addHeader(String... headerNames) {
        header.addAll(Arrays.asList(headerNames));
    }

    public String removeHeader(int index) {
        return header.remove(index);
    }

    public boolean removeHeader(String headerName) {
        return header.remove(headerName);
    }

    public int getDataSize() {
        return dataRows.size();
    }

    public void setDataRow(List<List<Object>> dataRows) {
        this.dataRows.clear();
        this.dataRows.addAll(dataRows);
    }

    public int addDataRow(Object... dataRow) {
        dataRows.add(Stream.of(dataRow).map(data -> String.valueOf(data)
                        .replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t"))
                .collect(Collectors.toList()));
        return dataRows.size() - 1;
    }

    public List<Object> removeDataRow(int index) {
        return dataRows.remove(index);
    }

    public void clear() {
        header.clear();
        dataRows.clear();
    }

    public Iterator<Table> paging(long pageSize) {
        return new Iterator<Table>() {

            private int startIndex = 0;

            @Override
            public boolean hasNext() {
                return startIndex < dataRows.size();
            }

            @Override
            public Table next() {
                Table table = new Table();
                table.setHeader(header);
                List<List<Object>> tableData = dataRows.stream().skip(startIndex).limit(pageSize).collect(Collectors.toList());
                table.setDataRow(tableData);
                startIndex += tableData.size();
                return table;
            }
        };
    }

    public Table getTable(long skip, long size) {
        Table table = new Table();
        table.setHeader(this.header);
        table.setDataRow(dataRows.stream().skip(skip).limit(size).collect(Collectors.toList()));
        return table;
    }

    /**
     * 获取表格样式的字符串
     *
     * @return 表格样式的字符串
     */
    public String format() {
        return format(header, dataRows);
    }

    /**
     * 获取表格样式的字符串，并将表格整体右移动
     *
     * @param rightShift 右移单位
     * @return 表格样式的字符串
     */
    public String format(String rightShift) {
        return rightShift + format().replaceAll("\n", "\n" + rightShift);
    }

    /**
     * 获取表格样式的字符串，并将表格整体右移动n个制表单位
     * @param unit 制表单位个数
     * @return 表格样式的字符串
     */
    public String formatAndRightShift(int unit) {
        StringBuilder rightShift = new StringBuilder();
        for (int i = 0; i < unit; i++) {
            rightShift.append("\t");
        }
        return format(rightShift.toString());
    }

    public String format(long skip, long size) {
        return getTable(skip, size).format();
    }


    private String format(List<String> tableHeader, List<List<Object>> tableData) {
        // 数据长度格式化
        List<List<String>> tableList = new ArrayList<>(tableData.size() + 1);
        int rowNum = tableHeader.size();
        tableList.add(tableHeader);
        for (List<Object> rowData : tableData) {
            tableList.add(getFormatList(rowData, rowNum));
        }

        List<TempPair<Integer, Boolean>> pairList = getMaxEntryLengthAndHaveFullWidthString(tableList, rowNum);

        String headerOnPart = getHeaderOnPart(pairList);
        String headerUnderPart = getHeaderUnderPart(pairList);
        String footPart = getFootPart(pairList);

        StringBuilder table = new StringBuilder();
        table.append(headerOnPart).append("\n")
                .append(getContextLine(tableHeader, pairList)).append("\n")
                .append(headerUnderPart).append("\n");
        tableList.remove(0);
        for (List<String> rowData : tableList) {
            table.append(getContextLine(rowData, pairList)).append("\n");
        }
        table.append(footPart).append("\n");
        return table.toString();
    }

    private String getHeaderOnPart(List<TempPair<Integer, Boolean>> pairList) {
        StringBuilder headerSeparator = new StringBuilder();
        int pairListSize = pairList.size();
        for (int i = 0; i < pairListSize; i++) {
            StringBuilder temp = new StringBuilder();
            TempPair<Integer, Boolean> currPair = pairList.get(i);
            if (i == 0) {
                temp.append(onHeaderStartSep)
                        .append(headerEntry)
                        .append(StringUtils.stringCopy(headerEntry, currPair.getOne(), ""))
                        .append(headerEntry)
                        .append(onHeaderSep);
            } else if (i == pairListSize - 1) {
                temp.append(headerEntry)
                        .append(StringUtils.stringCopy(headerEntry, currPair.getOne(), ""))
                        .append(headerEntry)
                        .append(onHeaderEndSep);
            } else {
                temp.append(headerEntry)
                        .append(StringUtils.stringCopy(headerEntry, currPair.getOne(), ""))
                        .append(headerEntry)
                        .append(onHeaderSep);
            }
            String tempString = currPair.getTwo() ? StringUtils.toFullWidth(temp.toString()) : temp.toString();
            headerSeparator.append(tempString);
        }
        return headerSeparator.toString();
    }

    private String getHeaderUnderPart(List<TempPair<Integer, Boolean>> pairList) {
        StringBuilder headerSeparator = new StringBuilder();
        int pairListSize = pairList.size();
        for (int i = 0; i < pairListSize; i++) {
            StringBuilder temp = new StringBuilder();
            TempPair<Integer, Boolean> currPair = pairList.get(i);
            if (i == 0) {
                temp.append(underHeaderStartSep)
                        .append(headerEntry)
                        .append(StringUtils.stringCopy(headerEntry, currPair.getOne(), ""))
                        .append(headerEntry)
                        .append(underHeaderSep);
            } else if (i == pairListSize - 1) {
                temp.append(headerEntry)
                        .append(StringUtils.stringCopy(headerEntry, currPair.getOne(), ""))
                        .append(headerEntry)
                        .append(underHeaderEndSep);
            } else {
                temp.append(headerEntry)
                        .append(StringUtils.stringCopy(headerEntry, currPair.getOne(), ""))
                        .append(headerEntry)
                        .append(underHeaderSep);
            }
            String tempString = currPair.getTwo() ? StringUtils.toFullWidth(temp.toString()) : temp.toString();
            headerSeparator.append(tempString);
        }
        return headerSeparator.toString();
    }

    private String getFootPart(List<TempPair<Integer, Boolean>> pairList) {
        StringBuilder headerSeparator = new StringBuilder();
        int pairListSize = pairList.size();
        for (int i = 0; i < pairListSize; i++) {
            StringBuilder temp = new StringBuilder();
            TempPair<Integer, Boolean> currPair = pairList.get(i);
            if (i == 0) {
                temp.append(footStartSep)
                        .append(headerEntry)
                        .append(StringUtils.stringCopy(headerEntry, currPair.getOne(), ""))
                        .append(headerEntry)
                        .append(footSep);
            } else if (i == pairListSize - 1) {
                temp.append(headerEntry)
                        .append(StringUtils.stringCopy(headerEntry, currPair.getOne(), ""))
                        .append(headerEntry)
                        .append(footEndSep);
            } else {
                temp.append(headerEntry)
                        .append(StringUtils.stringCopy(headerEntry, currPair.getOne(), ""))
                        .append(headerEntry)
                        .append(footSep);
            }
            String tempString = currPair.getTwo() ? StringUtils.toFullWidth(temp.toString()) : temp.toString();
            headerSeparator.append(tempString);
        }
        return headerSeparator.toString();
    }

    private String getContextLine(List<String> dataList, List<TempPair<Integer, Boolean>> pairList) {
        StringBuilder contextLine = new StringBuilder();
        int pairListSize = pairList.size();
        for (int i = 0; i < pairListSize; i++) {
            StringBuilder temp = new StringBuilder();
            TempPair<Integer, Boolean> currPair = pairList.get(i);
            if (i == 0) {
                temp.append(dataStartSep)
                        .append(dataFiller)
                        .append(dataList.get(i))
                        .append(StringUtils.stringCopy(dataFiller, currPair.getOne() - dataList.get(i).length(), ""))
                        .append(dataFiller)
                        .append(dataSep);
            } else if (i == pairListSize - 1) {
                temp.append(dataFiller)
                        .append(dataList.get(i))
                        .append(StringUtils.stringCopy(dataFiller, currPair.getOne() - dataList.get(i).length(), ""))
                        .append(dataFiller)
                        .append(dataEndSep);
            } else {
                temp.append(dataFiller)
                        .append(dataList.get(i))
                        .append(StringUtils.stringCopy(dataFiller, currPair.getOne() - dataList.get(i).length(), ""))
                        .append(dataFiller)
                        .append(dataSep);
            }
            String tempString = currPair.getTwo() ? StringUtils.toFullWidth(temp.toString()) : temp.toString();
            contextLine.append(tempString);
        }
        return contextLine.toString();
    }

    private List<TempPair<Integer, Boolean>> getMaxEntryLengthAndHaveFullWidthString(List<List<String>> dataList, int maxEntryLength) {

        Map<Integer, TempPair<Boolean, List<String>>> columnMap = new HashMap<>();
        int size = dataList.size();

        for (List<String> rowData : dataList) {
            for (int i = 0; i < maxEntryLength; i++) {
                String data = rowData.get(i);
                TempPair<Boolean, List<String>> pair = columnMap.computeIfAbsent(i, k -> TempPair.of(false, new ArrayList<>(size)));
                if (!pair.getOne() && StringUtils.containsFullWidth(data)) {
                    pair.setOne(true);
                }
                pair.getTwo().add(data);
            }
        }
        return columnMap.values().stream().map((p) -> TempPair.of(p.getTwo().stream().mapToInt(String::length).max().getAsInt(), p.getOne())).collect(Collectors.toList());
    }

    private List<String> getFormatList(List<Object> dataList, int maxEntryLength) {
        List<String> formatList = new ArrayList<>(maxEntryLength);
        int dataSize = ContainerUtils.isEmptyCollection(dataList) ? 0 : dataList.size();

        for (int i = 0; i < maxEntryLength; i++) {
            if (i < dataSize) {
                formatList.add(String.valueOf(dataList.get(i)));
            } else {
                formatList.add(" ");
            }
        }
        return formatList;
    }

    public void createData(@NonNull Object obj) {
        addHeader("fieldName", "fieldValue");
        for (Field field : ClassUtils.getAllFields(obj.getClass())) {
            addDataRow(getLineString(field.getName()), getLineString(FieldUtils.getValue(obj, field)));
        }
    }

    public void createDataByMap(Map<?, ?> map) {
        addHeader("key", "value");
        map.forEach((k, v) -> addDataRow(getLineString(k), getLineString(v)));
    }

    public void createDataByArray(Object[] dataArray) {
        Class<?> componentType = dataArray.getClass().getComponentType();
        Field[] allFields = ClassUtils.getAllFields(componentType);

        addHeader("_index_");
        for (Field field : allFields) {
            addHeader(getLineString(field.getName()));
        }

        int i = 1;
        for (Object data : dataArray) {
            List<Object> dataRows = new ArrayList<>(allFields.length);
            dataRows.add(i++);
            for (Field field : allFields) {
                dataRows.add(getLineString(FieldUtils.getValue(data, field)));
            }
            addDataRow(dataRows.toArray(new Object[0]));
        }
    }

    public void createDateByCollection(Collection<?> collection) {
        if (ContainerUtils.isEmptyCollection(collection)) return;

        Class<?> componentType = null;
        for (Object data : collection) {
            componentType = data.getClass();
            break;
        }

        Field[] allFields = ClassUtils.getAllFields(componentType);

        addHeader("_index_");
        for (Field field : allFields) {
            addHeader(getLineString(field.getName()));
        }

        int i = 1;
        for (Object data : collection) {
            List<Object> dataRows = new ArrayList<>(allFields.length);
            dataRows.add(i++);
            for (Field field : allFields) {
                dataRows.add(getLineString(FieldUtils.getValue(data, field)));
            }
            addDataRow(dataRows.toArray(new Object[0]));
        }
    }

    private String getLineString(Object data) {
        return String.valueOf(data)
                .replaceAll("\n", "\\\\n")
                .replaceAll("\t", "\\\\t")
                .replaceAll("\r", "\\\\r");
    }


    public static void main(String[] args) {
        Table table = new Table();
        table.styleSeven();

        table.addHeader("ID", "NAME", "AGE");
//        table.setHeader("", "", "");

        table.addDataRow(1, "Jack\n", 23);
        table.addDataRow(2, "Lucy", 18);
        table.addDataRow(3, "Tom", 35);

        System.out.println(table.format());
        table.addDataRow(4, "如来佛祖", "不晓得好多岁");

        Iterator<Table> iterator = table.paging(3);
        while (iterator.hasNext()) {
            System.out.println(iterator.next().formatAndRightShift(2));
        }

        table.clear();

        table.setHeader("id", "key", "value", "describe");
        table.addDataRow("1", "jdbc-url", "jdbc:mysql://127.0.0.1:3306/jack-db?useUnicode=true&characterEncoding=utf-8", "数据库链接");
        table.addDataRow("2", "driver-class-name", "com.mysql.cj.jdbc.Driver", "数据库驱动");
        table.addDataRow("3", "username", "root", "数据库用户");
        table.addDataRow("4", "password", "ygrmytlFK7075", "DATABASE SECRET");
        table.addDataRow("5", "connection-init-sql", "SELECT 2", "TEST SQL");
        System.out.println(table.format());
        table.clear();

    }


}
