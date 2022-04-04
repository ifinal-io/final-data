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

package org.ifinalframework.data.query;

import org.springframework.lang.NonNull;

import org.ifinalframework.data.annotation.Table;
import org.ifinalframework.data.annotation.View;
import org.ifinalframework.data.mapping.Entity;
import org.ifinalframework.data.mapping.MappingUtils;
import org.ifinalframework.data.mapping.converter.NameConverterRegistry;
import org.ifinalframework.query.QEntity;
import org.ifinalframework.query.QProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * @author ilikly
 * @version 1.0.0
 * @since 1.0.0
 */
public class AbsQEntity<I extends Serializable, T> implements QEntity<I, T> {

    private final List<QProperty<?>> properties = new ArrayList<>();

    private final Map<String, QProperty<?>> pathProperties = new HashMap<>();

    private final Class<T> type;

    private final String table;

    private QProperty<?> idProperty;

    private QProperty<?> versionProperty;

    public AbsQEntity(final Class<T> type) {

        this(type, NameConverterRegistry.getInstance().getTableNameConverter().convert(
                type.getAnnotation(Table.class) == null || type.getAnnotation(Table.class).value().isEmpty()
                        ? type.getSimpleName()
                        : type.getAnnotation(Table.class).value()
        ));
    }

    public AbsQEntity(final Class<T> type, final String table) {

        this.type = type;
        this.table = table;
        this.initProperties();
    }

    protected void initProperties() {
        Entity<T> entity = Entity.from(type);

        entity.stream()
                .filter(it -> !it.isTransient())
                .forEach(property -> {

                    final View view = property.findAnnotation(View.class);
                    final List<Class<?>> views = Optional.ofNullable(view).map(value -> Arrays.asList(value.value()))
                            .orElse(null);
                    final int order = property.getOrder();
                    if (property.isReference()) {

                        final Entity<?> referenceEntity = Entity.from(property.getType());

                        AtomicInteger index = new AtomicInteger();

                        property.getReferenceProperties()
                                .stream()
                                .map(referenceEntity::getRequiredPersistentProperty)
                                .forEach(referenceProperty -> addProperty(

                                        new QPropertyImpl.Builder<>(this, referenceProperty)
                                                .order(order + index.getAndIncrement())
                                                .path(property.getName() + "." + referenceProperty.getName())
                                                .name(MappingUtils.formatPropertyName(property, referenceProperty))
                                                .column(MappingUtils.formatColumn(entity, property, referenceProperty))
                                                .insert(referenceProperty.getInsert())
                                                .update(referenceProperty.getUpdate())
                                                .views(views)
                                                .readable(true)
                                                .writeable(property.isWriteable())
                                                .modifiable(property.isModifiable())
                                                .typeHandler(TypeHandlers.findTypeHandler(referenceProperty))
                                                .build()
                                ));

                    } else {

                        addProperty(
                                new QPropertyImpl.Builder<>(this, property)
                                        .order(order)
                                        .path(property.getName())
                                        .name(property.getName())
                                        .column(MappingUtils.formatColumn(entity, property, null))
                                        .insert(property.getInsert())
                                        .update(property.getUpdate())
                                        .idProperty(property.isIdProperty())
                                        .readable(!property.isTransient() && !property.isVirtual() && !property.isWriteOnly())
                                        .writeable(property.isWriteable())
                                        .modifiable(property.isModifiable())
                                        .typeHandler(TypeHandlers.findTypeHandler(property))
                                        .views(views)
                                        .build()
                        );
                    }
                });
        this.properties.sort(Comparator.comparing(QProperty::getOrder));
    }

    private void addProperty(final QProperty<?> property) {

        this.properties.add(property);
        this.pathProperties.put(property.getPath(), property);
        if (property.isIdProperty()) {
            this.idProperty = property;
        } else if (property.isVersionProperty()) {
            this.versionProperty = property;
        }
    }

    @Override
    public String getTable() {
        return this.table;
    }

    @Override
    public Class<T> getType() {
        return this.type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public QProperty<I> getIdProperty() {
        return (QProperty<I>) this.idProperty;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> QProperty<E> getVersionProperty() {
        return (QProperty<E>) this.versionProperty;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> QProperty<E> getProperty(final String path) {

        return (QProperty<E>) pathProperties.get(path);
    }

    @Override
    @NonNull
    public Iterator<QProperty<?>> iterator() {
        return properties.iterator();
    }

    public Stream<QProperty<?>> stream() {
        return properties.stream();
    }

}
