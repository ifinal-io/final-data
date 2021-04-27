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

package org.ifinalframework.data.mybatis.sql;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.data.mybatis.mapper.AbsMapper;
import org.ifinalframework.data.mybatis.sql.provider.ScriptSqlProvider;
import org.ifinalframework.data.query.sql.AnnotationQueryProvider;
import org.ifinalframework.data.query.sql.DefaultQueryProvider;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.query.Query;
import org.ifinalframework.query.QueryProvider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author likly
 * @version 1.0.0
 * @see AbsMapper
 * @since 1.0.0
 */
public interface AbsMapperSqlProvider extends ScriptSqlProvider {

    default Class<?> getEntityClass(final Class<?> mapper) {

        final Type[] interfaces = mapper.getGenericInterfaces();
        for (Type type : interfaces) {
            if (type instanceof ParameterizedType && Repository.class
                .isAssignableFrom((Class) ((ParameterizedType) type).getRawType())) {
                return (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[1];

            }
        }

        throw new IllegalArgumentException("can not find entity from mapper of " + mapper.getCanonicalName());
    }

    default QueryProvider query(String expression, Class<? extends IEntity> entity, Class<?> query) {
        return new AnnotationQueryProvider(expression, entity, query);
    }

    default QueryProvider query(Query query) {
        return new DefaultQueryProvider(query);
    }

    default String whereIdNotNull() {
        return "<where>${properties.idProperty.column} = #{id}</where>";
    }

    default String whereIdsNotNull() {
        return "<where>"
            + "${properties.idProperty.column}"
            + "<foreach collection=\"ids\" item=\"id\" open=\" IN (\" separator=\",\" close=\")\">#{id}</foreach>"
            + "</where>";
    }

}

