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

package org.ifinalframework.data.query.sql;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ibatis.type.TypeHandler;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;

import org.ifinalframework.core.Groupable;
import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.Limitable;
import org.ifinalframework.core.Orderable;
import org.ifinalframework.data.mapping.Entity;
import org.ifinalframework.data.mapping.Property;
import org.ifinalframework.data.query.DefaultQEntityFactory;
import org.ifinalframework.data.query.criterion.CriterionHandlerRegistry;
import org.ifinalframework.data.util.TenantUtils;
import org.ifinalframework.data.query.AndOr;
import org.ifinalframework.data.query.CriterionAttributes;
import org.ifinalframework.data.query.QEntity;
import org.ifinalframework.data.query.QEntityFactory;
import org.ifinalframework.data.query.annotation.Criteria;
import org.ifinalframework.data.query.annotation.Criterion;
import org.ifinalframework.data.query.annotation.CriterionSqlProvider;
import org.ifinalframework.data.query.annotation.Or;
import org.ifinalframework.data.query.annotation.function.Function;
import org.ifinalframework.util.Asserts;

import lombok.extern.slf4j.Slf4j;

/**
 * @author ilikly
 * @version 1.0.0
 * @see Orderable
 * @see Groupable
 * @see Limitable
 * @since 1.0.0
 */
@Slf4j
public final class AnnotationQueryProvider extends AbsQueryProvider {

    public static final String FORMAT = "%s.%s";

    private static final Set<String> IGNORE_ATTRIBUTES = Stream.of(
            CriterionAttributes.ATTRIBUTE_NAME_PROPERTY,
            CriterionAttributes.ATTRIBUTE_NAME_VALUE
    ).collect(Collectors.toSet());

    private final QEntityFactory entityFactory = DefaultQEntityFactory.INSTANCE;

    private final Class<?> query;

    private final String where;


    public AnnotationQueryProvider(final String expression, final Class<? extends IEntity> entity, final Class query) {

        this.query = query;

        final StringBuilder whereBuilder = new StringBuilder();
        whereBuilder.append("<where>");
        final QEntity<?, ?> properties = entityFactory.create(entity);

        if (TenantUtils.isTenant(entity)) {
            whereBuilder.append("<if test=\"properties.hasTenantProperty() and tenant != null\">")
                    .append("${properties.tenantProperty.column} = #{tenant} ")
                    .append("</if>");
        }


        appendCriteria(whereBuilder, expression, properties, query,
                AnnotatedElementUtils.isAnnotated(query, Or.class) ? AndOr.OR : AndOr.AND);

        final Map<Integer, String> orderFragments = new LinkedHashMap<>();
        CriterionSqlProvider criterionSqlProvider = CriterionHandlerRegistry.getInstance()
                .get(CriterionSqlProvider.class);
        whereBuilder.append("</where>");

        this.where = whereBuilder.toString();

    }


    private void appendCriteria(final StringBuilder sql, final String expression, final QEntity<?, ?> entity,
                                final Class<?> query, final AndOr andOr) {

        Entity.from(query)
                .forEach(property -> {
                    if (property.isAnnotationPresent(Criterion.class)) {
                        Class<? extends Annotation> annotation = property.getRequiredAnnotation(Criterion.class).value();
                        Field field = property.getField();
                        Objects.requireNonNull(field, "property filed can not be null:" + property.getName());

                        AnnotationAttributes criterionAttributes = AnnotatedElementUtils
                                .getMergedAnnotationAttributes(field, annotation);

                        Objects.requireNonNull(criterionAttributes,
                                "not found annotation of @" + annotation.getSimpleName() + " at " + query.getSimpleName() + "."
                                        + property.getName());

                        final CriterionAttributes metadata = new CriterionAttributes();

                        final String path = criterionAttributes.containsKey(CriterionAttributes.ATTRIBUTE_NAME_PROPERTY)
                                && Asserts.nonBlank(criterionAttributes.getString(CriterionAttributes.ATTRIBUTE_NAME_PROPERTY))
                                ? criterionAttributes.getString(CriterionAttributes.ATTRIBUTE_NAME_PROPERTY)
                                : property.getName();

                        metadata.put(CriterionAttributes.ATTRIBUTE_NAME_AND_OR, andOr);
                        metadata.put(CriterionAttributes.ATTRIBUTE_NAME_QUERY, expression);
                        metadata
                                .put(CriterionAttributes.ATTRIBUTE_NAME_PROPERTY,
                                        String.format(FORMAT, expression, property.getName()));
                        metadata
                                .put(CriterionAttributes.ATTRIBUTE_NAME_COLUMN, entity.getRequiredProperty(path).getColumn());
                        metadata.put(CriterionAttributes.ATTRIBUTE_NAME_VALUE,
                                String.format(FORMAT, expression, property.getName()));

                        CriterionSqlProvider criterionSqlProvider = CriterionHandlerRegistry.getInstance()
                                .get(CriterionSqlProvider.class);

                        // process @Function annotation
                        if (property.isAnnotationPresent(Function.class)) {
                            AnnotationAttributes functionAttributes = findFunctionAnnotationAttributes(property);
                            Objects.requireNonNull(functionAttributes);
                            appendAnnotationAttributesToMetadata(functionAttributes, metadata);
                            criterionSqlProvider.function(functionAttributes, metadata);
                        }

                        appendAnnotationAttributesToMetadata(criterionAttributes, metadata);

                        // process @Criterion annotation
                        final String value = criterionSqlProvider.provide(criterionAttributes, metadata);
                        sql.append(value);
                    } else if (property.isAnnotationPresent(Criteria.class)) {
                        sql.append("<if test=\"").append(expression).append(".").append(property.getName())
                                .append(" != null\">");
                        sql.append("<trim prefix=\" ").append(andOr.name())
                                .append(" (\" suffix=\")\" prefixOverrides=\"AND |OR \">");
                        appendCriteria(sql, expression + "." + property.getName(), entity, property.getType(),
                                property.isAnnotationPresent(Or.class) ? AndOr.OR : AndOr.AND);
                        sql.append("</trim>");
                        sql.append("</if>");
                    }
                });
    }

    private AnnotationAttributes findFunctionAnnotationAttributes(final Property property) {
        final Function function = property.getRequiredAnnotation(Function.class);
        return AnnotatedElementUtils.getMergedAnnotationAttributes(property.getField(), function.value());

    }

    private void appendAnnotationAttributesToMetadata(final AnnotationAttributes annotationAttributes,
                                                      final CriterionAttributes metadata) {
        //append annotation attributes
        for (Map.Entry<String, Object> entry : annotationAttributes.entrySet()) {
            if (IGNORE_ATTRIBUTES.contains(entry.getKey())
                    || (CriterionAttributes.ATTRIBUTE_NAME_JAVA_TYPE.equals(entry.getKey()) && Object.class
                    .equals(entry.getValue()))
                    || (CriterionAttributes.ATTRIBUTE_NAME_TYPE_HANDLER.equals(entry.getKey()) && TypeHandler.class
                    .equals(entry.getValue()))) {
                continue;
            }
            metadata.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String where() {
        return this.where;
    }



}

