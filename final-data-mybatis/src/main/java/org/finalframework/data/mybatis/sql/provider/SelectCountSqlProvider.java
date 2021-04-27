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

package org.finalframework.data.mybatis.sql.provider;

import org.finalframework.core.IEntity;
import org.finalframework.core.IQuery;
import org.finalframework.data.mybatis.sql.AbsMapperSqlProvider;
import org.finalframework.query.Query;
import org.finalframework.query.QueryProvider;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.ibatis.builder.annotation.ProviderContext;

/**
 * @author likly
 * @version 1.0.0
 * @see org.finalframework.data.mybatis.mapper.AbsMapper#selectCount(String, Collection, IQuery)
 * @since 1.0.0
 */

public class SelectCountSqlProvider implements AbsMapperSqlProvider, ScriptSqlProvider {

    private static final String QUERY = "query";

    @SuppressWarnings("unused")
    public String selectCount(final ProviderContext context, final Map<String, Object> parameters) {

        return provide(context, parameters);
    }

    @Override
    public void doProvide(final StringBuilder sql, final ProviderContext context,
        final Map<String, Object> parameters) {

        Object ids = parameters.get("ids");
        Object query = parameters.get(QUERY);

        final Class<?> entity = getEntityClass(context.getMapperType());

        sql.append("<trim prefix=\"SELECT COUNT(*) FROM\">${table}</trim>");

        if (ids != null) {
            sql.append(whereIdsNotNull());
        } else if (query instanceof Query) {
            QueryProvider provider = query((Query) query);

            Optional.ofNullable(provider.where()).ifPresent(sql::append);
            Optional.ofNullable(provider.orders()).ifPresent(sql::append);
            Optional.ofNullable(provider.limit()).ifPresent(sql::append);
        } else if (query != null) {

            final QueryProvider provider = query(QUERY, (Class<? extends IEntity<?>>) entity, query.getClass());

            if (Objects.nonNull(provider.where())) {
                sql.append(provider.where());
            }

            if (Objects.nonNull(provider.orders())) {
                sql.append(provider.orders());
            }

            if (Objects.nonNull(provider.limit())) {
                sql.append(provider.limit());
            }
        }

    }

}

