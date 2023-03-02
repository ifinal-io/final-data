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
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import org.ifinalframework.context.exception.NotFoundException;
import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IEnum;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IUser;
import org.ifinalframework.core.IView;
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.data.spi.AfterReturnQueryConsumer;
import org.ifinalframework.data.spi.AfterThrowingQueryConsumer;
import org.ifinalframework.data.spi.Consumer;
import org.ifinalframework.data.spi.PostQueryConsumer;
import org.ifinalframework.data.spi.PostUpdateYNConsumer;
import org.ifinalframework.data.spi.PreFilter;
import org.ifinalframework.data.spi.PreInsertFunction;
import org.ifinalframework.data.spi.PreQueryConsumer;
import org.ifinalframework.data.spi.PreUpdateYnValidator;
import org.ifinalframework.data.spi.SpiAction;
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

    private final Map<Class<?>, Class<? extends IQuery>> queryClassMap;
    private final Map<Class<?>, Class<?>> domainClassMap;

    private final PreInsertFunction<Object, IUser<?>, T> preInsertFunction;

    private final PreFilter<T, IUser<?>> preInsertFilter;
    private final Consumer<T, IUser<?>> preInsertConsumer;
    private final Consumer<T, IUser<?>> postInsertConsumer;

    // list
    private final PreQueryConsumer<IQuery, IUser<?>> preQueryConsumer;
    private final PostQueryConsumer<T, IQuery, IUser<?>> postQueryConsumer;

    private final AfterThrowingQueryConsumer<T, IQuery, IUser<?>> afterThrowingQueryConsumer;
    private final AfterReturnQueryConsumer<T, IQuery, IUser<?>> afterReturnQueryConsumer;

    // detail
    private final PreQueryConsumer<IQuery, IUser<?>> preDetailQueryConsumer;
    private final PostQueryConsumer<T, IQuery, IUser<?>> postDetailQueryConsumer;
    private final Consumer<T, IUser<?>> postDetailConsumer;

    // count
    private final PreQueryConsumer<IQuery, IUser<?>> preCountQueryConsumer;

    // update
    private final Consumer<T, IUser<?>> preUpdateConsumer;
    private final Consumer<T, IUser<?>> postUpdateConsumer;

    // update yn
    private final PreUpdateYnValidator<T, IUser<?>> preUpdateYnValidator;
    private final PostUpdateYNConsumer<T, IUser<?>> postUpdateYNConsumer;

    // delete

    private final PreQueryConsumer<IQuery, IUser<?>> preDeleteQueryConsumer;
    private final PostQueryConsumer<T, IQuery, IUser<?>> postDeleteQueryConsumer;
    private final Consumer<T, IUser<?>> preDeleteConsumer;
    private final Consumer<T, IUser<?>> postDeleteConsumer;

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

        entities = entities.stream().filter(item -> preInsertFilter.test(item, user)).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(entities)) {
            return 0;
        }

        preInsertConsumer.accept(SpiAction.PRE_CREATE, entities, user);
        int result = repository.insert(entities);
        postInsertConsumer.accept(SpiAction.POST_CREATE, entities, user);
        return result;
    }

    @Override
    public List<T> list(@NonNull IQuery query, @NonNull IUser<?> user) {
        List<T> list = null;
        Throwable throwable = null;
        try {
            preQueryConsumer.accept(SpiAction.PRE_LIST, query, user);
            list = repository.select(query);
            if (CollectionUtils.isEmpty(list)) {
                return list;
            }
            postQueryConsumer.accept(SpiAction.POST_LIST, list, query, user);
            return list;
        } catch (Exception e) {
            throwable = e;
            afterThrowingQueryConsumer.accept(list, query, user, e);
            throw e;
        } finally {
            afterReturnQueryConsumer.accept(list, query, user, throwable);
        }
    }

    @Override
    public T detail(@NonNull IQuery query, @NonNull IUser<?> user) {

        T entity = null;
        Throwable throwable = null;
        try {

            preDetailQueryConsumer.accept(SpiAction.PRE_DETAIL, query, user);
            entity = repository.selectOne(IView.Detail.class, query);
            if (Objects.nonNull(entity)) {
                postDetailQueryConsumer.accept(SpiAction.POST_DETAIL, entity, query, user);
                postDetailConsumer.accept(SpiAction.POST_DETAIL, entity, user);
            }
            return entity;
        } catch (Throwable e) {
            throwable = e;
            afterThrowingQueryConsumer.accept(entity, query, user, e);
            throw e;
        } finally {
            afterReturnQueryConsumer.accept(entity, query, user, throwable);
        }
    }

    @Override
    public T detail(@NonNull ID id, @NonNull IUser<?> user) {
        T entity = repository.selectOne(id);
        if (Objects.nonNull(entity)) {
            postDetailConsumer.accept(SpiAction.POST_DETAIL, entity, user);
        }
        return entity;
    }

    @Override
    public Long count(@NonNull IQuery query, @NonNull IUser<?> user) {
        preCountQueryConsumer.accept(SpiAction.PRE_COUNT, query, user);
        return repository.selectCount(query);
    }

    @Override
    public int delete(@NonNull IQuery query, @NonNull IUser<?> user) {
        preDeleteQueryConsumer.accept(SpiAction.PRE_DELETE, query, user);
        List<T> entities = repository.select(query);
        if (CollectionUtils.isEmpty(entities)) {
            return 0;
        }
        preDeleteConsumer.accept(SpiAction.PRE_DELETE, entities, user);
        int delete = repository.delete(entities.stream().map(T::getId).collect(Collectors.toList()));
        postDeleteQueryConsumer.accept(SpiAction.POST_DELETE, entities, query, user);
        postDeleteConsumer.accept(SpiAction.POST_DETAIL, entities, user);
        return delete;
    }

    @Override
    public int delete(@NonNull ID id, @NonNull IUser<?> user) {
        T entity = repository.selectOne(id);

        if (Objects.isNull(entity)) {
            throw new NotFoundException("not found delete target. id=" + id);
        }

        preDeleteConsumer.accept(SpiAction.PRE_DELETE, entity, user);
        int delete = repository.delete(id);
        postDeleteConsumer.accept(SpiAction.POST_DELETE, entity, user);
        return delete;
    }

    @Override
    public int update(@NonNull T entity, @NonNull ID id, boolean selective, @NonNull IUser<?> user) {
        T dbEntity = repository.selectOne(id);
        if (Objects.isNull(dbEntity)) {
            throw new NotFoundException("not found entity by id= " + id);
        }
        preUpdateConsumer.accept(SpiAction.PRE_UPDATE, entity, user);
        int update = repository.update(entity, selective, id);
        postUpdateConsumer.accept(SpiAction.POST_UPDATE, entity, user);
        return update;
    }

    @Override
    public int yn(@NonNull ID id, @NonNull YN yn, @NonNull IUser<?> user) {
        T entity = repository.selectOne(id);
        if (Objects.isNull(entity)) {
            throw new NotFoundException("not found entity by id= " + id);
        }

        preUpdateYnValidator.validate(entity, yn, user);
        Update update = Update.update().set("yn", yn);
        int rows = repository.update(update, id);
        postUpdateYNConsumer.accept(entity, yn, user);
        return rows;
    }

    @Override
    public int status(@NonNull ID id, @NonNull IEnum<?> status, @NonNull IUser<?> user) {
        T entity = repository.selectOne(id);
        if (Objects.isNull(entity)) {
            throw new NotFoundException("not found entity by id= " + id);
        }
        Update update = Update.update().set("status", status);
        return repository.update(update, id);
    }
}
