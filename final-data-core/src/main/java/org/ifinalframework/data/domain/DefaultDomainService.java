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
import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IEnum;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IUser;
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.domain.action.DeleteAction;
import org.ifinalframework.data.domain.action.DomainActions;
import org.ifinalframework.data.domain.action.InsertAction;
import org.ifinalframework.data.domain.action.SelectAction;
import org.ifinalframework.data.domain.action.UpdateAction;
import org.ifinalframework.data.domain.model.AuditValue;
import org.ifinalframework.data.spi.SpiAction;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.Serializable;

/**
 * DefaultDomainService.
 *
 * @author ilikly
 * @version 1.4.3
 * @since 1.4.3
 */
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class DefaultDomainService<ID extends Serializable, T extends IEntity<ID>, U extends IUser<?>> implements DomainService<ID, T, U> {

    private final DomainActions domainActions;

    @NonNull
    @Override
    public Class<T> entityClass() {
        return (Class<T>) domainActions.getEntityClass();
    }

    @Nullable
    @Override
    public Class<?> domainEntityClass(Class<?> prefix) {
        return domainActions.getDomainEntityClasses().get(prefix);
    }

    @NonNull
    @Override
    public Class<? extends IQuery> domainQueryClass(Class<?> prefix) {
        return (Class<? extends IQuery>) domainActions.getDomainQueryClasses().get(prefix);
    }

    @Override
    public Object export(IQuery query, U user) {
        final SelectAction selectAction = (SelectAction) domainActions.getDomainActions().get(SpiAction.Type.EXPORT_BY_QUERY);
        return selectAction.select(query, user);
    }

    @Override
    public Object create(@NonNull Object entity, @NonNull U user) {
        final InsertAction insertAction = (InsertAction) domainActions.getDomainActions().get(SpiAction.Type.CREATE);
        return insertAction.insert(entity, user);
    }

    @Override
    public Object list(@NonNull IQuery query, @NonNull U user) {
        final SelectAction selectAction = (SelectAction) domainActions.getDomainActions().get(SpiAction.Type.LIST_BY_QUERY);
        return selectAction.select(query, user);
    }

    @Override
    public Object detail(@NonNull IQuery query, @NonNull U user) {
        final SelectAction selectAction = (SelectAction) domainActions.getDomainActions().get(SpiAction.Type.DETAIL_BY_QUERY);
        return selectAction.select(query, user);

    }

    @Override
    public Object detail(@NonNull ID id, @NonNull U user) {
        final SelectAction selectAction = (SelectAction) domainActions.getDomainActions().get(SpiAction.Type.DETAIL_BY_ID);
        return selectAction.select(id, user);
    }

    @Override
    public Object count(@NonNull IQuery query, @NonNull U user) {
        final SelectAction selectAction = (SelectAction) domainActions.getDomainActions().get(SpiAction.Type.COUNT_BY_QUERY);
        return selectAction.select(query, user);
    }

    @Override
    public Object delete(@NonNull IQuery query, @NonNull U user) {
        final DeleteAction deleteAction = (DeleteAction) domainActions.getDomainActions().get(SpiAction.Type.DELETE_BY_QUERY);
        return deleteAction.delete(query, user);

    }

    @Override
    public Object delete(@NonNull ID id, @NonNull U user) {
        final DeleteAction deleteAction = (DeleteAction) domainActions.getDomainActions().get(SpiAction.Type.DELETE_BY_ID);
        return deleteAction.delete(id, user);
    }

    @Override
    public Object update(@NonNull T entity, @NonNull ID id, boolean selective, @NonNull U user) {
        entity.setId(id);
        final UpdateAction updateAction = (UpdateAction) domainActions.getDomainActions().get(SpiAction.Type.UPDATE_BY_ID);
        return updateAction.update(id, selective, entity, user);
    }

    @Override
    public Object yn(@NonNull ID id, @Nullable YN current, @NonNull YN yn, @NonNull U user) {
        final UpdateAction updateAction = (UpdateAction) domainActions.getDomainActions().get(SpiAction.Type.UPDATE_YN_BY_ID);
        return updateAction.update(id, current, yn, user);
    }

    @Override
    public Object status(@NonNull ID id, @NonNull IEnum<?> status, @NonNull U user) {
        final UpdateAction updateAction = (UpdateAction) domainActions.getDomainActions().get(SpiAction.Type.UPDATE_STATUS_BY_ID);
        return updateAction.update(id, null, status, user);
    }

    @Override
    public Object lock(@NonNull ID id, @Nullable Boolean current, @NonNull Boolean locked, @NonNull U user) {
        final UpdateAction updateAction = (UpdateAction) domainActions.getDomainActions().get(SpiAction.Type.UPDATE_LOCKED_BY_ID);
        return updateAction.update(id, current, locked, user);
    }

    @Override
    public Object audit(@NonNull ID id, @NonNull AuditValue auditValue, @NonNull U user) {
        final UpdateAction updateAction = (UpdateAction) domainActions.getDomainActions().get(SpiAction.Type.UPDATE_AUDIT_STATUS_BY_ID);
        return updateAction.update(id, null, auditValue, user);
    }
}
