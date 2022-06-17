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

package org.apache.ibatis.reflection.property;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;

import org.ifinalframework.data.mybatis.reflection.FinalPropertyTokenizer;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * PropertyTokenizerTest.
 *
 * @author ilikly
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class PropertyTokenizerTest {


    @Test
    void test(){
        PropertyTokenizer tokenizer = new PropertyTokenizer("arr[0][0]");

        List<List<Integer>> arr = Arrays.asList(Arrays.asList(1));

        MetaObject metaObject = MetaObject.forObject(Collections.singletonMap("arr", arr), new DefaultObjectFactory(), new DefaultObjectWrapperFactory(), new DefaultReflectorFactory());
        Object objectValue = metaObject.getValue("arr[0]");
        FinalPropertyTokenizer propertyTokenizer = new FinalPropertyTokenizer("arr[0][0]");
        metaObject.metaObjectForProperty(propertyTokenizer.getIndexedName());

        Assertions.assertEquals(1,1);

    }
}


