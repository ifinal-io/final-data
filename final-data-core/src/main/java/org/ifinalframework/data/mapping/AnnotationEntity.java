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

package org.ifinalframework.data.mapping;

import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import org.ifinalframework.util.Reflections;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ilikly
 * @version 1.0.0
 * @since 1.0.0
 */
public class AnnotationEntity<T> extends BasicPersistentEntity<T, Property> implements Entity<T> {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationEntity.class);

    private final List<Property> properties = new ArrayList<>();

    private AnnotationEntity(final TypeInformation<T> information) {

        super(information);
    }

    public AnnotationEntity(final Class<T> entityClass) {

        this(ClassTypeInformation.from(entityClass));
        init();
    }

    private void init() {
        initProperties();
    }

    private void initProperties() {
        try {
            final Class<?> entityClass = getType();
            BeanInfo beanInfo = Introspector.getBeanInfo(entityClass);
            Arrays.stream(beanInfo.getPropertyDescriptors())
                .filter(it -> !"class".equals(it.getName()))
                .map(it -> buildProperty(entityClass, it))
                .forEach(it -> {
                    addPersistentProperty(it);
                    properties.add(it);
                });

            this.properties.sort(Comparator.comparing(Property::getOrder));

        } catch (IntrospectionException e) {
            logger.error("", e);
        }
    }

    private Property buildProperty(final Class<?> entityClass, final PropertyDescriptor descriptor) {

        final Field field = Reflections.findField(entityClass, descriptor.getName());
        return field == null
            ? new AnnotationProperty(
            org.springframework.data.mapping.model.Property.of(getTypeInformation(), descriptor), this,
            SimpleTypeHolder.DEFAULT)
            : new AnnotationProperty(
                org.springframework.data.mapping.model.Property.of(getTypeInformation(), field, descriptor), this,
                SimpleTypeHolder.DEFAULT);
    }

    @Override
    public Stream<Property> stream() {
        return properties.stream();
    }

}
