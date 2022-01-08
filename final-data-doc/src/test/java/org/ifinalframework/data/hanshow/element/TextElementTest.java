package org.ifinalframework.data.hanshow.element;

import org.ifinalframework.data.hanshow.Element;
import org.ifinalframework.json.Json;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author likly
 * @version 1.2.4
 **/
class TextElementTest {
    @Test
    void test(){
        TextElement element = new TextElement();
        String json = Json.toJson(element);
        assertTrue(json.contains("CONTENT_TYPE_TEXT"));
        Element object = Json.toObject(json, Element.class);
        assertTrue(object instanceof TextElement);
    }


}