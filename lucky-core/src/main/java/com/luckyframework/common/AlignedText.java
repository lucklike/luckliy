package com.luckyframework.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 行首前缀 &amp; 列对齐工具类，逐行添加多列数据，输出时按列对齐并在每一行行首统一添加前缀字符。
 *
 * <p>与 {@link Table} 不同，本类只做轻量的列对齐与行首前缀处理，不依赖任何表格渲染逻辑。
 *
 * <p>使用方式：每次 {@code add} 添加一行（一行的各列以可变参数传入），最后调用
 * {@link #format(String)} 得到按列对齐、且每行带上行首前缀的字符串。
 *
 * <p>处理约定：
 * <ul>
 *     <li>列宽按每列所有内容的最大显示宽度对齐（中英文/全角按显示宽度计算，复用 {@link StringUtils#displayWidth}）</li>
 *     <li>列间以单个空格分隔，最后一列不做右侧填充（避免行尾出现悬空空格）</li>
 *     <li>单元格内嵌的换行（【\r\n】/【\r】/【\n】）会被拆成多行、顶部对齐，因此列对齐不会被破坏</li>
 *     <li>单元格末尾的换行不会产生悬空地占位行</li>
 *     <li>行首前缀为 null 或空串时不加任何前缀字符；输出各行以【\n】连接，末尾不带换行符</li>
 * </ul>
 * </p>
 *
 * @author fk7075
 * @version 3.0.0
 * @date 2026/10/14
 */
public class AlignedText {

    /**
     * 换行分割正则，兼容 \r\n、\r、\n 三种换行风格
     */
    private static final String LINE_BREAK_REGEX = "\r\n|\r|\n";

    /**
     * 每一行的列内容集合
     */
    private final List<List<String>> rows = new ArrayList<>();

    /**
     * 添加一行，可变参数中的每个元素代表该行的一个单元格
     *
     * <p>例如：{@code add("1", "Jack")} 添加一行两列；元素为 null 时按空串处理
     *
     * @param cells 该行的各列内容，允许为 null 或包含 null 元素
     * @return 当前实例，支持链式调用
     */
    public AlignedText add(Object... cells) {
        List<String> row = new ArrayList<>();
        if (cells != null) {
            for (Object cell : cells) {
                row.add(cell == null ? "" : cell.toString());
            }
        }
        rows.add(row);
        return this;
    }

    /**
     * 清空已添加的所有行
     *
     * @return 当前实例，支持链式调用
     */
    public AlignedText clear() {
        rows.clear();
        return this;
    }

    /**
     * 已添加的行数
     *
     * @return 行数
     */
    public int size() {
        return rows.size();
    }

    /**
     * 返回按列对齐后的字符串，各行不加行首前缀
     *
     * @return 对齐后的文本
     */
    public String format() {
        return format("");
    }

    /**
     * 返回按列对齐后的字符串，并在每一行行首统一添加给定前缀
     *
     * <p>例如：<br/>
     * {@code add("1", "Jack").add("2", "Lucky").format("| ")}<br/>
     * ==> {@code "| 1 Jack\n| 2 Lucky"}
     *
     * @param linePrefix 需要添加到每行行首的字符串，为 null 视为空串
     * @return 按列对齐且每行加上前缀后的文本
     */
    public String format(String linePrefix) {
        if (rows.isEmpty()) {
            return "";
        }
        String prefix = linePrefix == null ? "" : linePrefix;

        // 列数以各行中最大的列数为准，不足的列按空串补齐
        int columnCount = 0;
        for (List<String> row : rows) {
            columnCount = Math.max(columnCount, row.size());
        }
        if (columnCount == 0) {
            return "";
        }

        // 将每个单元格拆成物理行，同时统计每一列的最大显示宽度
        List<List<List<String>>> cellLines = new ArrayList<>(rows.size());
        int[] widths = new int[columnCount];
        for (List<String> row : rows) {
            List<List<String>> rowCellLines = new ArrayList<>(columnCount);
            for (int c = 0; c < columnCount; c++) {
                String cell = c < row.size() ? row.get(c) : "";
                List<String> physicalLines = splitLines(cell);
                for (String line : physicalLines) {
                    int width = StringUtils.displayWidth(line);
                    if (width > widths[c]) {
                        widths[c] = width;
                    }
                }
                rowCellLines.add(physicalLines);
            }
            cellLines.add(rowCellLines);
        }

        List<String> outputLines = new ArrayList<>();
        for (List<List<String>> rowCellLines : cellLines) {
            // 行高取该行各单元格的最大子行数，未使用的子行以空串占位（顶部对齐）
            int height = 1;
            for (List<String> physicalLines : rowCellLines) {
                height = Math.max(height, physicalLines.size());
            }
            for (int line = 0; line < height; line++) {
                StringBuilder sb = new StringBuilder(prefix);
                for (int c = 0; c < columnCount; c++) {
                    List<String> physicalLines = rowCellLines.get(c);
                    String segment = line < physicalLines.size() ? physicalLines.get(line) : "";
                    if (c == columnCount - 1) {
                        // 最后一列不做右侧填充，避免行尾悬空空格
                        sb.append(segment);
                    } else {
                        sb.append(padToWidth(segment, widths[c])).append(" ");
                    }
                }
                outputLines.add(sb.toString());
            }
        }
        return String.join("\n", outputLines);
    }

    /**
     * 左侧对齐填充：用空格将内容补齐到指定的显示宽度
     *
     * @param value 单元格内容
     * @param width 目标显示宽度
     * @return 补齐后的内容
     */
    private String padToWidth(String value, int width) {
        int diff = width - StringUtils.displayWidth(value);
        if (diff <= 0) {
            return value;
        }
        StringBuilder sb = new StringBuilder(value);
        for (int i = 0; i < diff; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * 将单元格内容拆分为多个物理行，并丢弃末尾换行产生的悬空空行
     *
     * @param cell 单元格内容
     * @return 物理行列表，至少包含一个元素（空串表示空行）
     */
    private List<String> splitLines(String cell) {
        String trimmed = cell;
        while (trimmed.endsWith("\n") || trimmed.endsWith("\r")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return Arrays.asList(trimmed.split(LINE_BREAK_REGEX, -1));
    }

    /**
     * 预览测试入口，逐一验证 add 与 format 在常规与边界场景下的输出。
     * 由于空格不可见，输出会用【&lt;】和【&gt;】包裹每一行便于核对列对齐与行首前缀。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        print("1. 基础用法：按列对齐 + 行首前缀",
                new AlignedText().add("1", "Jack").add("2", "Lucky").format("| "));

        print("2. 列宽自适应对齐（ID列、NAME列按最大宽度对齐）",
                new AlignedText().add("10", "LongName").add("2", "X").add("3", "Bob").format(""));

        print("3. 中英文混排按显示宽度对齐",
                new AlignedText().add("名字", "Jack").add("年龄", "23").format("· "));

        print("4. 单元格内嵌换行拆多行、顶部对齐不破坏列对齐",
                new AlignedText().add("a", "L1\nL2").add("bb", "R").format("> "));

        print("5. 各行列数不一致：少的列自动补空",
                new AlignedText().add("1", "A", "X").add("2", "B").format("| "));

        print("6. 空实例与 null 前缀",
                new AlignedText().add("only").format(null));

        System.out.println();
        System.out.println("==================== 7. 空内容 ====================");
        System.out.println("<" + new AlignedText().format("| ") + ">");
    }

    private static void print(String title, String text) {
        System.out.println();
        System.out.println("==================== " + title + " ====================");
        for (String line : text.split("\n", -1)) {
            System.out.println("<" + line + ">");
        }
    }
}
