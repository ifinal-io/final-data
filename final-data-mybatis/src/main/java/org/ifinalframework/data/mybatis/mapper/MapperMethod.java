/*
 * Copyright 2020-2021 the original author or authors.
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

import lombok.Getter;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.query.Update;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
public enum MapperMethod {
    /**
     * @see AbsMapper#insert(String, Class, boolean, Collection)
     */
    INSERT("insert", InsertProvider.class, String.class, Class.class, boolean.class, Collection.class),
    /**
     * @see AbsMapper#replace(String, Class, Collection)
     */
    REPLACE("replace", InsertProvider.class, String.class, Class.class, Collection.class),
    /**
     * @see AbsMapper#save(String, Class, Collection)
     */
    SAVE("save", InsertProvider.class, String.class, Class.class, Collection.class),
    /**
     * @see AbsMapper#update(String, Class, IEntity, Update, boolean, Collection, IQuery)
     */
    UPDATE("update", UpdateProvider.class, String.class, Class.class, IEntity.class, Update.class, boolean.class, Collection.class,
            IQuery.class),
    /**
     * @see AbsMapper#delete(String, Collection, IQuery)
     */
    DELETE("delete", DeleteProvider.class, String.class, Collection.class, IQuery.class),
    /**
     * @see AbsMapper#select(String, Class, Collection, IQuery)
     */
    SELECT("select", SelectProvider.class, String.class, Class.class, Collection.class, IQuery.class),
    /**
     * @see AbsMapper#selectOne(String, Class, Serializable, IQuery)
     */
    SELECT_ONE("selectOne", SelectProvider.class, String.class, Class.class, Serializable.class, IQuery.class),
    /**
     * @see AbsMapper#selectIds(String, IQuery)
     */
    SELECT_IDS("selectIds", SelectProvider.class, String.class, IQuery.class),
    /**
     * @see AbsMapper#selectCount(String, Collection, IQuery)
     */
    SELECT_COUNT("selectCount", SelectProvider.class, String.class, Collection.class, IQuery.class),
    /**
     * @see AbsMapper#truncate(String)
     */
    TRUNCATE("truncate", UpdateProvider.class, String.class);

    private final String method;

    private final Class<? extends Annotation> annotation;

    @Getter
    private final Class<?>[] args;

    MapperMethod(final String method, final Class<? extends Annotation> annotation, Class<?>... args) {

        this.method = method;
        this.annotation = annotation;
        this.args = args;
    }

}
