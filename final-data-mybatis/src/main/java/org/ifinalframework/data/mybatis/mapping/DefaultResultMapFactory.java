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

package org.ifinalframework.data.mybatis.mapping;

import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandler;
import org.ifinalframework.data.annotation.Json;
import org.ifinalframework.data.annotation.Reference;
import org.ifinalframework.data.annotation.ReferenceMode;
import org.ifinalframework.data.annotation.UpperCase;
import org.ifinalframework.data.mapping.Entity;
import org.ifinalframework.data.mapping.Property;
import org.ifinalframework.data.mapping.converter.NameConverterRegistry;
import org.ifinalframework.data.mybatis.handler.EnumTypeHandler;
import org.ifinalframework.data.mybatis.handler.JsonTypeReferenceTypeHandler;
import org.ifinalframework.data.mybatis.handler.sharing.LocalDateTimeTypeHandler;
import org.ifinalframework.data.query.TypeHandlers;
import org.ifinalframework.data.query.type.JsonParameterTypeHandler;
import org.springframework.lang.NonNull;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * DefaultResultMapFactory.
 *
 * @author ilikly
 * @version 1.2.2
 * @since 1.2.2
 */
public class DefaultResultMapFactory implements ResultMapFactory {

    private final Map<Class<?>, ResultMap> cache = new ConcurrentHashMap<>();

    @Override
    @NonNull
    public ResultMap create(@NonNull Configuration configuration, @NonNull Class<?> clazz) {

        return cache.computeIfAbsent(clazz, key -> {
            final String id = clazz.getCanonicalName();

            Entity<?> entity = Entity.from(clazz);

            final List<ResultMapping> resultMappings = entity.stream()
                    .filter(it -> !it.isTransient() && !it.isVirtual() && !it.isWriteOnly())
                    .map(property -> {
                        final Class<?> type = property.getType();
                        if (property.isAssociation()) {

                            final Reference reference = property.getRequiredAnnotation(Reference.class);
                            final Entity<?> referenceEntity = Entity.from(type);
                            final List<ResultMapping> composites = Arrays.stream(reference.properties())
                                    .map(referenceEntity::getPersistentProperty)
                                    .map(referenceProperty -> {

                                        final String name = referenceProperty.getName();
                                        String column = formatColumn(entity, property, referenceProperty);
                                        final TypeHandler<?> typeHandler = findTypeHandler(configuration, referenceProperty);

                                        return new ResultMapping.Builder(configuration, name, column, type)
                                                .javaType(referenceProperty.getJavaType())
                                                .flags(referenceProperty.isIdProperty() ? Collections.singletonList(ResultFlag.ID)
                                                        : Collections.emptyList())
                                                .typeHandler(typeHandler)
                                                .build();
                                    })
                                    .collect(Collectors.toList());

                            final String name = property.getName();

                            return new ResultMapping.Builder(configuration, name)
                                    .column(formatColumn(entity, null, property))
                                    .javaType(type)
                                    .flags(property.isIdProperty() ? Collections.singletonList(ResultFlag.ID)
                                            : Collections.emptyList())
                                    .composites(composites)
                                    .nestedResultMapId(id + "[" + name + "]")
                                    .build();

                        } else {
                            final String name = property.getName();
                            final String column = formatColumn(entity, null, property);

                            final TypeHandler<?> typeHandler = findTypeHandler(configuration, property);

                            return new ResultMapping.Builder(configuration, name, column, type)
                                    .flags(property.isIdProperty() ? Collections.singletonList(ResultFlag.ID)
                                            : Collections.emptyList())
                                    .typeHandler(typeHandler)
                                    .build();
                        }

                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return new ResultMap.Builder(configuration, id, entity.getType(), resultMappings)
                    .build();
        });

    }

    private String formatColumn(final Entity<?> entity, final Property property,
                                final Property referenceProperty) {

        String column;
        if (property == null) {
            column = referenceProperty.getColumn();
        } else {
            final String referenceColumn = property.getReferenceColumn(referenceProperty);
            column = referenceProperty.isIdProperty() && property.getReferenceMode() == ReferenceMode.SIMPLE
                    ? property.getColumn()
                    : property.getColumn() + referenceColumn.substring(0, 1).toUpperCase() + referenceColumn.substring(1);
        }
        column = NameConverterRegistry.getInstance().getColumnNameConverter().convert(column);
        if (Optional.ofNullable(property).orElse(referenceProperty).isAnnotationPresent(UpperCase.class) || entity
                .isAnnotationPresent(UpperCase.class)) {
            column = column.toUpperCase();
        }
        return column;
    }

    private TypeHandler<?> findTypeHandler(final Configuration configuration, final Property property) {

        Field field = property.getField();
        Objects.requireNonNull(field);

        try {

            Class<? extends TypeHandler<?>> typeHandler = TypeHandlers.findTypeHandler(property);

            if (JsonParameterTypeHandler.class.equals(typeHandler)) {
                return new JsonTypeReferenceTypeHandler<>(field.getGenericType());
            }

            if (typeHandler != null && !TypeHandler.class.equals(typeHandler)) {
                return typeHandler.getConstructor().newInstance();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        if (LocalDateTime.class.equals(field.getType())) {
            return new LocalDateTimeTypeHandler();
        }

        if (property.isAnnotationPresent(Json.class) || property.isCollectionLike() || property.isMap()) {
            Objects.requireNonNull(field);
            return new JsonTypeReferenceTypeHandler<>(field.getGenericType());
        }
        if (property.isEnum()) {
            final Class<? extends Enum<?>> type = (Class<? extends Enum<?>>) property.getType();
            return new EnumTypeHandler<>(type);
        }
        return configuration.getTypeHandlerRegistry().getTypeHandler(property.getType());
    }
}
