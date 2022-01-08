package org.ifinalframework.data.hanshow.element;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.ifinalframework.data.hanshow.Element;

import javax.validation.constraints.NotNull;

/**
 * @author likly
 * @version 1.2.4
 **/
@Setter
@Getter
public class TextElement extends Element {

    /**
     * 字体类型
     * 字体名称后可添加三个属性：Regular/BOLD/ITALIC
     * 字体文件放置在${eslworking.home}/data/usr/fonts。
     * 如果找不到设置的字体文件，将使用系统默认字体。
     */
    @NotNull
    @JsonProperty("font_type")
    private String fontType;
    /**
     * 图层内容
     */
    @NotNull
    @JsonProperty("content_title")
    private String title;
    /**
     * 内容值
     */
    @JsonProperty("content_value")
    private String value;
    /**
     * 字体颜色
     * 设置为 RED 时，需要有红屏硬件才能显示。如果不支持的硬件默认显示黑色。
     */
    @NotNull
    @JsonProperty("content_color")
    private Color color = Color.BLACK;
    /**
     * 对齐方式
     */
    @JsonProperty("content_alignment")
    private Alignment alignment;
    /**
     * 文本垂直对齐方式
     */
    @JsonProperty("content_vertical_alignment")
    private VerticalAlignment verticalAlignment;
    /**
     * 文本是否自
     * 动换行
     */
    private Boolean autoWrap;

}
