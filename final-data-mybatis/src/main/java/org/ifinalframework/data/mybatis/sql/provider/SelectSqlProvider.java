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

package org.ifinalframework.data.mybatis.sql.provider;

import org.springframework.lang.NonNull;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.data.annotation.Metadata;
import org.ifinalframework.data.mybatis.mapper.AbsMapper;
import org.ifinalframework.data.mybatis.sql.AbsMapperSqlProvider;
import org.ifinalframework.data.query.DefaultQEntityFactory;
import org.ifinalframework.query.QEntity;
import org.ifinalframework.query.QProperty;
import org.ifinalframework.query.Query;
import org.ifinalframework.query.QueryProvider;
import org.ifinalframework.util.Asserts;
import org.ifinalframework.velocity.Velocities;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.ibatis.builder.annotation.ProviderContext;
import org.apache.ibatis.type.TypeHandler;

/**
 * @author likly
 * @version 1.0.0
 * @see AbsMapper#select(String, Class, Collection, IQuery)
 * @see AbsMapper#selectOne(String, Class, Serializable, IQuery)
 * @since 1.0.0
 */
public class SelectSqlProvider implements AbsMapperSqlProvider {

    public static final String QUERY_PARAMETER_NAME = "query";

    private static final String SELECT_METHOD_NAME = "select";

    private static final String SELECT_ONE_METHOD_NAME = "selectOne";

    private static final String DEFAULT_READER = "${column}";

    public String select(final ProviderContext context, final Map<String, Object> parameters) {

        return provide(context, parameters);
    }

    public String selectOne(final ProviderContext context, final Map<String, Object> parameters) {

        return provide(context, parameters);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void doProvide(final StringBuilder sql, final ProviderContext context,
        final Map<String, Object> parameters) {

        final Class<?> entity = getEntityClass(context.getMapperType());
        final QEntity<?, ?> properties = DefaultQEntityFactory.INSTANCE.create(entity);
        parameters.put("entity", properties);

        sql.append("<trim prefix=\"SELECT\" suffixOverrides=\",\">");
        appendColumns(sql, properties);
        sql.append("</trim>");
        sql.append("<trim prefix=\"FROM\">")
            .append("${table}")
            .append("</trim>");

        Object query = parameters.get(QUERY_PARAMETER_NAME);

        if (SELECT_ONE_METHOD_NAME.equals(context.getMapperMethod().getName()) && parameters.get("id") != null) {
            // <where> id = #{id} </where>
            sql.append("<where>${properties.idProperty.column} = #{id}</where>");
        } else if (SELECT_METHOD_NAME.equals(context.getMapperMethod().getName()) && parameters.get("ids") != null) {
            sql.append(whereIdsNotNull());
        } else if (query instanceof Query) {
            QueryProvider provider = query((Query) query);

            Optional.ofNullable(provider.where()).ifPresent(sql::append);
            Optional.ofNullable(provider.groups()).ifPresent(sql::append);
            Optional.ofNullable(provider.orders()).ifPresent(sql::append);
            Optional.ofNullable(provider.limit()).ifPresent(sql::append);
        } else if (query != null) {

            final QueryProvider provider = query(QUERY_PARAMETER_NAME, (Class<? extends IEntity<?>>) entity,
                query.getClass());

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

    private void appendColumns(final @NonNull StringBuilder sql, final @NonNull QEntity<?, ?> entity) {

        entity.stream()
            .filter(QProperty::isReadable)
            .forEach(property -> {
                sql.append("<if test=\"entity.getRequiredProperty('")
                    .append(property.getPath())
                    .append("').hasView(view)\">");

                final Metadata metadata = new Metadata();

                metadata.setProperty(property.getName());
                metadata.setColumn(property.getColumn());
                metadata.setValue(property.getName());
                metadata.setJavaType(property.getType());
                metadata.setTypeHandler((Class<? extends TypeHandler>) property.getTypeHandler());

                final String reader = Asserts.isBlank(property.getReader()) ? DEFAULT_READER : property.getReader();

                sql.append(Velocities.getValue(reader, metadata));
                sql.append(",</if>");
            });
    }

}

