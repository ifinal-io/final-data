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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.ibatis.builder.annotation.ProviderContext;

import org.springframework.lang.NonNull;

import org.ifinalframework.core.IQuery;
import org.ifinalframework.data.annotation.Metadata;
import org.ifinalframework.data.mybatis.mapper.AbsMapper;
import org.ifinalframework.data.mybatis.sql.AbsMapperSqlProvider;
import org.ifinalframework.data.query.DefaultQEntityFactory;
import org.ifinalframework.query.QEntity;
import org.ifinalframework.query.QProperty;
import org.ifinalframework.util.Asserts;
import org.ifinalframework.velocity.Velocities;

/**
 * @author ilikly
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

    public String selectIds(final ProviderContext context, final Map<String, Object> parameters) {
        return provide(context, parameters);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void doProvide(final StringBuilder sql, final ProviderContext context,
                          final Map<String, Object> parameters) {


        final Class<?> entity = getEntityClass(context.getMapperType());
        final QEntity<?, ?> properties = DefaultQEntityFactory.INSTANCE.create(entity);
        final String mapperMethodName = context.getMapperMethod().getName();
        parameters.put("entity", properties);
        parameters.put("properties", properties);
        if (!parameters.containsKey("columns") || Objects.isNull(parameters.get("columns"))) {
            if ("selectIds".equals(mapperMethodName)) {
                parameters.put("columns", Collections.singletonList(properties.getIdProperty().getColumn()));
            } else {
                parameters.put("columns", buildColumns(properties, parameters.containsKey("view") ? (Class<?>) parameters.get("view") : null));
            }
        }

        sql.append("<trim prefix=\"SELECT\" suffixOverrides=\",\">");
        sql.append("<foreach item=\"column\" collection=\"columns\" separator=\",\">${column}</foreach>");
//        appendColumns(sql, properties);
        sql.append("</trim>");
        sql.append("<trim prefix=\"FROM\">")
                .append("${table}")
                .append("</trim>");

        Object query = parameters.get(QUERY_PARAMETER_NAME);

        if (SELECT_ONE_METHOD_NAME.equals(mapperMethodName) && parameters.get("id") != null) {
            // <where> id = #{id} </where>
            sql.append(whereIdNotNull());
        } else if (SELECT_METHOD_NAME.equals(mapperMethodName) && parameters.get("ids") != null) {
            sql.append(whereIdsNotNull());
        } else {

            appendQuery(sql, entity, query, SELECT_ONE_METHOD_NAME.equals(mapperMethodName));
        }

    }

    private List<String> buildColumns(QEntity<?, ?> entity, Class<?> view) {
        return entity.stream()
                .filter(QProperty::isReadable)
                .filter(it -> it.hasView(view))
                .map(property -> {
                    final Metadata metadata = new Metadata();
                    metadata.setProperty(property.getName());
                    metadata.setColumn(property.getColumn());
                    metadata.setValue(property.getName());
                    metadata.setJavaType(property.getType());
                    metadata.setTypeHandler(property.getTypeHandler());
                    final String reader = Asserts.isBlank(property.getReader()) ? DEFAULT_READER : property.getReader();
                    return Velocities.getValue(reader, metadata);
                }).collect(Collectors.toList());
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
                    metadata.setTypeHandler(property.getTypeHandler());

                    final String reader = Asserts.isBlank(property.getReader()) ? DEFAULT_READER : property.getReader();

                    sql.append(Velocities.getValue(reader, metadata));
                    sql.append(",</if>");
                });
    }

}

