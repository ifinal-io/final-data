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

import lombok.Builder;
import org.ifinalframework.context.exception.NotFoundException;
import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IEnum;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IUser;
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.domain.action.*;
import org.ifinalframework.data.domain.model.AuditValue;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.data.spi.Consumer;
import org.ifinalframework.data.spi.PreInsertFunction;
import org.ifinalframework.data.spi.PreQueryConsumer;
import org.ifinalframework.data.spi.SpiAction;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private final InsertDomainAction<ID, T, IUser<?>> insertDomainAction;

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
    private final UpdateDomainActionDispatcher<ID, T, ID, YN, IUser<?>> updateYnByIdDomainAction;

    // update status
    private final UpdateDomainActionDispatcher<ID, T, ID, IEnum<?>, IUser<?>> updateStatusByIdDomainAction;

    // update locked
    private final UpdateDomainActionDispatcher<ID, T, ID, Boolean, IUser<?>> updateLockedByIdDomainAction;
    // update audit-status
    private final UpdateDomainActionDispatcher<ID, T, ID, AuditValue, IUser<?>> updateAuditStatusByIdDomainAction;
    // delete
    private final UpdateDomainActionDispatcher<ID, T, ID, Void, IUser<?>> deleteByIdDomainAction;
    private final UpdateDomainActionDispatcher<ID, T, IQuery, Void, IUser<?>> deleteDomainAction;

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
    public Object create(@NonNull List<T> entities, @NonNull IUser<?> user) {
        return insertDomainAction.doAction(null, entities, user);
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

    @Override
    public Object audit(@NonNull ID id, @NonNull AuditValue auditValue, @NonNull IUser<?> user) {
        return updateAuditStatusByIdDomainAction.doAction(id, auditValue, user);
    }
}
