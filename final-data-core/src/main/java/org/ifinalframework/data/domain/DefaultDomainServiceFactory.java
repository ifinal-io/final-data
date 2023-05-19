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
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.ILock;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IStatus;
import org.ifinalframework.core.IUser;
import org.ifinalframework.core.IView;
import org.ifinalframework.data.annotation.DomainResource;
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.domain.action.AbsUpdateDomainAction;
import org.ifinalframework.data.domain.action.DeleteByIdDomainAction;
import org.ifinalframework.data.domain.action.DeleteDomainAction;
import org.ifinalframework.data.domain.action.DetailByIdDomainAction;
import org.ifinalframework.data.domain.action.DetailQueryDomainAction;
import org.ifinalframework.data.domain.action.InsertDomainAction;
import org.ifinalframework.data.domain.action.ListQueryDomainAction;
import org.ifinalframework.data.domain.action.UpdateLockedByIdDomainAction;
import org.ifinalframework.data.domain.action.UpdateStatusByIdDomainAction;
import org.ifinalframework.data.domain.action.UpdateYnByIdDomainAction;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.data.spi.AfterReturningConsumer;
import org.ifinalframework.data.spi.AfterReturningQueryConsumer;
import org.ifinalframework.data.spi.AfterThrowingConsumer;
import org.ifinalframework.data.spi.AfterThrowingQueryConsumer;
import org.ifinalframework.data.spi.BiConsumer;
import org.ifinalframework.data.spi.Consumer;
import org.ifinalframework.data.spi.Filter;
import org.ifinalframework.data.spi.Function;
import org.ifinalframework.data.spi.PostQueryConsumer;
import org.ifinalframework.data.spi.PreInsertFunction;
import org.ifinalframework.data.spi.PreQueryConsumer;
import org.ifinalframework.data.spi.PreUpdateValidator;
import org.ifinalframework.data.spi.SpiAction;
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
public class DefaultDomainServiceFactory implements DomainServiceFactory {

    private final Class<? extends IUser<?>> userClass;
    private final ApplicationContext applicationContext;

    private final DomainSpiMatcher domainSpiMatcher = new SimpleNameDomainSpiMatcher();

    @Override
    @SuppressWarnings("unchecked,rawtypes")
    public <ID extends Serializable, T extends IEntity<ID>> DomainService<ID, T> create(Repository<ID, T> repository) {
        DefaultDomainService.DefaultDomainServiceBuilder<ID, T> builder = DefaultDomainService.builder();
        builder.repository(repository);
        ResolvableType repositoryResolvableType = ResolvableType.forClass(AopUtils.getTargetClass(repository)).as(Repository.class);
        Class<?> entityClass = Objects.requireNonNull(repositoryResolvableType.resolveGeneric(1));
        builder.entityClass((Class<T>) entityClass);
        ClassLoader classLoader = entityClass.getClassLoader();

        final DomainResource domainResource = AnnotationUtils.findAnnotation(entityClass, DomainResource.class);
        // entity
        final Map<Class<?>, Class<?>> domainEntityClassMap = new LinkedHashMap<>();
        builder.domainClassMap(domainEntityClassMap);

        // query
        final Map<Class<?>, Class<? extends IQuery>> queryClassMap = new LinkedHashMap<>();
        builder.queryClassMap(queryClassMap);
        final String queryPackage = DomainNameHelper.domainQueryPackage(entityClass);
        final String defaultQueryName = DomainNameHelper.domainQueryName(entityClass);
        final String defaultQueryClassName = String.join(".", queryPackage, defaultQueryName);
        final Class<?> defaultqueryClass = ClassUtils.resolveClassName(defaultQueryClassName, classLoader);
        queryClassMap.put(IView.class, (Class<? extends IQuery>) defaultqueryClass);

        // create

        String dtoClassName = DomainNameHelper.modelClassName(entityClass, IView.Create.class.getSimpleName());

        if (ClassUtils.isPresent(dtoClassName, entityClass.getClassLoader())) {
            Class<?> dtoClass = ClassUtils.resolveClassName(dtoClassName, entityClass.getClassLoader());
            domainEntityClassMap.put(IView.Create.class, dtoClass);
            PreInsertFunction preInsertFunction = (PreInsertFunction) applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(PreInsertFunction.class, dtoClass, userClass, entityClass)).getObject();
            builder.preInsertFunction(preInsertFunction);
        }

