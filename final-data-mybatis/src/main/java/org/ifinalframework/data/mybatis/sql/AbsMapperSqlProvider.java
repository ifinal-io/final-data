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
import java.util.Objects;
import java.util.Optional;

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

    default QueryProvider query(String expression, Class<?> entity, Class<?> query) {
        return new AnnotationQueryProvider(expression, (Class<? extends IEntity>) entity, query);
    }

    default QueryProvider query(Query query) {
        return new DefaultQueryProvider(query);
    }

    default void appendQuery(StringBuilder sql, Class<?> entity, Object query) {
        if (query instanceof Query) {
            QueryProvider provider = query((Query) query);

            Optional.ofNullable(provider.where()).ifPresent(sql::append);
            Optional.ofNullable(provider.groups()).ifPresent(sql::append);
            Optional.ofNullable(provider.orders()).ifPresent(sql::append);
            Optional.ofNullable(provider.limit()).ifPresent(sql::append);
        } else if (query != null) {

            final QueryProvider provider = query("query", entity, query.getClass());

            if (Objects.nonNull(provider.where())) {
                sql.append(provider.where());
            }

            if (Objects.nonNull(provider.groups())) {
                sql.append(provider.groups());
            }

            if (Objects.nonNull(provider.orders())) {
                sql.append(provider.orders());
            }

            if (Objects.nonNull(provider.limit())) {
                sql.append(provider.limit());
            }
        }
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

