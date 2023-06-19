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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ifinalframework.core.*;
import org.ifinalframework.data.annotation.DomainResource;
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.domain.action.*;
import org.ifinalframework.data.domain.model.AuditValue;
import org.ifinalframework.data.domain.spi.LoggerAfterConsumer;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.data.spi.*;
import org.ifinalframework.util.CompositeProxies;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DefaultDomainServiceFactory.
 *
 * @author ilikly
 * @version 1.4.3
 * @since 1.4.3
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultDomainServiceFactory<U extends IUser<?>> implements DomainServiceFactory {

    private final Class<U> userClass;
    private final ApplicationContext applicationContext;

    private final DomainSpiMatcher domainSpiMatcher = new SimpleNameDomainSpiMatcher();

    private final LoggerAfterConsumer loggerAfterConsumer;

    @Override
    @SuppressWarnings("unchecked,rawtypes")
    public <ID extends Serializable, T extends IEntity<ID>, U extends IUser<?>> DomainService<ID, T, U> create(Repository<ID, T> repository) {
        DefaultDomainService.DefaultDomainServiceBuilder<ID, T, U> builder = DefaultDomainService.builder();
        builder.repository(repository);
        ResolvableType repositoryResolvableType = ResolvableType.forClass(AopUtils.getTargetClass(repository)).as(Repository.class);
        Class<?> idClass = Objects.requireNonNull(repositoryResolvableType.resolveGeneric());
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

        InsertDomainActionDispatcher<ID, T, U> insertDomainActionDispatcher = new InsertDomainActionDispatcher<>(repository, domainResource.insertIgnore());
        // PreInsert
        insertDomainActionDispatcher.setPreInsertFilter(getSpiComposite(SpiAction.CREATE, SpiAction.Advice.PRE, Filter.class, entityClass, userClass));
        insertDomainActionDispatcher.setPreInsertConsumer(getSpiComposite(SpiAction.CREATE, SpiAction.Advice.PRE, Consumer.class, entityClass, userClass));
        // PostInsert
        insertDomainActionDispatcher.setPostInsertConsumer(getSpiComposite(SpiAction.CREATE, SpiAction.Advice.POST, Consumer.class, entityClass, userClass));
        insertDomainActionDispatcher.setAfterThrowingInsertConsumer(getSpiComposite(SpiAction.CREATE, SpiAction.Advice.AFTER_THROWING, AfterThrowingConsumer.class, entityClass, userClass));
        insertDomainActionDispatcher.setAfterReturningInsertConsumer(getSpiComposite(SpiAction.CREATE, SpiAction.Advice.AFTER_RETURNING, AfterReturningConsumer.class, entityClass, Integer.class, userClass));
        insertDomainActionDispatcher.setAfterConsumer(getSpiComposite(SpiAction.CREATE, SpiAction.Advice.AFTER, AfterConsumer.class, entityClass, Void.class, Void.class, Integer.class, userClass));
        builder.insertDomainActionDispatcher(insertDomainActionDispatcher);


        // delete
        final Class<?> deleteQueryClass = resolveClass(classLoader, queryPackage + "." + DomainNameHelper.domainQueryName(entityClass, IView.Delete.class), defaultqueryClass);
        queryClassMap.put(IView.Delete.class, (Class<? extends IQuery>) deleteQueryClass);

        //UpdateFunction<Entity,DeleteQuery,Void,User>
        final DeleteFunction<T, IQuery, U> deleteUpdateFunction = (DeleteFunction<T, IQuery, U>) applicationContext.getBeanProvider(
                        ResolvableType.forClassWithGenerics(
                                UpdateFunction.class,
                                ResolvableType.forClass(entityClass),
                                ResolvableType.forClass(deleteQueryClass),
                                ResolvableType.forClass(Void.class),
                                ResolvableType.forClass(userClass)
                        ))
                .getIfAvailable(() -> new DefaultDeleteFunction<>(repository));

        final DeleteDomainActionDispatcher<ID, T, IQuery, U> deleteDomainAction = new DeleteDomainActionDispatcher<>(SpiAction.DELETE, repository, deleteUpdateFunction);
        deleteDomainAction.setPreQueryConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.PRE, PreQueryConsumer.class, deleteQueryClass, userClass));
        deleteDomainAction.setPreConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.PRE, Consumer.class, entityClass, userClass));
        deleteDomainAction.setPostConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.POST, Consumer.class, entityClass, userClass));
        deleteDomainAction.setPostQueryConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.POST, BiConsumer.class, entityClass, deleteQueryClass, userClass));
        deleteDomainAction.setAfterConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.AFTER, AfterConsumer.class, entityClass, deleteQueryClass, Void.class, Integer.class, userClass));
        builder.deleteDomainAction(deleteDomainAction);


        //UpdateFunction<Entity,DeleteQuery,Void,User>
        final UpdateFunction<T, ID, Void, U> deleteUpdateFunctionById = (UpdateFunction<T, ID, Void, U>) applicationContext.getBeanProvider(
                        ResolvableType.forClassWithGenerics(
                                UpdateFunction.class,
                                ResolvableType.forClass(entityClass),
                                ResolvableType.forClass(idClass),
                                ResolvableType.forClass(Void.class),
                                ResolvableType.forClass(userClass)
                        ))
                .getIfAvailable(() -> new DefaultDeleteFunction<>(repository));

        final UpdateDomainActionDispatcher<ID, T, ID, Void, U> deleteByIdDomainAction = new UpdateDomainActionDispatcher<>(SpiAction.DELETE, repository, deleteUpdateFunctionById);
        deleteByIdDomainAction.setPreConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.PRE, Consumer.class, entityClass, userClass));
        deleteByIdDomainAction.setPostConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.POST, Consumer.class, entityClass, userClass));
        deleteByIdDomainAction.setAfterConsumer(getSpiComposite(SpiAction.DELETE, SpiAction.Advice.AFTER, AfterConsumer.class, entityClass, idClass, Void.class, Integer.class, userClass));
        builder.deleteByIdDomainAction(deleteByIdDomainAction);

        // update
        builder.preUpdateConsumer(getSpiComposite(SpiAction.UPDATE, SpiAction.Advice.PRE, Consumer.class, entityClass, userClass));
        builder.postUpdateConsumer(getSpiComposite(SpiAction.UPDATE, SpiAction.Advice.POST, Consumer.class, entityClass, userClass));


        // list
        final Class<?> listQueryClass = resolveClass(classLoader, queryPackage + "." + DomainNameHelper.domainQueryName(entityClass, IView.List.class), defaultqueryClass);
        queryClassMap.put(IView.List.class, (Class<? extends IQuery>) listQueryClass);


        // SelectFunction<Query,User,List<Entity>>
        final SelectFunction<IQuery, U, List<T>> listSelectFunction = (SelectFunction<IQuery, U, List<T>>) applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(SelectFunction.class, ResolvableType.forClass(listQueryClass), ResolvableType.forClass(userClass), ResolvableType.forClassWithGenerics(List.class, entityClass)))
                .getIfAvailable(() -> new DefaultSelectFunction<>(repository));

        final SelectDomainDispatcher<ID, T, IQuery, U, List<T>> listQueryDomainAction = new SelectDomainDispatcher<>(SpiAction.LIST, IView.List.class, listSelectFunction);
        listQueryDomainAction.setPreQueryConsumer(getSpiComposite(SpiAction.LIST, SpiAction.Advice.PRE, PreQueryConsumer.class, listQueryClass, userClass));
        listQueryDomainAction.setPostQueryConsumer(getSpiComposite(SpiAction.LIST, SpiAction.Advice.POST, BiConsumer.class, entityClass, listQueryClass, userClass));
        listQueryDomainAction.setAfterThrowingQueryConsumer(getSpiComposite(SpiAction.LIST, SpiAction.Advice.AFTER_THROWING, AfterThrowingQueryConsumer.class, entityClass, listQueryClass, userClass));
        listQueryDomainAction.setAfterReturningQueryConsumer(getSpiComposite(SpiAction.LIST, SpiAction.Advice.AFTER_RETURNING, AfterReturningQueryConsumer.class, entityClass, listQueryClass, userClass));
        // PostQueryFunction<List<T>,IQuery,IUser>
        applicationContext.getBeanProvider(ResolvableType.forClassWithGenerics(Function.class,
                ResolvableType.forClassWithGenerics(List.class, entityClass),
                ResolvableType.forClass(listQueryClass),
                ResolvableType.forClass(userClass)
        )).ifAvailable(postQueryFunction -> listQueryDomainAction.setPostQueryFunction((Function<List<T>, IQuery, U>) postQueryFunction));
        builder.listQueryDomainAction(listQueryDomainAction);


        // detail
        final Class<?> detailQueryClass = resolveClass(classLoader, queryPackage + "." + DomainNameHelper.domainQueryName(entityClass, IView.Detail.class), defaultqueryClass);
        queryClassMap.put(IView.Detail.class, (Class<? extends IQuery>) detailQueryClass);

        // SelectFunction<Query,User,Entity>
        final SelectFunction<IQuery, U, T> selectOneFunctionByQuery = (SelectFunction<IQuery, U, T>) applicationContext.getBeanProvider(
                        ResolvableType.forClassWithGenerics(
                                SelectFunction.class,
                                ResolvableType.forClass(detailQueryClass),
                                ResolvableType.forClass(userClass),
                                ResolvableType.forClass(entityClass)
                        ))
                .getIfAvailable(() -> new DefaultSelectOneFunction<>(repository));
        final SelectDomainDispatcher<ID, T, IQuery, U, T> detailQueryDomainAction = new SelectDomainDispatcher<>(SpiAction.DETAIL, IView.Detail.class, selectOneFunctionByQuery);
        detailQueryDomainAction.setPreQueryConsumer(getSpiComposite(SpiAction.DETAIL, SpiAction.Advice.PRE, PreQueryConsumer.class, detailQueryClass, userClass));
        detailQueryDomainAction.setPostConsumer(getSpiComposite(SpiAction.DETAIL, SpiAction.Advice.POST, Consumer.class, entityClass, userClass));
        detailQueryDomainAction.setPostQueryConsumer(getSpiComposite(SpiAction.DETAIL, SpiAction.Advice.POST, BiConsumer.class, entityClass, detailQueryClass, userClass));
        builder.detailQueryDomainAction(detailQueryDomainAction);


        // SelectFunction<Long,User,Entity>
        final SelectFunction<ID, U, T> selectOneFunctionById = (SelectFunction<ID, U, T>) applicationContext.getBeanProvider(
                        ResolvableType.forClassWithGenerics(
                                SelectFunction.class,
                                ResolvableType.forClass(idClass),
                                ResolvableType.forClass(userClass),
                                ResolvableType.forClass(entityClass)
                        ))
                .getIfAvailable(() -> new DefaultSelectOneFunction<>(repository));
        final SelectDomainDispatcher<ID, T, ID, U, T> detailByIdDomainAction = new SelectDomainDispatcher<>(SpiAction.DETAIL, IView.Detail.class, selectOneFunctionById);
        detailByIdDomainAction.setPostConsumer(getSpiComposite(SpiAction.DETAIL, SpiAction.Advice.POST, Consumer.class, entityClass, userClass));
        builder.detailByIdDomainAction(detailByIdDomainAction);

        // count
        final Class<?> countQueryClass = resolveClass(classLoader, queryPackage + "." + DomainNameHelper.domainQueryName(entityClass, IView.Count.class), defaultqueryClass);
        queryClassMap.put(IView.Count.class, (Class<? extends IQuery>) countQueryClass);
        builder.preCountQueryConsumer(getSpiComposite(SpiAction.COUNT, SpiAction.Advice.PRE, PreQueryConsumer.class, countQueryClass, userClass));

        // update yn

        //UpdateFunction<Entity,ID,YN,User>
        final UpdateFunction<T, ID, YN, U> updateYnByIdFunction = (UpdateFunction<T, ID, YN, U>) applicationContext.getBeanProvider(
                        ResolvableType.forClassWithGenerics(
                                UpdateFunction.class,
                                ResolvableType.forClass(entityClass),
                                ResolvableType.forClass(idClass),
                                ResolvableType.forClass(YN.class),
                                ResolvableType.forClass(userClass)
                        ))
                .getIfAvailable(() -> new DefaultUpdateYNFunction<>(repository));

        final UpdateDomainActionDispatcher<ID, T, ID, YN, U> updateYnByIdDomainAction = new UpdateDomainActionDispatcher<>(SpiAction.UPDATE_YN, repository, updateYnByIdFunction);
        acceptUpdateDomainAction(updateYnByIdDomainAction, SpiAction.UPDATE_YN, entityClass, idClass, YN.class, userClass);
        builder.updateYnByIdDomainAction(updateYnByIdDomainAction);


        // update status
        if (IStatus.class.isAssignableFrom(entityClass)) {
            final Class<?> statusClass = ResolvableType.forClass(entityClass).as(IStatus.class).resolveGeneric();

            //UpdateFunction<Entity,ID,Status,User>
            final UpdateFunction<T, ID, IEnum<?>, U> updateStatusByIdFunction = (UpdateFunction<T, ID, IEnum<?>, U>) applicationContext.getBeanProvider(
                            ResolvableType.forClassWithGenerics(
                                    UpdateFunction.class,
                                    ResolvableType.forClass(entityClass),
                                    ResolvableType.forClass(idClass),
                                    ResolvableType.forClass(statusClass),
                                    ResolvableType.forClass(userClass)
                            ))
                    .getIfAvailable(() -> new DefaultUpdateStatusFunction<>(repository));

            final UpdateDomainActionDispatcher<ID, T, ID, IEnum<?>, U> updateStatusByIdDomainAction = new UpdateDomainActionDispatcher<>(SpiAction.UPDATE_STATUS, repository, updateStatusByIdFunction);
            acceptUpdateDomainAction(updateStatusByIdDomainAction, SpiAction.UPDATE_STATUS, entityClass, idClass, statusClass, userClass);
            builder.updateStatusByIdDomainAction(updateStatusByIdDomainAction);
        }
        // update locked
        if (ILock.class.isAssignableFrom(entityClass)) {

            //UpdateFunction<Entity,ID,Boolean,User>
            final UpdateFunction<T, ID, Boolean, U> updateLockedByIdFunction = (UpdateFunction<T, ID, Boolean, U>) applicationContext.getBeanProvider(
                            ResolvableType.forClassWithGenerics(
                                    UpdateFunction.class,
                                    ResolvableType.forClass(entityClass),
                                    ResolvableType.forClass(idClass),
                                    ResolvableType.forClass(Boolean.class),
                                    ResolvableType.forClass(userClass)
                            ))
                    .getIfAvailable(() -> new DefaultUpdateLockedFunction<>(repository));
            final UpdateDomainActionDispatcher<ID, T, ID, Boolean, U> updateLockedByIdDomainAction = new UpdateDomainActionDispatcher<>(SpiAction.UPDATE_LOCKED, repository, updateLockedByIdFunction);
            acceptUpdateDomainAction(updateLockedByIdDomainAction, SpiAction.UPDATE_LOCKED, entityClass, idClass, Boolean.class, userClass);
            builder.updateLockedByIdDomainAction(updateLockedByIdDomainAction);
        }

        if (IAudit.class.isAssignableFrom(entityClass)) {
            //UpdateFunction<Entity,ID,AuditValue,User>
            final UpdateFunction<T, ID, AuditValue, U> updateLockedByIdFunction = (UpdateFunction<T, ID, AuditValue, U>) applicationContext.getBeanProvider(
                            ResolvableType.forClassWithGenerics(
                                    UpdateFunction.class,
                                    ResolvableType.forClass(entityClass),
                                    ResolvableType.forClass(idClass),
                                    ResolvableType.forClass(AuditValue.class),
                                    ResolvableType.forClass(userClass)
                            ))
                    .getIfAvailable(() -> new DefaultUpdateAuditStatusFunction<>(repository));
            final UpdateDomainActionDispatcher<ID, T, ID, AuditValue, U> updateAuditStatusByIdDomainAction = new UpdateDomainActionDispatcher<>(SpiAction.UPDATE_AUDIT_STATUS, repository, updateLockedByIdFunction);
            acceptUpdateDomainAction(updateAuditStatusByIdDomainAction, SpiAction.UPDATE_AUDIT_STATUS, entityClass, idClass, Boolean.class, userClass);
            builder.updateAuditStatusByIdDomainAction(updateAuditStatusByIdDomainAction);
        }


        return builder.build();
    }

    private void acceptUpdateDomainAction(UpdateDomainActionDispatcher action, SpiAction spiAction, Class<?> entityClass, Class<?> paramClass, Class<?> valueClass, Class<U> userClass) {
        action.setPreUpdateValidator(getSpiComposite(spiAction, SpiAction.Advice.PRE, BiValidator.class, entityClass, valueClass, userClass));
        action.setPreUpdateConsumer(getSpiComposite(spiAction, SpiAction.Advice.PRE, BiConsumer.class, entityClass, valueClass, userClass));
        action.setPostUpdateConsumer(getSpiComposite(spiAction, SpiAction.Advice.POST, BiConsumer.class, entityClass, valueClass, userClass));
        action.setAfterConsumer(getSpiComposite(spiAction, SpiAction.Advice.AFTER, AfterConsumer.class, entityClass, paramClass, Void.class, Integer.class, userClass));

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

        if (type == AfterConsumer.class) {
            beans.add(loggerAfterConsumer);
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
