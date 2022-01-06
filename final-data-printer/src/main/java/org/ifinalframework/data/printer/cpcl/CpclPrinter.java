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

    public CpclPrinter start(Integer offset, Integer height, Integer qty) {
        ps.println(commands("!", offset, 200, 200, height, qty));
        return this;
    }

    public CpclPrinter text(Integer font, Integer size, Integer x, Integer y, String data) {
        ps.println(commands("T", font, size, x, y, data));
        return this;
    }

    public CpclPrinter vtext(Integer font, Integer size, Integer x, Integer y, String data) {
        ps.println(commands("VT", font, size, x, y, data));
        return this;
    }

    public CpclPrinter text90(Integer font, Integer size, Integer x, Integer y, String data) {
        ps.println(commands("T90", font, size, x, y, data));
        return this;
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

    public void print() {
        ps.println("PRINT");
    }

    private String commands(Object... commands) {
        return Arrays.stream(commands).map(String::valueOf).collect(Collectors.joining(" "));
    }
}