        InsertDomainAction<ID, T, IUser<?>> insertDomainAction = new InsertDomainAction<>(repository, domainResource.insertIgnore());
        // PreInsert
        insertDomainAction.setPreInsertFilter(getSpiComposite(SpiAction.CREATE, SpiAction.Advice.PRE, Filter.class, entityClass, userClass));
        insertDomainAction.setPreInsertConsumer(getSpiComposite(SpiAction.CREATE, SpiAction.Advice.PRE, Consumer.class, entityClass, userClass));
        // PostInsert
        insertDomainAction.setPostInsertConsumer(getSpiComposite(SpiAction.CREATE, SpiAction.Advice.POST, Consumer.class, entityClass, userClass));
        insertDomainAction.setAfterThrowingInsertConsumer(getSpiComposite(SpiAction.CREATE, SpiAction.Advice.AFTER_THROWING, AfterThrowingConsumer.class, entityClass, userClass));
        insertDomainAction.setAfterReturningInsertConsumer(getSpiComposite(SpiAction.CREATE, SpiAction.Advice.AFTER_RETURNING, AfterReturningConsumer.class, entityClass, Integer.class, userClass));

        builder.insertDomainAction(insertDomainAction);


        // delete
        final Class<?> deleteQueryClass = resolveClass(classLoader, queryPackage + "." + DomainNameHelper.domainQueryName(entityClass, IView.Delete.class), defaultqueryClass);
        queryClassMap.put(IView.Delete.class, (Class<? extends IQuery>) deleteQueryClass);

