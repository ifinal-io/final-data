/*
 * Copyright 2020-2022 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ifinalframework.data.printer.cpcl;

import javax.validation.constraints.Max;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * CpclPrinter.
 *
 * @author likly
 * @version 1.2.4
 * @since 1.2.4
 */
public class CpclPrinter {
    private final PrintStream ps;

    public CpclPrinter(PrintStream ps) {
        this.ps = ps;
    }

    /**
     * 开始命令
     *
     * @param offset 开始命令
     * @param height 开始命令
     * @param qty    打印标签的数量，最大 1024 张
     */
    public CpclPrinter start(int offset, int height, @Max(1024) int qty) {
        ps.println(commands("!", offset, 200, 200, height, qty));
        return this;
    }

    public CpclPrinter text(int font, int size, int x, int y, String data) {
        text("T", font, size, x, y, data);
        return this;
    }

    public CpclPrinter vtext(int font, int size, int x, int y, String data) {
        text("VT", font, size, x, y, data);
        ps.println(commands("VT", font, size, x, y, data));
        return this;
    }

    public CpclPrinter text90(int font, int size, int x, int y, String data) {
        text("T90", font, size, x, y, data);
        return this;
    }

    public CpclPrinter text180(int font, int size, int x, int y, String data) {
        text("T180", font, size, x, y, data);
        return this;
    }

    public CpclPrinter text270(int font, int size, int x, int y, String data) {
        text("T270", font, size, x, y, data);
        return this;
    }

    /**
     * 文本命令
     * <pre class="code>
     * {command} {font} {size} {x} {y} {data}
     * </pre>
     *
     * @param command 指令
     * @param font    字体号
     * @param size    字体大小
     * @param x       水平打印起始位置
     * @param y       垂直打印起始位置
     * @param data    打印的文本内容
     * @see #text(int, int, int, int, String)
     * @see #text90(int, int, int, int, String)
     * @see #text180(int, int, int, int, String)
     * @see #text270(int, int, int, int, String)
     */
    private void text(String command, int font, int size, int x, int y, String data) {
        ps.println(commands(command, font, size, x, y, data));
    }

    public CpclPrinter barcode(String type, int width, int ratio, int height, int x, int y, String data) {
        ps.println(commands("B", type, width, ratio, height, x, y, data));
        return this;
    }

    public CpclPrinter vbarcode(String type, int width, int ratio, int height, int x, int y, String data) {
        ps.println(commands("VB", type, width, ratio, height, x, y, data));
        return this;
    }

    /**
     * 图形命令
     *
     * @param width  图像宽度，以字节为单位
     * @param height 图像宽度，以字节为单位
     * @param x      水平起始位置
     * @param y      垂直起始位置
     * @param data   垂直起始位置
     */
    public CpclPrinter expandedGraphics(int width, int height, int x, int y, String data) {
        ps.println(commands("EG", width, height, x, y, data));
        return this;
    }

    public CpclPrinter compressedGraphics(int width, int height, int x, int y, String data) {
        ps.println(commands("CG", width, height, x, y, data));
        return this;
    }

    public CpclPrinter form() {
        ps.println("FORM");
        return this;
    }

    /**
     * 打印命令.
     *
     * <pre class="code">
     * PRINT
     * </pre>
     * <p>
     * 使用 FORM 指令后，打印机将走纸到下一标签的起始位置，打印机找下一标签的起始位置是根据两个 标签之间的缝隙判断的。
     */
    public void print() {
        ps.println("PRINT");
    }

    private String commands(Object... commands) {
        return Arrays.stream(commands).map(String::valueOf).collect(Collectors.joining(" "));
    }
}
