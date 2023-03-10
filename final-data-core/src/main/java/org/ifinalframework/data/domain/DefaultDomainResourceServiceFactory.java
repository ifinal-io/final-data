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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IUser;
import org.ifinalframework.core.IView;
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.core.AutoNameHelper;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.data.spi.AfterReturningConsumer;
import org.ifinalframework.data.spi.AfterReturningQueryConsumer;
import org.ifinalframework.data.spi.AfterThrowingConsumer;
import org.ifinalframework.data.spi.AfterThrowingQueryConsumer;
import org.ifinalframework.data.spi.Consumer;
import org.ifinalframework.data.spi.Filter;
import org.ifinalframework.data.spi.PostQueryConsumer;
import org.ifinalframework.data.spi.PreInsertFunction;
import org.ifinalframework.data.spi.PreQueryConsumer;
import org.ifinalframework.data.spi.PreUpdateValidator;
import org.ifinalframework.data.spi.SpiAction;
import org.ifinalframework.data.spi.UpdateConsumer;
import org.ifinalframework.util.CompositeProxies;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DefaultDomainServiceFactory.
 *
 * @author ilikly
 * @version 1.4.3
 * @since 1.4.3
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultDomainResourceServiceFactory implements DomainResourceServiceFactory {

    private final Class<? extends IUser<?>> userClass;
    private final ApplicationContext applicationContext;

    private final DomainSpiMatcher domainSpiMatcher = new SimpleNameDomainSpiMatcher();

    @Override
    @SuppressWarnings("unchecked,rawtypes")
    public <ID extends Serializable, T extends IEntity<ID>> DomainResourceService<ID, T> create(Repository<ID, T> repository) {
        DefaultDomainResourceService.DefaultDomainResourceServiceBuilder<ID, T> builder = DefaultDomainResourceService.builder();
        builder.repository(repository);
        ResolvableType repositoryResolvableType = ResolvableType.forClass(AopUtils.getTargetClass(repository)).as(Repository.class);
        Class<?> entityClass = Objects.requireNonNull(repositoryResolvableType.resolveGeneric(1));
        builder.entityClass((Class<T>) entityClass);
        ClassLoader classLoader = entityClass.getClassLoader();

        // entity
        final Map<Class<?>, Class<?>> domainEntityClassMap = new LinkedHashMap<>();
        builder.domainClassMap(domainEntityClassMap);

        // query
        final Map<Class<?>, Class<? extends IQuery>> queryClassMap = new LinkedHashMap<>();
        builder.queryClassMap(queryClassMap);
        final String queryPackage = AutoNameHelper.queryPackage(entityClass);
        final String defaultQueryName = AutoNameHelper.queryName(entityClass);
        final String defaultQueryClassName = String.join(".", queryPackage, defaultQueryName);
        final Class<?> defaultqueryClass = ClassUtils.resolveClassName(defaultQueryClassName, classLoader);

        // create

        String dtoClassName = AutoNameHelper.dtoClassName(entityClass, IView.Create.class.getSimpleName());

        if (ClassUtils.isPresent(dtoClassName, entityClass.getClassLoader())) {
            Class<?> dtoClass = ClassUtils.resolveClassName(dtoClassName, entityClass.getClassLoader());
            domainEntityClassMap.put(IView.Create.class, dtoClass);
            PreInsertFunction preInsertFunction = (PreInsertFunction) applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(PreInsertFunction.class, dtoClass, userClass, entityClass)).getObject();
            builder.preInsertFunction(preInsertFunction);

        }

        // PreInsert
        builder.preInsertFilter(getSpiComposite(SpiAction.CREATE, SpiAction.Advice.PRE, Filter.class, entityClass, userClass));
        builder.preInsertConsumer(getSpiComposite(SpiAction.CREATE, SpiAction.Advice.PRE, Consumer.class, entityClass, userClass));
        // PostInsert
        builder.postInsertConsumer(getSpiComposite(SpiAction.CREATE, SpiAction.Advice.POST, Consumer.class, entityClass, userClass));
        builder.afterThrowingInsertConsumer(getSpiComposite(SpiAction.CREATE, SpiAction.Advice.AFTER_THROWING, AfterThrowingConsumer.class, entityClass, userClass));
        builder.afterReturningInsertConsumer(getSpiComposite(SpiAction.CREATE, SpiAction.Advice.AFTER_RETURNING, AfterReturningConsumer.class, entityClass, Integer.class, userClass));

        // delete
        final Class<?> deleteQueryClass = resolveClass(classLoader, buildClassName(queryPackage, IView.Delete.class, defaultQueryName), defaultqueryClass);
        queryClassMap.put(IView.Delete.class, (Class<? extends IQuery>) deleteQueryClass);
        // PreDelete
        builder.preDeleteQueryConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.PRE, PreQueryConsumer.class, deleteQueryClass, userClass));
        builder.preDeleteConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.PRE, Consumer.class, entityClass, userClass));
        // PostDelete
        builder.postDeleteQueryConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.POST, PostQueryConsumer.class, entityClass, deleteQueryClass, userClass));
        builder.postDeleteConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.POST, Consumer.class, entityClass, userClass));

        // update
        builder.preUpdateConsumer(getSpiComposite(SpiAction.UPDATE, SpiAction.Advice.PRE, Consumer.class, entityClass, userClass));
        builder.postUpdateConsumer(getSpiComposite(SpiAction.UPDATE, SpiAction.Advice.POST, Consumer.class, entityClass, userClass));


        // list
        final Class<?> listQueryClass = resolveClass(classLoader, buildClassName(queryPackage, IView.List.class, defaultQueryName), defaultqueryClass);
        queryClassMap.put(IView.List.class, (Class<? extends IQuery>) listQueryClass);
        builder.preQueryConsumer(getSpiComposite(SpiAction.LIST, SpiAction.Advice.PRE, PreQueryConsumer.class, listQueryClass, userClass));
        builder.postQueryConsumer(getSpiComposite(SpiAction.LIST, SpiAction.Advice.POST, PostQueryConsumer.class, entityClass, listQueryClass, userClass));
        builder.afterThrowingQueryConsumer(getSpiComposite(SpiAction.LIST, SpiAction.Advice.AFTER_THROWING, AfterThrowingQueryConsumer.class, entityClass, listQueryClass, userClass));
        builder.afterReturningQueryConsumer(getSpiComposite(SpiAction.LIST, SpiAction.Advice.AFTER_RETURNING, AfterReturningQueryConsumer.class, entityClass, listQueryClass, userClass));

        // detail
        final Class<?> detailQueryClass = resolveClass(classLoader, buildClassName(queryPackage, IView.Detail.class, defaultQueryName), defaultqueryClass);
        queryClassMap.put(IView.Detail.class, (Class<? extends IQuery>) detailQueryClass);
        builder.preDetailQueryConsumer(getSpiComposite(SpiAction.DETAIL, SpiAction.Advice.PRE, PreQueryConsumer.class, detailQueryClass, userClass));
        builder.postDetailQueryConsumer(getSpiComposite(SpiAction.DETAIL, SpiAction.Advice.POST, PostQueryConsumer.class, entityClass, detailQueryClass, userClass));
        builder.postDetailConsumer(getSpiComposite(SpiAction.DETAIL, SpiAction.Advice.POST, Consumer.class, entityClass, userClass));

        // count
        final Class<?> countQueryClass = resolveClass(classLoader, buildClassName(queryPackage, IView.Count.class, defaultQueryName), defaultqueryClass);
        queryClassMap.put(IView.Count.class, (Class<? extends IQuery>) countQueryClass);
        builder.preCountQueryConsumer(getSpiComposite(SpiAction.COUNT, SpiAction.Advice.PRE, PreQueryConsumer.class, countQueryClass, userClass));

        // update yn
        builder.preUpdateYnValidator(getSpiComposite(SpiAction.UPDATE_YN, SpiAction.Advice.PRE, PreUpdateValidator.class, entityClass, YN.class, userClass));
        builder.postUpdateYnConsumer(getSpiComposite(SpiAction.UPDATE_YN, SpiAction.Advice.POST, UpdateConsumer.class, entityClass, YN.class, userClass));
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

    @SuppressWarnings("unchecked")
    private <SPI> SPI getSpiComposite(SpiAction action, SpiAction.Advice advice, Class<SPI> type, Class<?>... generics) {
        List beans = getBeansOf(action, advice, type, generics);
        if (CollectionUtils.isEmpty(beans) && type == Filter.class) {
            beans = Collections.singletonList((Filter) (action1, entity, user) -> true);
        }
        return (SPI) CompositeProxies.composite(type, beans);
    }


    private List getBeansOf(SpiAction action, SpiAction.Advice advice, Class<?> type, Class<?>... generics) {
        return applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(type, generics))
                .orderedStream()
                .filter(it -> {

                    final boolean matches = domainSpiMatcher.matches(it, action, advice);
                    if (matches) {
                        logger.info("found type={} for action={} with advice={}", type, action, advice);
                    }
                    return matches;

                })
                .collect(Collectors.toList());
    }

}