        final DeleteDomainAction<ID, T, IUser<?>> deleteDomainAction = new DeleteDomainAction<>(repository);
        deleteDomainAction.setPreQueryConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.PRE, PreQueryConsumer.class, deleteQueryClass, userClass));
        deleteDomainAction.setPreConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.PRE, Consumer.class, entityClass, userClass));
        deleteDomainAction.setPostConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.POST, Consumer.class, entityClass, userClass));
        deleteDomainAction.setPostQueryConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.POST, PostQueryConsumer.class, entityClass, deleteQueryClass, userClass));
        builder.deleteDomainAction(deleteDomainAction);

        final DeleteByIdDomainAction<ID, T, IUser<?>> deleteByIdDomainAction = new DeleteByIdDomainAction<>(repository);
        deleteByIdDomainAction.setPreConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.PRE, Consumer.class, entityClass, userClass));
        deleteByIdDomainAction.setPostConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.POST, Consumer.class, entityClass, userClass));
        builder.deleteByIdDomainAction(deleteByIdDomainAction);

        // update
        builder.preUpdateConsumer(getSpiComposite(SpiAction.UPDATE, SpiAction.Advice.PRE, Consumer.class, entityClass, userClass));
        builder.postUpdateConsumer(getSpiComposite(SpiAction.UPDATE, SpiAction.Advice.POST, Consumer.class, entityClass, userClass));


        // list
        final Class<?> listQueryClass = resolveClass(classLoader, queryPackage + "." + DomainNameHelper.domainQueryName(entityClass, IView.List.class), defaultqueryClass);
        queryClassMap.put(IView.List.class, (Class<? extends IQuery>) listQueryClass);

        final ListQueryDomainAction<ID, T, IQuery, IUser<?>> listQueryDomainAction = new ListQueryDomainAction<>(repository);
        listQueryDomainAction.setPreQueryConsumer(getSpiComposite(SpiAction.LIST, SpiAction.Advice.PRE, PreQueryConsumer.class, listQueryClass, userClass));
        listQueryDomainAction.setPostQueryConsumer(getSpiComposite(SpiAction.LIST, SpiAction.Advice.POST, PostQueryConsumer.class, entityClass, listQueryClass, userClass));
        listQueryDomainAction.setAfterThrowingQueryConsumer(getSpiComposite(SpiAction.LIST, SpiAction.Advice.AFTER_THROWING, AfterThrowingQueryConsumer.class, entityClass, listQueryClass, userClass));
        listQueryDomainAction.setAfterReturningQueryConsumer(getSpiComposite(SpiAction.LIST, SpiAction.Advice.AFTER_RETURNING, AfterReturningQueryConsumer.class, entityClass, listQueryClass, userClass));
        // PostQueryFunction<List<T>,IQuery,IUser>
        applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(Function.class,
                ResolvableType.forClassWithGenerics(List.class, entityClass),
                ResolvableType.forClass(listQueryClass),
                ResolvableType.forClass(userClass)
        )).ifAvailable(postQueryFunction -> listQueryDomainAction.setPostQueryFunction((Function<List<T>, IQuery, IUser<?>>) postQueryFunction));
        builder.listQueryDomainAction(listQueryDomainAction);


        // detail
        final Class<?> detailQueryClass = resolveClass(classLoader, queryPackage + "." + DomainNameHelper.domainQueryName(entityClass, IView.Detail.class), defaultqueryClass);
        queryClassMap.put(IView.Detail.class, (Class<? extends IQuery>) detailQueryClass);

        final DetailQueryDomainAction<ID, T, IQuery, IUser<?>> detailQueryDomainAction = new DetailQueryDomainAction<>(repository);
        detailQueryDomainAction.setPreQueryConsumer(getSpiComposite(SpiAction.DETAIL, SpiAction.Advice.PRE, PreQueryConsumer.class, detailQueryClass, userClass));
        detailQueryDomainAction.setPostConsumer(getSpiComposite(SpiAction.DETAIL, SpiAction.Advice.POST, Consumer.class, entityClass, userClass));
        detailQueryDomainAction.setPostQueryConsumer(getSpiComposite(SpiAction.DETAIL, SpiAction.Advice.POST, PostQueryConsumer.class, entityClass, detailQueryClass, userClass));
        builder.detailQueryDomainAction(detailQueryDomainAction);


        final DetailByIdDomainAction<ID, T, IUser<?>> detailByIdDomainAction = new DetailByIdDomainAction<>(repository);
        detailByIdDomainAction.setPostConsumer(getSpiComposite(SpiAction.DETAIL, SpiAction.Advice.POST, Consumer.class, entityClass, userClass));
        builder.detailByIdDomainAction(detailByIdDomainAction);

        // count
        final Class<?> countQueryClass = resolveClass(classLoader, queryPackage + "." + DomainNameHelper.domainQueryName(entityClass, IView.Count.class), defaultqueryClass);
        queryClassMap.put(IView.Count.class, (Class<? extends IQuery>) countQueryClass);
        builder.preCountQueryConsumer(getSpiComposite(SpiAction.COUNT, SpiAction.Advice.PRE, PreQueryConsumer.class, countQueryClass, userClass));

        // update yn
        final UpdateYnByIdDomainAction<ID, T, IUser<?>> updateYnByIdDomainAction = new UpdateYnByIdDomainAction<>(repository);
        acceptUpdateDomainAction(updateYnByIdDomainAction, SpiAction.UPDATE_YN, entityClass, YN.class, userClass);
        builder.updateYnByIdDomainAction(updateYnByIdDomainAction);


        // update status
        if (IStatus.class.isAssignableFrom(entityClass)) {
            final Class<?> statusClass = ResolvableType.forClass(entityClass).as(IStatus.class).resolveGeneric();
            final UpdateStatusByIdDomainAction<ID, T, IUser<?>> updateStatusByIdDomainAction = new UpdateStatusByIdDomainAction<>(repository);
            acceptUpdateDomainAction(updateStatusByIdDomainAction, SpiAction.UPDATE_STATUS, entityClass, statusClass, userClass);
            builder.updateStatusByIdDomainAction(updateStatusByIdDomainAction);
        }
        // update locked
        if (ILock.class.isAssignableFrom(entityClass)) {
            final UpdateLockedByIdDomainAction<ID, T, IUser<?>> updateLockedByIdDomainAction = new UpdateLockedByIdDomainAction<>(repository);
            acceptUpdateDomainAction(updateLockedByIdDomainAction, SpiAction.UPDATE_LOCKED, entityClass, Boolean.class, userClass);
            builder.updateLockedByIdDomainAction(updateLockedByIdDomainAction);
        }


        return builder.build();
    }

    private <ID extends Serializable, T extends IEntity<ID>, Q, V, R> void acceptUpdateDomainAction(AbsUpdateDomainAction<ID, T, Q, V, R, IUser<?>> action, SpiAction spiAction, Class<?> entityClass, Class<?> valueClass, Class<?> userClass) {
        action.setPreUpdateValidator(getSpiComposite(spiAction, SpiAction.Advice.PRE, PreUpdateValidator.class, entityClass, valueClass, userClass));
        action.setPreUpdateConsumer(getSpiComposite(spiAction, SpiAction.Advice.PRE, BiConsumer.class, entityClass, valueClass, userClass));
        action.setPostUpdateConsumer(getSpiComposite(spiAction, SpiAction.Advice.POST, BiConsumer.class, entityClass, valueClass, userClass));
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
