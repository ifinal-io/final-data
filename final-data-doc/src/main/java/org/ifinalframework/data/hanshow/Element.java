package org.ifinalframework.data.hanshow;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.ifinalframework.core.IEnum;
import org.ifinalframework.data.hanshow.element.BarcodeElement;
import org.ifinalframework.data.hanshow.element.NumberElement;
import org.ifinalframework.data.hanshow.element.QRCodeElement;
import org.ifinalframework.data.hanshow.element.TextElement;

import javax.validation.constraints.NotNull;

/**
 * @author likly
 * @version 1.2.4
 **/
@Setter
@Getter
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "content_type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextElement.class,name = "CONTENT_TYPE_TEXT"),
        @JsonSubTypes.Type(value = BarcodeElement.class,name = "CONTENT_TYPE_BARCODE"),
        @JsonSubTypes.Type(value = QRCodeElement.class,name = "CONTENT_TYPE_QRCODE"),
        @JsonSubTypes.Type(value = NumberElement.class,name = "CONTENT_TYPE_NUMBER")
})
public abstract class Element {
    /**
     * 图层起点 X 坐标
     */
    @NotNull
    @JsonProperty("start_x")
    private Integer startX;
    /**
     * 图层起点 Y 坐标
     */
    @NotNull
    @JsonProperty("start_y")
    private Integer startY;
    /**
     * 图层终点 X 坐标
     */
    @NotNull
    @JsonProperty("end_x")
    private Integer endX;
    /**
     * 图层终点 Y 坐标
     */
    @NotNull
    @JsonProperty("end_y")
    private Integer endY;
    /**
     * 是否反显
     */
    @JsonProperty("content_reverse")
    private Boolean reverse;


    @Getter
    @AllArgsConstructor
    public enum Type implements IEnum<String> {
        /**
         * 显示数字
         */
        NUMBER("CONTENT_TYPE_NUMBER","数字"),
        /**
         * CURRENCY
         */
//        CURRENCY,
        /**
         * 显示日期
         */
//        DATA,
        /**
         * 显示时间
         */
//        TIME,
        /**
         * 显示文本
         */
        TEXT("CONTENT_TYPE_TEXT","文本");
        /**
         * 显示图片
         */
//        BPM_FILE,
        /**
         * 显示二位码
         */
//        QRCODE,
        /**
         * BARCODE
         */
//        BARCODE,
        /**
         * 显示直线
         */
//        LINE;
        private final String code;
        private final String desc;
    }

    public enum Color {
        WHITE,BLACK,RED
    }

    public enum Alignment{
        LEFT,RIGHT,CENTER
    }

    public enum VerticalAlignment {
        TOP,BOTTOM,MIDDLE
    }




}
