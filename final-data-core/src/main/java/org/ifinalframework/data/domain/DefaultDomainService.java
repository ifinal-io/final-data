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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import org.ifinalframework.context.exception.NotFoundException;
import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IEnum;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IUser;
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.domain.action.DeleteByIdDomainAction;
import org.ifinalframework.data.domain.action.DeleteDomainAction;
import org.ifinalframework.data.domain.action.DetailByIdDomainAction;
import org.ifinalframework.data.domain.action.DetailQueryDomainAction;
import org.ifinalframework.data.domain.action.ListQueryDomainAction;
import org.ifinalframework.data.domain.action.UpdateLockedByIdDomainAction;
import org.ifinalframework.data.domain.action.UpdateStatusByIdDomainAction;
import org.ifinalframework.data.domain.action.UpdateYnByIdDomainAction;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.data.spi.AfterReturningConsumer;
import org.ifinalframework.data.spi.AfterThrowingConsumer;
import org.ifinalframework.data.spi.Consumer;
import org.ifinalframework.data.spi.Filter;
import org.ifinalframework.data.spi.PreInsertFunction;
import org.ifinalframework.data.spi.PreQueryConsumer;
import org.ifinalframework.data.spi.SpiAction;

import lombok.Builder;

/**
 * DefaultDomainService.
 *
 * @author ilikly
 * @version 1.4.3
 * @since 1.4.3
 */
@Builder
@SuppressWarnings("unchecked")
public class DefaultDomainService<ID extends Serializable, T extends IEntity<ID>> implements DomainService<ID, T> {

    private final Repository<ID, T> repository;

    private final Class<T> entityClass;

    private final Map<Class<?>, Class<? extends IQuery>> queryClassMap;
    private final Map<Class<?>, Class<?>> domainClassMap;

    private final PreInsertFunction<Object, IUser<?>, T> preInsertFunction;

    // create
    private final Filter<T, IUser<?>> preInsertFilter;
    private final Consumer<T, IUser<?>> preInsertConsumer;
    private final Consumer<T, IUser<?>> postInsertConsumer;
    private final AfterThrowingConsumer<T, IUser<?>> afterThrowingInsertConsumer;
    private final AfterReturningConsumer<T, Integer, IUser<?>> afterReturningInsertConsumer;


    // list
    private final ListQueryDomainAction<ID, T, IQuery, IUser<?>> listQueryDomainAction;
    // detail
    private final DetailQueryDomainAction<ID, T, IQuery, IUser<?>> detailQueryDomainAction;
    private final DetailByIdDomainAction<ID, T, IUser<?>> detailByIdDomainAction;

    // count
    private final PreQueryConsumer<IQuery, IUser<?>> preCountQueryConsumer;

    // update
    private final Consumer<T, IUser<?>> preUpdateConsumer;
    private final Consumer<T, IUser<?>> postUpdateConsumer;

    // update yn
    private final UpdateYnByIdDomainAction<ID, T, IUser<?>> updateYnByIdDomainAction;

    // update status
    private final UpdateStatusByIdDomainAction<ID, T, IUser<?>> updateStatusByIdDomainAction;

    // update locked
    private final UpdateLockedByIdDomainAction<ID, T, IUser<?>> updateLockedByIdDomainAction;
    // delete
    private final DeleteDomainAction<ID, T, IUser<?>> deleteDomainAction;
    private final DeleteByIdDomainAction<ID, T, IUser<?>> deleteByIdDomainAction;

    @NonNull
    @Override
    public Class<T> entityClass() {
        return entityClass;
    }

    @Nullable
    @Override
    public Class<?> domainEntityClass(Class<?> prefix) {
        return domainClassMap.get(prefix);
    }

    @NonNull
    @Override
    public Class<? extends IQuery> domainQueryClass(Class<?> prefix) {
        return queryClassMap.get(prefix);
    }

    @Override
    public PreInsertFunction<Object, IUser<?>, T> preInsertFunction() {
        return preInsertFunction;
    }

    @Override
    public Integer create(@NonNull List<T> entities, @NonNull IUser<?> user) {
        Integer result = null;
        Throwable exception = null;
        try {
            entities = entities.stream().filter(item -> preInsertFilter.test(SpiAction.CREATE, item, user)).collect(Collectors.toList());

            if (CollectionUtils.isEmpty(entities)) {
                return result = 0;
            }

            preInsertConsumer.accept(SpiAction.CREATE, SpiAction.Advice.PRE, entities, user);
            result = repository.insert(entities);
            postInsertConsumer.accept(SpiAction.CREATE, SpiAction.Advice.POST, entities, user);
            return result;
        } catch (Throwable e) {
            exception = e;
            afterThrowingInsertConsumer.accept(SpiAction.CREATE, entities, user, exception);
            throw e;
        } finally {
            afterReturningInsertConsumer.accept(SpiAction.CREATE, entities, result, user, exception);
        }
    }

    @Override
    public Object list(@NonNull IQuery query, @NonNull IUser<?> user) {
        return listQueryDomainAction.doAction(query, null, user);
    }

    @Override
    public Object detail(@NonNull IQuery query, @NonNull IUser<?> user) {
        return detailQueryDomainAction.doAction(query, null, user);

    }

    @Override
    public Object detail(@NonNull ID id, @NonNull IUser<?> user) {
        return detailByIdDomainAction.doAction(id, null, user);
    }

    @Override
    public Long count(@NonNull IQuery query, @NonNull IUser<?> user) {
        preCountQueryConsumer.accept(SpiAction.COUNT, query, user);
        return repository.selectCount(query);
    }

    @Override
    public Object delete(@NonNull IQuery query, @NonNull IUser<?> user) {
        return deleteDomainAction.doAction(query, null, user);

    }

    @Override
    public Object delete(@NonNull ID id, @NonNull IUser<?> user) {
        return deleteByIdDomainAction.doAction(id, null, user);
    }

    @Override
    public int update(@NonNull T entity, @NonNull ID id, boolean selective, @NonNull IUser<?> user) {
        T dbEntity = repository.selectOne(id);
        if (Objects.isNull(dbEntity)) {
            throw new NotFoundException("not found entity by id= " + id);
        }
        preUpdateConsumer.accept(SpiAction.UPDATE, SpiAction.Advice.PRE, Collections.singletonList(entity), user);
        int update = repository.update(entity, selective, id);
        postUpdateConsumer.accept(SpiAction.UPDATE, SpiAction.Advice.POST, Collections.singletonList(entity), user);
        return update;
    }

    @Override
    public Object yn(@NonNull ID id, @NonNull YN yn, @NonNull IUser<?> user) {
        return updateYnByIdDomainAction.doAction(id, yn, user);
    }

    @Override
    public Object status(@NonNull ID id, @NonNull IEnum<?> status, @NonNull IUser<?> user) {
        return updateStatusByIdDomainAction.doAction(id, status, user);
    }

    @Override
    public Object lock(@NonNull ID id, @NonNull Boolean locked, @NonNull IUser<?> user) {
        return updateLockedByIdDomainAction.doAction(id, locked, user);
    }
}
