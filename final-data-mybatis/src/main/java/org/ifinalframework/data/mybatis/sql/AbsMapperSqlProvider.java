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

import java.util.Optional;

import org.springframework.core.ResolvableType;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.data.mybatis.mapper.AbsMapper;
import org.ifinalframework.data.mybatis.sql.provider.ScriptSqlProvider;
import org.ifinalframework.data.query.sql.AnnotationQueryProvider;
import org.ifinalframework.data.query.sql.DefaultQueryProvider;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.query.Query;
import org.ifinalframework.query.QueryProvider;

/**
 * @author ilikly
 * @version 1.0.0
 * @see AbsMapper
 * @since 1.0.0
 */
public interface AbsMapperSqlProvider extends ScriptSqlProvider {

    default Class<?> getEntityClass(final Class<?> mapper) {
        return ResolvableType.forClass(mapper).as(Repository.class).resolveGeneric(1);
    }

    default QueryProvider query(String expression, Class<?> entity, Class<?> query) {
        return new AnnotationQueryProvider(expression, (Class<? extends IEntity>) entity, query);
    }

    default QueryProvider query(Query query) {
        return new DefaultQueryProvider(query);
    }

    default void appendQuery(StringBuilder sql, Class<?> entity, Object query) {

        QueryProvider provider = null;

        if (query instanceof Query) {
            provider = query((Query) query);
        } else if (query != null) {
            provider = query("query", entity, query.getClass());
        }

        Optional.ofNullable(provider)
                .ifPresent(it -> {
                    Optional.ofNullable(it.where()).ifPresent(sql::append);
                    Optional.ofNullable(it.groups()).ifPresent(sql::append);
                    Optional.ofNullable(it.orders()).ifPresent(sql::append);
                    Optional.ofNullable(it.limit()).ifPresent(sql::append);
                });
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

