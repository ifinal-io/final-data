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
import org.ifinalframework.data.domain.model.AuditValue;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.data.spi.PreInsertFunction;
import org.ifinalframework.data.spi.PreQueryConsumer;
import org.ifinalframework.data.spi.SpiAction;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.Serializable;
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
public class DefaultDomainService<ID extends Serializable, T extends IEntity<ID>, U extends IUser<?>> implements DomainService<ID, T, U> {

    private final Repository<ID, T> repository;

    private final Class<T> entityClass;


    private final Map<Class<?>, Class<? extends IQuery>> queryClassMap;
    private final Map<Class<?>, Class<?>> domainClassMap;


    private final PreInsertFunction<Object, U, T> preInsertFunction;

    // create
    private final InsertDomainActionDispatcher<ID, T, U> insertDomainActionDispatcher;

    // list
    private final SelectDomainDispatcher<ID, T, IQuery, U, List<T>> listQueryDomainAction;

    // detail
    private final SelectDomainDispatcher<ID, T, IQuery, U, T> detailQueryDomainAction;
    private final SelectDomainDispatcher<ID, T, ID, U, T> detailByIdDomainAction;

    // count
    private final PreQueryConsumer<IQuery, U> preCountQueryConsumer;

    // update
    private final BiUpdateDomainActionDispatcher<ID, T, ID, Boolean, T, U> updateByIdDomainAction;

    // update yn
    private final BiUpdateDomainActionDispatcher<ID, T, ID, YN, YN, U> updateYnByIdDomainAction;

    // update status
    private final UpdateDomainActionDispatcher<ID, T, ID, IEnum<?>, U> updateStatusByIdDomainAction;

    // update locked
    private final BiUpdateDomainActionDispatcher<ID, T, ID, Boolean, Boolean, U> updateLockedByIdDomainAction;
    // update audit-status
    private final UpdateDomainActionDispatcher<ID, T, ID, AuditValue, U> updateAuditStatusByIdDomainAction;
    // delete
    private final DeleteDomainActionDispatcher<ID, T, ID, U> deleteByIdDomainAction;
    private final DeleteDomainActionDispatcher<ID, T, IQuery, U> deleteDomainAction;

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
    public PreInsertFunction<Object, U, T> preInsertFunction() {
        return preInsertFunction;
    }

    @Override
    public Object create(@NonNull List<T> entities, @NonNull U user) {
        return insertDomainActionDispatcher.dispatch(null, entities, user);
    }

    @Override
    public Object list(@NonNull IQuery query, @NonNull U user) {
        return listQueryDomainAction.dispatch(query, null, user);
    }

    @Override
    public Object detail(@NonNull IQuery query, @NonNull U user) {
        return detailQueryDomainAction.dispatch(query, null, user);

    }

    @Override
    public Object detail(@NonNull ID id, @NonNull U user) {
        return detailByIdDomainAction.dispatch(id, null, user);
    }

    @Override
    public Long count(@NonNull IQuery query, @NonNull U user) {
        preCountQueryConsumer.accept(SpiAction.COUNT, query, user);
        return repository.selectCount(query);
    }

    @Override
    public Object delete(@NonNull IQuery query, @NonNull U user) {
        return deleteDomainAction.dispatch(query, null, user);

    }

    @Override
    public Object delete(@NonNull ID id, @NonNull U user) {
        return deleteByIdDomainAction.dispatch(id, null, user);
    }

    @Override
    public Object update(@NonNull T entity, @NonNull ID id, boolean selective, @NonNull U user) {
        T dbEntity = repository.selectOne(id);
        if (Objects.isNull(dbEntity)) {
            throw new NotFoundException("not found entity by id= " + id);
        }
        entity.setId(id);
        return updateByIdDomainAction.dispatch(id, selective, entity, user);
    }

    @Override
    public Object yn(@NonNull ID id, @Nullable YN current, @NonNull YN yn, @NonNull U user) {
        return updateYnByIdDomainAction.dispatch(id, current, yn, user);
    }

    @Override
    public Object status(@NonNull ID id, @NonNull IEnum<?> status, @NonNull U user) {
        return updateStatusByIdDomainAction.dispatch(id, status, user);
    }

    @Override
    public Object lock(@NonNull ID id,@Nullable Boolean current, @NonNull Boolean locked, @NonNull U user) {
        return updateLockedByIdDomainAction.dispatch(id,current, locked, user);
    }

    @Override
    public Object audit(@NonNull ID id, @NonNull AuditValue auditValue, @NonNull U user) {
        return updateAuditStatusByIdDomainAction.dispatch(id, auditValue, user);
    }
}
