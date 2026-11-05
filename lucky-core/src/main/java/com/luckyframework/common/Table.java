package com.luckyframework.common;

import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import org.springframework.lang.NonNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 表格类，将数据格式化为一张表格
 *
 * <p>使用约束与说明：
 * <ul>
 *     <li>渲染时以表头列数为准，数据行的列数不能超过表头列数，超过时将抛出异常；不足时自动使用空格补齐</li>
 *     <li>未设置表头时列数按数据行的最大列数自动推导，不足的行自动补空列，并跳过表头内容行与其下的分隔线；表头与数据均为空时输出空字符串</li>
 *     <li>数据中的【\n】（含【\r\n】、【\r】）会作为单元格内换行渲染为多行，未使用的子行以空格占位（顶部对齐）；【\t】等其他控制字符会被转义为字面量后再参与渲染</li>
 *     <li>列宽按East Asian Width显示宽度计算（中文、全角字符与Emoji按2宽度、组合字符按0宽度），中英文混排时自动使用填充符补齐对齐，数据内容保持原样不做任何转换</li>
 *     <li>为保证对齐精度，填充符建议使用单个半角空格，边框字符建议使用单个字符</li>
 *     <li>ZWJ组合的复杂Emoji序列（如家庭Emoji）与部分边缘宽字符可能存在对齐偏差</li>
 * </ul>
 * </p>
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

    /**
     * 样式8:
     * 圆角虚线风格
     * ╭┄┄┄┄┬┄┄┄┄┄┄┬┄┄┄┄┄╮
     * │ ID │ NAME │ AGE │
     * ├┄┄┄┄┼┄┄┄┄┄┄┼┄┄┄┄┄┤
     * │ 1  │ Jack │ 23  │
     * │ 2  │ Lucy │ 18  │
     * │ 3  │ Tom  │ 35  │
     * ╰┄┄┄┄┴┄┄┄┄┄┄┴┄┄┄┄┄╯
     */
    public void styleEight() {
        this.headerEntry = "┄";
        this.onHeaderSep = "┬";
        this.onHeaderStartSep = "╭";
        this.onHeaderEndSep = "╮";
        this.underHeaderSep = "┼";
        this.underHeaderStartSep = "├";
        this.underHeaderEndSep = "┤";
        this.footSep = "┴";
        this.footStartSep = "╰";
        this.footEndSep = "╯";
        this.dataFiller = " ";
        this.dataSep = "│";
        this.dataStartSep = "│";
        this.dataEndSep = "│";
    }

    /**
     * 样式9:
     * 管道网格风格（Markdown 风格的分段连接线）
     * |----|------|-----|
     * | ID | NAME | AGE |
     * |----|------|-----|
     * | 1  | Jack | 23  |
     * | 2  | Lucy | 18  |
     * |----|------|-----|
     */
    public void styleNine() {
        this.headerEntry = "-";
        this.onHeaderSep = "|";
        this.onHeaderStartSep = "|";
        this.onHeaderEndSep = "|";
        this.underHeaderSep = "|";
        this.underHeaderStartSep = "|";
        this.underHeaderEndSep = "|";
        this.footSep = "|";
        this.footStartSep = "|";
        this.footEndSep = "|";
        this.dataFiller = " ";
        this.dataSep = "|";
        this.dataStartSep = "|";
        this.dataEndSep = "|";
    }

    /**
     * 样式10:
     * 星号风格
     * *******************
     * | ID | NAME | AGE |
     * *******************
     * | 1  | Jack | 23  |
     * *******************
     */
    public void styleTen() {
        this.headerEntry = "*";
        this.onHeaderSep = "*";
        this.onHeaderStartSep = "*";
        this.onHeaderEndSep = "*";
        this.underHeaderSep = "*";
        this.underHeaderStartSep = "*";
        this.underHeaderEndSep = "*";
        this.footSep = "*";
        this.footStartSep = "*";
        this.footEndSep = "*";
        this.dataFiller = " ";
        this.dataSep = "|";
        this.dataStartSep = "|";
        this.dataEndSep = "|";
    }

    /**
     * 样式11:
     * 双线贯穿风格
     * ═══════════════════
     * │ ID │ NAME │ AGE │
     * ═══════════════════
     * │ 1  │ Jack │ 23  │
     * ═══════════════════
     */
    public void styleEleven() {
        this.headerEntry = "═";
        this.onHeaderSep = "═";
        this.onHeaderStartSep = "═";
        this.onHeaderEndSep = "═";
        this.underHeaderSep = "═";
        this.underHeaderStartSep = "═";
        this.underHeaderEndSep = "═";
        this.footSep = "═";
        this.footStartSep = "═";
        this.footEndSep = "═";
        this.dataFiller = " ";
        this.dataSep = "│";
        this.dataStartSep = "│";
        this.dataEndSep = "│";
    }

    /**
     * 样式12:
     * 波浪线风格
     * ~~~~~~~~~~~~~~~~~~~
     *  ID   NAME   AGE
     * ~~~~~~~~~~~~~~~~~~~
     *  1    Jack   23
     * ~~~~~~~~~~~~~~~~~~~
     */
    public void styleTwelve() {
        this.headerEntry = "~";
        this.onHeaderSep = "~";
        this.onHeaderStartSep = "~";
        this.onHeaderEndSep = "~";
        this.underHeaderSep = "~";
        this.underHeaderStartSep = "~";
        this.underHeaderEndSep = "~";
        this.footSep = "~";
        this.footStartSep = "~";
        this.footEndSep = "~";
        this.dataFiller = " ";
        this.dataSep = " ";
        this.dataStartSep = " ";
        this.dataEndSep = " ";
    }

    /**
     * 创建【样式1】的空白表格，等价于new Table()后调用{@link #styleOne()}
     *
     * @return 样式1的表格
     */
    public static Table ofStyleOne() {
        Table table = new Table();
        table.styleOne();
        return table;
    }

    /**
     * 创建【样式2】的空白表格，等价于new Table()后调用{@link #styleTwo()}
     *
     * @return 样式2的表格
     */
    public static Table ofStyleTwo() {
        Table table = new Table();
        table.styleTwo();
        return table;
    }

    /**
     * 创建【样式3】的空白表格，等价于new Table()后调用{@link #styleThree()}
     *
     * @return 样式3的表格
     */
    public static Table ofStyleThree() {
        Table table = new Table();
        table.styleThree();
        return table;
    }

    /**
     * 创建【样式4】的空白表格，等价于new Table()后调用{@link #styleFour()}
     *
     * @return 样式4的表格
     */
    public static Table ofStyleFour() {
        Table table = new Table();
        table.styleFour();
        return table;
    }

    /**
     * 创建【样式5】的空白表格，等价于new Table()后调用{@link #styleFive()}
     *
     * @return 样式5的表格
     */
    public static Table ofStyleFive() {
        Table table = new Table();
        table.styleFive();
        return table;
    }

    /**
     * 创建【样式6】的空白表格，等价于new Table()后调用{@link #styleSix()}
     *
     * @return 样式6的表格
     */
    public static Table ofStyleSix() {
        Table table = new Table();
        table.styleSix();
        return table;
    }

    /**
     * 创建【样式7】的空白表格，等价于new Table()后调用{@link #styleSeven()}
     *
     * @return 样式7的表格
     */
    public static Table ofStyleSeven() {
        Table table = new Table();
        table.styleSeven();
        return table;
    }

    /**
     * 创建【样式8】的空白表格，等价于new Table()后调用{@link #styleEight()}
     *
     * @return 样式8的表格
     */
    public static Table ofStyleEight() {
        Table table = new Table();
        table.styleEight();
        return table;
    }

    /**
     * 创建【样式9】的空白表格，等价于new Table()后调用{@link #styleNine()}
     *
     * @return 样式9的表格
     */
    public static Table ofStyleNine() {
        Table table = new Table();
        table.styleNine();
        return table;
    }

    /**
     * 创建【样式10】的空白表格，等价于new Table()后调用{@link #styleTen()}
     *
     * @return 样式10的表格
     */
    public static Table ofStyleTen() {
        Table table = new Table();
        table.styleTen();
        return table;
    }

    /**
     * 创建【样式11】的空白表格，等价于new Table()后调用{@link #styleEleven()}
     *
     * @return 样式11的表格
     */
    public static Table ofStyleEleven() {
        Table table = new Table();
        table.styleEleven();
        return table;
    }

    /**
     * 创建【样式12】的空白表格，等价于new Table()后调用{@link #styleTwelve()}
     *
     * @return 样式12的表格
     */
    public static Table ofStyleTwelve() {
        Table table = new Table();
        table.styleTwelve();
        return table;
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

    /**
     * 将当前表格的样式复制到目标表格
     *
     * @param target 目标表格
     */
    public void copyStyleTo(Table target) {
        target.headerEntry = this.headerEntry;
        target.onHeaderSep = this.onHeaderSep;
        target.onHeaderStartSep = this.onHeaderStartSep;
        target.onHeaderEndSep = this.onHeaderEndSep;
        target.underHeaderSep = this.underHeaderSep;
        target.underHeaderStartSep = this.underHeaderStartSep;
        target.underHeaderEndSep = this.underHeaderEndSep;
        target.footSep = this.footSep;
        target.footStartSep = this.footStartSep;
        target.footEndSep = this.footEndSep;
        target.dataFiller = this.dataFiller;
        target.dataSep = this.dataSep;
        target.dataStartSep = this.dataStartSep;
        target.dataEndSep = this.dataEndSep;
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

    /**
     * 获取表头（返回拷贝，修改返回值不会影响当前表格）
     *
     * @return 表头列表
     */
    public List<String> getHeader() {
        return new ArrayList<>(header);
    }

    /**
     * 获取所有数据行（返回拷贝，修改返回值不会影响当前表格）
     *
     * @return 数据行列表
     */
    public List<List<Object>> getDataRows() {
        List<List<Object>> copy = new ArrayList<>(dataRows.size());
        for (List<Object> dataRow : dataRows) {
            copy.add(dataRow == null ? null : new ArrayList<>(dataRow));
        }
        return copy;
    }

    public int getDataSize() {
        return dataRows.size();
    }

    public void setDataRow(List<List<Object>> dataRows) {
        this.dataRows.clear();
        for (List<Object> dataRow : dataRows) {
            this.dataRows.add(toTableRow(dataRow));
        }
    }

    public int addDataRow(Object... dataRow) {
        dataRows.add(toTableRow(Arrays.asList(dataRow)));
        return dataRows.size() - 1;
    }

    /**
     * 将一行原始数据转换为表格内部存储格式：元素统一转换为字符串并转义控制字符
     *
     * @param dataRow 原始行数据
     * @return 转换后的行数据
     */
    private List<Object> toTableRow(List<?> dataRow) {
        if (dataRow == null) {
            return null;
        }
        return dataRow.stream().map(this::getLineString).collect(Collectors.toList());
    }

    public List<Object> removeDataRow(int index) {
        return dataRows.remove(index);
    }

    public void clear() {
        header.clear();
        dataRows.clear();
    }

    public Iterator<Table> paging(long pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be greater than 0, but was: " + pageSize);
        }
        return new Iterator<Table>() {

            private int startIndex = 0;

            @Override
            public boolean hasNext() {
                return startIndex < dataRows.size();
            }

            @Override
            public Table next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Table table = new Table();
                copyStyleTo(table);
                table.setHeader(header);
                List<List<Object>> tableData = dataRows.stream().skip(startIndex).limit(pageSize).collect(Collectors.toList());
                table.setDataRow(tableData);
                startIndex += tableData.size();
                return table;
            }
        };
    }

    public Table getTable(long skip, long size) {
        if (skip < 0 || size < 0) {
            throw new IllegalArgumentException("skip and size must be greater than or equal to 0, but was: skip=" + skip + ", size=" + size);
        }
        Table table = new Table();
        copyStyleTo(table);
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
        String shiftedTable = rightShift + format().replace("\n", "\n" + rightShift);
        // 表格末尾自带换行符，消除换行后悬空的缩进片段
        String danglingIndent = "\n" + rightShift;
        return shiftedTable.endsWith(danglingIndent)
                ? shiftedTable.substring(0, shiftedTable.length() - rightShift.length())
                : shiftedTable;
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
        boolean hasHeader = !tableHeader.isEmpty();
        List<List<String>> tableList = new ArrayList<>(tableData.size() + 1);
        int rowNum;
        if (hasHeader) {
            rowNum = tableHeader.size();
            tableList.add(getFormatList(tableHeader, rowNum));
        } else {
            // 未设置表头时：列数按数据行的最大列数自动推导，无有效数据时输出空字符串
            rowNum = 0;
            for (List<Object> rowData : tableData) {
                if (rowData != null && rowData.size() > rowNum) {
                    rowNum = rowData.size();
                }
            }
            if (rowNum == 0) {
                return "";
            }
        }

        for (List<Object> rowData : tableData) {
            if (hasHeader && rowData != null && rowData.size() > rowNum) {
                throw new IllegalArgumentException("The number of columns in the data row [" + rowData.size() + "] exceeds the number of columns in the header [" + rowNum + "]!");
            }
            tableList.add(getFormatList(rowData, rowNum));
        }

        List<Integer> columnWidths = getColumnWidths(tableList, rowNum);

        String headerOnPart = getHeaderOnPart(columnWidths);
        String headerUnderPart = getHeaderUnderPart(columnWidths);
        String footPart = getFootPart(columnWidths);

        StringBuilder table = new StringBuilder();
        table.append(headerOnPart).append("\n");
        if (hasHeader) {
            // 表头部分组成：表头内容行与表头下的分隔线；未设置表头时这两部分不渲染，顶线作为表格上边框保留
            table.append(getContextLines(tableList.get(0), columnWidths))
                    .append(headerUnderPart).append("\n");
            tableList.remove(0);
        }
        for (List<String> rowData : tableList) {
            table.append(getContextLines(rowData, columnWidths));
        }
        table.append(footPart).append("\n");
        return table.toString();
    }

    private String getHeaderOnPart(List<Integer> columnWidths) {
        return buildSeparatorLine(columnWidths, onHeaderStartSep, onHeaderSep, onHeaderEndSep);
    }

    private String getHeaderUnderPart(List<Integer> columnWidths) {
        return buildSeparatorLine(columnWidths, underHeaderStartSep, underHeaderSep, underHeaderEndSep);
    }

    private String getFootPart(List<Integer> columnWidths) {
        return buildSeparatorLine(columnWidths, footStartSep, footSep, footEndSep);
    }

    /**
     * 构建由起始符、内容填充符、连接符与结束符组成的分隔线
     *
     * @param columnWidths 各列的显示宽度
     * @param startSep     起始符
     * @param sep          列间连接符
     * @param endSep       结束符
     * @return 分隔线字符串
     */
    private String buildSeparatorLine(List<Integer> columnWidths, String startSep, String sep, String endSep) {
        StringBuilder separatorLine = new StringBuilder();
        int columnNum = columnWidths.size();
        int fillerWidth = StringUtils.displayWidth(dataFiller);
        for (int i = 0; i < columnNum; i++) {
            StringBuilder temp = new StringBuilder();
            if (i == 0) {
                temp.append(startSep);
            }
            // 分隔线段的目标显示宽度与单元格区域（填充符+数据+补齐+填充符）保持一致
            int targetWidth = columnWidths.get(i) + 2 * fillerWidth;
            temp.append(StringUtils.stringCopy(headerEntry, entryRepeatCount(targetWidth, headerEntry), ""));
            temp.append(i == columnNum - 1 ? endSep : sep);
            separatorLine.append(temp);
        }
        return separatorLine.toString();
    }

    private String getContextLines(List<String> dataList, List<Integer> columnWidths) {
        int columnNum = columnWidths.size();
        List<List<String>> cellLines = new ArrayList<>(columnNum);
        int height = 1;
        for (String data : dataList) {
            List<String> lines = splitCellLines(data);
            cellLines.add(lines);
            if (lines.size() > height) {
                height = lines.size();
            }
        }

        StringBuilder contextLines = new StringBuilder();
        for (int row = 0; row < height; row++) {
            for (int i = 0; i < columnNum; i++) {
                // 单元格不足的子行以空串占位，保证顶部对齐与边框连续
                List<String> lines = cellLines.get(i);
                String data = row < lines.size() ? lines.get(row) : "";
                int padCount = fillerRepeatCount(columnWidths.get(i) - StringUtils.displayWidth(data));

                StringBuilder temp = new StringBuilder();
                if (i == 0) {
                    temp.append(dataStartSep);
                }
                temp.append(dataFiller)
                        .append(data)
                        .append(StringUtils.stringCopy(dataFiller, padCount, ""))
                        .append(dataFiller);
                temp.append(i == columnNum - 1 ? dataEndSep : dataSep);
                contextLines.append(temp);
            }
            contextLines.append("\n");
        }
        return contextLines.toString();
    }

    /**
     * 按换行符将单元格内容拆分为多个子行
     *
     * @param cell 单元格内容
     * @return 子行列表
     */
    private List<String> splitCellLines(String cell) {
        return Arrays.asList(cell.split("\n", -1));
    }

    /**
     * 计算单元格的显示宽度，多行单元格取其各子行的最大显示宽度
     *
     * @param cell 单元格内容
     * @return 显示宽度
     */
    private int cellDisplayWidth(String cell) {
        int maxWidth = 0;
        for (String line : splitCellLines(cell)) {
            int width = StringUtils.displayWidth(line);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        return maxWidth;
    }

    private List<Integer> getColumnWidths(List<List<String>> dataList, int columnNum) {
        int[] widthArray = new int[columnNum];
        for (List<String> rowData : dataList) {
            for (int i = 0; i < columnNum; i++) {
                int width = cellDisplayWidth(rowData.get(i));
                if (width > widthArray[i]) {
                    widthArray[i] = width;
                }
            }
        }
        List<Integer> columnWidths = new ArrayList<>(columnNum);
        for (int width : widthArray) {
            columnWidths.add(width);
        }
        return columnWidths;
    }

    /**
     * 计算分隔线中表头元素的重复次数，使分隔线的显示宽度贴合目标宽度
     *
     * @param targetWidth 目标显示宽度
     * @param entry       重复元素
     * @return 重复次数
     */
    private int entryRepeatCount(int targetWidth, String entry) {
        int entryWidth = StringUtils.displayWidth(entry);
        if (entryWidth <= 0) {
            return 0;
        }
        return Math.max(1, Math.round((float) targetWidth / entryWidth));
    }

    /**
     * 计算数据填充符的重复次数，使单元格的显示宽度补齐到列宽
     *
     * @param widthDiff 列宽与单元格显示宽度的差值
     * @return 填充符重复次数
     */
    private int fillerRepeatCount(int widthDiff) {
        int fillerWidth = StringUtils.displayWidth(dataFiller);
        if (fillerWidth <= 0 || widthDiff <= 0) {
            return 0;
        }
        return Math.round((float) widthDiff / fillerWidth);
    }

    private List<String> getFormatList(List<?> dataList, int maxEntryLength) {
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
            addDataRow(field.getName(), FieldUtils.getValue(obj, field));
        }
    }

    public void createDataByMap(Map<?, ?> map) {
        addHeader("key", "value");
        map.forEach((k, v) -> addDataRow(k, v));
    }

    public void createDataByArray(Object[] dataArray) {
        Class<?> componentType = dataArray.getClass().getComponentType();
        Field[] allFields = ClassUtils.getAllFields(componentType);

        addHeader("_index_");
        for (Field field : allFields) {
            addHeader(field.getName());
        }

        int i = 1;
        for (Object data : dataArray) {
            List<Object> dataRows = new ArrayList<>(allFields.length);
            dataRows.add(i++);
            for (Field field : allFields) {
                dataRows.add(FieldUtils.getValue(data, field));
            }
            addDataRow(dataRows.toArray(new Object[0]));
        }
    }

    public void createDataByCollection(Collection<?> collection) {
        if (ContainerUtils.isEmptyCollection(collection)) return;

        Class<?> componentType = null;
        for (Object data : collection) {
            componentType = data.getClass();
            break;
        }

        Field[] allFields = ClassUtils.getAllFields(componentType);

        addHeader("_index_");
        for (Field field : allFields) {
            addHeader(field.getName());
        }

        int i = 1;
        for (Object data : collection) {
            List<Object> dataRows = new ArrayList<>(allFields.length);
            dataRows.add(i++);
            for (Field field : allFields) {
                dataRows.add(FieldUtils.getValue(data, field));
            }
            addDataRow(dataRows.toArray(new Object[0]));
        }
    }

    /**
     * @deprecated 方法名拼写有误，请使用 {@link #createDataByCollection(Collection)}
     */
    @Deprecated
    public void createDateByCollection(Collection<?> collection) {
        createDataByCollection(collection);
    }

    private String getLineString(Object data) {
        // 【\r\n】、【\r】统一归一为【\n】作为多行分隔；【\t】转义为字面量
        return String.valueOf(data)
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\t", "\\t");
    }


    public static void main(String[] args) {
        // ==================== 1. 十二种内置样式 ====================
        // 注：【样式6】为无边框样式（分隔线为空串），渲染出的空行属于预期输出
        // 使用静态构造方法Table.ofStyleXxx()直接创建带样式的表格，等价于new Table()后调用对应的styleXxx()
        title("1. 十二种内置样式");
        Table[] styleTables = {
                Table.ofStyleOne(), Table.ofStyleTwo(), Table.ofStyleThree(),
                Table.ofStyleFour(), Table.ofStyleFive(), Table.ofStyleSix(),
                Table.ofStyleSeven(), Table.ofStyleEight(), Table.ofStyleNine(),
                Table.ofStyleTen(), Table.ofStyleEleven(), Table.ofStyleTwelve()
        };
        for (int style = 1; style <= styleTables.length; style++) {
            Table table = styleTables[style - 1];
            table.setHeader("ID", "NAME", "AGE");
            table.addDataRow(1, "Jack", 23);
            table.addDataRow(2, "Lucy", 18);
            table.addDataRow(3, "Tom", "32");

            System.out.println("--- style" + style + " ---");
            System.out.println(table.format());
        }

        // ==================== 2. 宽字符与多行单元格 ====================
        title("2. 宽字符与多行单元格");
        Table multiLineTable = new Table();
        multiLineTable.styleSeven();
        multiLineTable.setHeader("ID", "描述（表头也可多行）", "备注");
        multiLineTable.addDataRow(1, "第一行\n第二行内容", "中英文混排\nABC 123");
        multiLineTable.addDataRow(2, "Emoji 😀🚀", "组合字符 e\u0301");
        multiLineTable.addDataRow(3, "单行数据", "短");
        System.out.println(multiLineTable.format());

        // ==================== 3. 特殊值与控制字符 ====================
        title("3. 特殊值与控制字符");
        Table escapeTable = new Table();
        escapeTable.setHeader("ID", "类型", "值");
        escapeTable.addDataRow(1, "制表符", "a\tb\t制表符会被转义");
        escapeTable.addDataRow(2, "null值", null);
        escapeTable.addDataRow(3, "布尔值", true);
        escapeTable.addDataRow(4, "数字", 3.14);
        escapeTable.addDataRow(5, "长内容", "jdbc:mysql://127.0.0.1:3306/demo?useUnicode=true&characterEncoding=utf-8");
        System.out.println(escapeTable.format());

        // ==================== 4. 数据构造方式 ====================
        title("4. 数据构造方式");

        // 4.1 由Map构造（生成key/value两列）
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("driver-class-name", "com.mysql.cj.jdbc.Driver");
        config.put("jdbc-url", "jdbc:mysql://127.0.0.1:3306/demo?useUnicode=true");
        config.put("username", "root");
        config.put("password", "********");
        Table mapTable = new Table();
        mapTable.styleSeven();
        mapTable.createDataByMap(config);
        System.out.println(mapTable.format());

        // 4.2 由对象数组构造（反射字段生成表头与数据行）
        User[] users = {
                new User("Jack", 23, true),
                new User("露西", 18, false),
                new User("如来佛祖", 999, true),
        };
        Table arrayTable = new Table();
        arrayTable.createDataByArray(users);
        System.out.println(arrayTable.format());

        // 4.3 由集合构造（入口与对象数组等价）
        Table collectionTable = new Table();
        collectionTable.styleFive();
        collectionTable.createDataByCollection(Arrays.asList(users));
        System.out.println(collectionTable.format());

        // 4.4 由单个对象构造（生成fieldName/fieldValue两列）
        Table beanTable = new Table();
        beanTable.styleSeven();
        beanTable.createData(new User("如来佛祖", 999, true));
        System.out.println(beanTable.format());

        // 4.5 setDataRow批量设置数据行
        Table batchTable = new Table();
        batchTable.setHeader("序号", "内容");
        batchTable.setDataRow(Arrays.asList(
                Arrays.asList("A", "批量行1"),
                Arrays.asList("B", "批量行2"),
                Arrays.asList("C", "批量行3")));
        // addDataRow会返回新行在表格中的索引
        int newRowIndex = batchTable.addDataRow("D", "addDataRow追加");
        System.out.println(batchTable.format());
        System.out.println("addDataRow返回的新行索引: " + newRowIndex);

        // ==================== 5. 分页与切片 ====================
        title("5. 分页与切片");
        Table bigTable = new Table();
        bigTable.styleFour();
        bigTable.setHeader("ID", "USER", "STATUS");
        for (int i = 1; i <= 7; i++) {
            bigTable.addDataRow(i, "User-" + i, i % 2 == 0 ? "正常" : "冻结");
        }
        int pageNo = 1;
        Iterator<Table> pages = bigTable.paging(3);
        while (pages.hasNext()) {
            System.out.println("--- 分页(pageSize=3) 第" + pageNo++ + "页 ---");
            System.out.println(pages.next().format());
        }
        System.out.println("--- 切片 format(2, 3)：跳过前2条数据后取3条 ---");
        System.out.println(bigTable.format(2, 3));

        // ==================== 6. 缩进嵌套输出 ====================
        title("6. 缩进嵌套输出（日志/树形场景）");
        Table detailTable = new Table();
        detailTable.styleSeven();
        detailTable.setHeader("参数", "值");
        detailTable.addDataRow("timeout", "3000ms");
        detailTable.addDataRow("retry", 2);
        System.out.println("[INFO] 开始处理任务");
        System.out.println(detailTable.formatAndRightShift(1));
        System.out.println("[INFO] 子任务执行中...");
        System.out.println(detailTable.formatAndRightShift(2));
        System.out.println("[INFO] 任务完成");
        System.out.println("--- 自定义缩进前缀 format(\"> \") ---");
        System.out.println(detailTable.format("> "));

        // ==================== 7. 自定义样式配置 ====================
        title("7. 自定义样式配置");

        // 7.1 外框简约风：在样式1的基础上去掉列间竖线
        Table outlineTable = new Table();
        outlineTable.styleOne();
        outlineTable.setOnHeaderSep("");
        outlineTable.setUnderHeaderSep("");
        outlineTable.setFootSep("");
        outlineTable.setDataSep("");
        outlineTable.setHeader("ID", "NAME", "AGE");
        outlineTable.addDataRow(1, "Jack", 23);
        outlineTable.addDataRow(2, "露西", 18);
        System.out.println(outlineTable.format());

        // 7.2 无边框+自定义填充符（点线风格日志）
        Table dotsTable = new Table();
        dotsTable.styleSix();
        dotsTable.setDataFiller(".");
        dotsTable.setHeader("LEVEL", "MESSAGE");
        dotsTable.addDataRow("INFO", "服务启动完成");
        dotsTable.addDataRow("WARN", "连接池即将耗尽");
        dotsTable.addDataRow("ERROR", "请求失败\n进入重试队列");
        System.out.println(dotsTable.format());

        // ==================== 8. 复用与边界场景 ====================
        title("8. 复用与边界场景");

        // 8.1 clear复用：清空后可作为一张新表格继续使用
        Table reuseTable = new Table();
        reuseTable.styleThree();
        reuseTable.setHeader("A", "B");
        reuseTable.addDataRow("1", "复用前数据");
        System.out.println("--- clear前 ---");
        System.out.println(reuseTable.format());
        reuseTable.clear();
        reuseTable.setHeader("X", "Y");
        reuseTable.addDataRow("9", "clear后的新数据");
        System.out.println("--- clear后 ---");
        System.out.println(reuseTable.format());

        // 8.2 只有表头的空表
        Table emptyTable = new Table();
        emptyTable.setHeader("ID", "NAME", "AGE");
        System.out.println(emptyTable.format());

        // 8.3 删除表头列与数据行
        Table editTable = new Table();
        editTable.styleSeven();
        editTable.setHeader("ID", "NAME", "AGE");
        String removedHeader = editTable.removeHeader(2);
        editTable.addDataRow(1, "Jack");
        editTable.addDataRow(2, "露西");
        System.out.println("被移除的表头: " + removedHeader);
        System.out.println(editTable.format());
        List<Object> removedRow = editTable.removeDataRow(0);
        System.out.println("被移除的数据行: " + removedRow);
        System.out.println(editTable.format());

        // 8.4 数据列数超过表头列数时的预期异常
        Table invalidTable = new Table();
        invalidTable.setHeader("A", "B");
        invalidTable.addDataRow("1", "2", "3");
        try {
            invalidTable.format();
        } catch (IllegalArgumentException e) {
            System.out.println("捕获预期异常: " + e.getMessage());
        }

        // 8.5 未设置表头：列数按数据行的最大列数自动推导，短行自动补空列
        Table noHeaderTable = new Table();
        noHeaderTable.styleSeven();
        noHeaderTable.addDataRow("1", "Jack", 23);
        noHeaderTable.addDataRow("2", "露西");
        noHeaderTable.addDataRow("3", "如来佛祖", 999, "extra");
        System.out.println(noHeaderTable.format());
    }

    private static void title(String text) {
        System.out.println();
        System.out.println("==================== " + text + " ====================");
    }

    /**
     * 演示用POJO，作为createData/createDataByArray/createDataByCollection的输入类型
     */
    private static class User {

        private String name;
        private int age;
        private boolean vip;

        User(String name, int age, boolean vip) {
            this.name = name;
            this.age = age;
            this.vip = vip;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public boolean isVip() {
            return vip;
        }
    }


}
