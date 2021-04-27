/*
 * Copyright 2020-2021 the original author or authors.
 *
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

package org.ifinalframework.data.mybatis.mapper;

import java.lang.annotation.Annotation;

import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

/**
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
public enum MethodMethod {
    INSERT("insert", InsertProvider.class),
    REPLACE("replace", InsertProvider.class),
    SAVE("save", InsertProvider.class),
    UPDATE("update", UpdateProvider.class),
    DELETE("delete", DeleteProvider.class),
    SELECT("select", SelectProvider.class),
    SELECT_ONE("selectOne", SelectProvider.class),
    SELECT_IDS("selectIds", SelectProvider.class),
    SELECT_COUNT("selectCount", SelectProvider.class),
    TRUNCATE("truncate", UpdateProvider.class);

    private final String method;

    private final Class<? extends Annotation> annotation;

    MethodMethod(final String method, final Class<? extends Annotation> annotation) {

        this.method = method;
        this.annotation = annotation;
    }

}
