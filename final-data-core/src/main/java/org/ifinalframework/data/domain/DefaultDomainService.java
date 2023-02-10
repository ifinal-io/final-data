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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import org.ifinalframework.context.exception.NotFoundException;
import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IStatus;
import org.ifinalframework.core.IUser;
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.data.spi.PostDeleteConsumer;
import org.ifinalframework.data.spi.PostInsertConsumer;
import org.ifinalframework.data.spi.PostQueryConsumer;
import org.ifinalframework.data.spi.PostUpdateConsumer;
import org.ifinalframework.data.spi.PreDeleteConsumer;
import org.ifinalframework.data.spi.PreInsertConsumer;
import org.ifinalframework.data.spi.PreInsertFunction;
import org.ifinalframework.data.spi.PreQueryConsumer;
import org.ifinalframework.data.spi.PreUpdateConsumer;
import org.ifinalframework.data.spi.PreUpdateYnValidator;
import org.ifinalframework.query.Update;

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

    private final Map<Class<?>, Class<? extends IQuery>> queryClass;
    private final Map<Class<?>, Class<?>> domainClassMap;

    private final PreInsertFunction<Object, IUser<?>, T> preInsertFunction;
    private final PreInsertConsumer<T, IUser<?>> preInsertConsumer;
    private final PostInsertConsumer<T, IUser<?>> postInsertConsumer;
    private final PreQueryConsumer<IQuery, IUser<?>> preQueryConsumer;
    private final PostQueryConsumer<T, IQuery, IUser<?>> postQueryConsumer;

    private final PreUpdateConsumer<T, IUser<?>> preUpdateConsumer;
    private final PostUpdateConsumer<T, IUser<?>> postUpdateConsumer;

    private final PreUpdateYnValidator<T, IUser<?>> preUpdateYnValidator;

    private final PreDeleteConsumer<T, IUser<?>> preDeleteConsumer;
    private final PostDeleteConsumer<T, IUser<?>> postDeleteConsumer;

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

    @Override
    public Class<? extends IQuery> domainQueryClass(Class<?> prefix) {
        return queryClass.get(prefix);
    }

    @Override
    public PreInsertFunction<Object, IUser<?>, T> preInsertFunction() {
        return preInsertFunction;
    }

    @Override
    public Integer create(List<T> entities, IUser<?> user) {
        preInsertConsumer.accept(entities, user);
        int result = repository.insert(entities);
        postInsertConsumer.accept(entities, user);
        return result;
    }

    @Override
    public List<T> list(IQuery query, IUser<?> user) {
        preQueryConsumer.accept(query, user);
        List<T> list = repository.select(query);
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        postQueryConsumer.accept(list, query, user);

        return list;
    }

    @Override
    public T detail(ID id, IUser<?> user) {
        return repository.selectOne(id);
    }

    @Override
    public Long count(IQuery query, IUser<?> user) {
        return repository.selectCount(query);
    }

    @Override
    public int delete(ID id, IUser<?> user) {
        T entity = repository.selectOne(id);

        if (Objects.isNull(entity)) {
            throw new NotFoundException("not found delete target. id=" + id);
        }

        preDeleteConsumer.accept(entity, user);
        int delete = repository.delete(id);
        postDeleteConsumer.accept(entity, user);
        return delete;
    }

    @Override
    public int update(T entity, ID id, boolean selective, IUser<?> user) {
        T dbEntity = repository.selectOne(id);
        if (Objects.isNull(dbEntity)) {
            throw new NotFoundException("not found entity by id= " + id);
        }
        preUpdateConsumer.accept(entity, user);
        int update = repository.update(entity, selective, id);
        postUpdateConsumer.accept(entity, user);
        return update;
    }

    @Override
    public int yn(ID id, YN yn, IUser<?> user) {
        T entity = repository.selectOne(id);
        if (Objects.isNull(entity)) {
            throw new NotFoundException("not found entity by id= " + id);
        }

        preUpdateYnValidator.validate(entity, yn, user);
        Update update = Update.update().set("yn", yn);
        return repository.update(update, id);
    }

    @Override
    public int status(ID id, IStatus<?> status, IUser<?> user) {
        T entity = repository.selectOne(id);
        if (Objects.isNull(entity)) {
            throw new NotFoundException("not found entity by id= " + id);
        }
        Update update = Update.update().set("status", status);
        return repository.update(update, id);
    }
}
