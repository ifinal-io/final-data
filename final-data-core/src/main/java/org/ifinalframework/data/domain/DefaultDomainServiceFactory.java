/*
 * Copyright 2020-2023 the original author or authors.
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

package org.ifinalframework.data.domain;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IUser;
import org.ifinalframework.core.IView;
import org.ifinalframework.data.core.AutoNameHelper;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.data.spi.PostDeleteConsumer;
import org.ifinalframework.data.spi.PostInsertConsumer;
import org.ifinalframework.data.spi.PostQueryConsumer;
import org.ifinalframework.data.spi.PreDeleteConsumer;
import org.ifinalframework.data.spi.PreInsertConsumer;
import org.ifinalframework.data.spi.PreQueryConsumer;
import org.ifinalframework.data.spi.PreUpdateYnValidator;
import org.ifinalframework.data.spi.composite.PostDeleteConsumerComposite;
import org.ifinalframework.data.spi.composite.PostInsertConsumerComposite;
import org.ifinalframework.data.spi.composite.PostQueryConsumerComposite;
import org.ifinalframework.data.spi.composite.PreDeleteConsumerComposite;
import org.ifinalframework.data.spi.composite.PreInsertConsumerComposite;
import org.ifinalframework.data.spi.composite.PreQueryConsumerComposite;
import org.ifinalframework.data.spi.composite.PreUpdateYnValidatorComposite;

import lombok.RequiredArgsConstructor;

/**
 * DefaultDomainServiceFactory.
 *
 * @author ilikly
 * @version 1.4.3
 * @since 1.4.3
 */
@RequiredArgsConstructor
public class DefaultDomainServiceFactory implements DomainServiceFactory {

    private final Class<? extends IUser<?>> userClass;
    private final ApplicationContext applicationContext;

    @Override
    @SuppressWarnings("unchecked,rawtypes")
    public <ID extends Serializable, T extends IEntity<ID>> DomainService<ID, T> create(Repository<ID, T> repository) {
        DefaultDomainService.DefaultDomainServiceBuilder<ID, T> builder = DefaultDomainService.builder();
        builder.repository(repository);
        ResolvableType repositoryResolvableType = ResolvableType.forClass(AopUtils.getTargetClass(repository)).as(Repository.class);
        Class<?> entityClass = Objects.requireNonNull(repositoryResolvableType.resolveGeneric(1));
        builder.entityClass((Class<T>) entityClass);
        ClassLoader classLoader = entityClass.getClassLoader();

        // query
        final Map<Class<?>, Class<? extends IQuery>> queryClassMap = new LinkedHashMap<>();
        builder.queryClass(queryClassMap);
        final String queryPackage = AutoNameHelper.queryPackage(entityClass);
        final String defaultQueryName = AutoNameHelper.queryName(entityClass);
        final String defaultQueryClassName = String.join(".", queryPackage, defaultQueryName);
        final Class<?> defaultqueryClass = ClassUtils.resolveClassName(defaultQueryClassName, classLoader);

        // create
        builder.preInsertConsumer(new PreInsertConsumerComposite<>(getBeansOf(PreInsertConsumer.class, entityClass, userClass)));
        builder.postInsertConsumer(new PostInsertConsumerComposite<>(getBeansOf(PostInsertConsumer.class, entityClass, userClass)));

        // delete
        builder.preDeleteConsumer(new PreDeleteConsumerComposite<>(getBeansOf(PreDeleteConsumer.class, entityClass, userClass)));
        builder.postDeleteConsumer(new PostDeleteConsumerComposite<>(getBeansOf(PostDeleteConsumer.class, entityClass, userClass)));


        // list
        final Class<?> listQueryClass = resolveClass(classLoader, buildClassName(queryPackage, IView.List.class, defaultQueryName), defaultqueryClass);
        queryClassMap.put(IView.List.class, (Class<? extends IQuery>) listQueryClass);
        builder.preQueryConsumer(new PreQueryConsumerComposite(getBeansOf(PreQueryConsumer.class, listQueryClass, userClass)));
        builder.postQueryConsumer(new PostQueryConsumerComposite<>(getBeansOf(PostQueryConsumer.class, entityClass, listQueryClass, userClass)));

        // count
        final Class<?> countQueryClass = resolveClass(classLoader, buildClassName(queryPackage, IView.Count.class, defaultQueryName), defaultqueryClass);
        queryClassMap.put(IView.Count.class, (Class<? extends IQuery>) countQueryClass);

        // yn
        builder.preUpdateYnValidator(new PreUpdateYnValidatorComposite<>(getBeansOf(PreUpdateYnValidator.class, entityClass, userClass)));
        return builder.build();
    }

    private static String buildClassName(String packageName, Class<?> prefix, String className) {
        return packageName + "." + prefix.getSimpleName() + className;
    }

    private static Class<?> resolveClass(ClassLoader classLoader, String className, Class<?> defaultClass) {
        if (ClassUtils.isPresent(className, classLoader)) {
            return ClassUtils.resolveClassName(className, classLoader);
        }
        return defaultClass;
    }

    private List getBeansOf(Class<?> type, Class<?>... generics) {
        return applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(type, generics))
                .orderedStream()
                .collect(Collectors.toList());
    }
}
