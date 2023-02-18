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
import org.ifinalframework.data.spi.PostDeleteConsumer;
import org.ifinalframework.data.spi.PostDeleteQueryConsumer;
import org.ifinalframework.data.spi.PostDetailConsumer;
import org.ifinalframework.data.spi.PostDetailQueryConsumer;
import org.ifinalframework.data.spi.PostInsertConsumer;
import org.ifinalframework.data.spi.PostQueryConsumer;
import org.ifinalframework.data.spi.PostUpdateConsumer;
import org.ifinalframework.data.spi.PostUpdateYNConsumer;
import org.ifinalframework.data.spi.PreCountQueryConsumer;
import org.ifinalframework.data.spi.PreDeleteConsumer;
import org.ifinalframework.data.spi.PreDeleteQueryConsumer;
import org.ifinalframework.data.spi.PreDetailQueryConsumer;
import org.ifinalframework.data.spi.PreInsertConsumer;
import org.ifinalframework.data.spi.PreInsertFilter;
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

    private final Map<Class<?>, Class<? extends IQuery>> queryClassMap;
    private final Map<Class<?>, Class<?>> domainClassMap;

    private final PreInsertFunction<Object, IUser<?>, T> preInsertFunction;

    private final PreInsertFilter<T, IUser<?>> preInsertFilter;
    private final PreInsertConsumer<T, IUser<?>> preInsertConsumer;
    private final PostInsertConsumer<T, IUser<?>> postInsertConsumer;

    // list
    private final PreQueryConsumer<IQuery, IUser<?>> preQueryConsumer;
    private final PostQueryConsumer<T, IQuery, IUser<?>> postQueryConsumer;

    // detail
    private final PreDetailQueryConsumer<IQuery, IUser<?>> preDetailQueryConsumer;
    private final PostDetailQueryConsumer<T, IQuery, IUser<?>> postDetailQueryConsumer;
    private final PostDetailConsumer<T, IUser<?>> postDetailConsumer;

    // count
    private final PreCountQueryConsumer<IQuery, IUser<?>> preCountQueryConsumer;

    // update
    private final PreUpdateConsumer<T, IUser<?>> preUpdateConsumer;
    private final PostUpdateConsumer<T, IUser<?>> postUpdateConsumer;

    // update yn
    private final PreUpdateYnValidator<T, IUser<?>> preUpdateYnValidator;
    private final PostUpdateYNConsumer<T, IUser<?>> postUpdateYNConsumer;

    // delete

    private final PreDeleteQueryConsumer<IQuery, IUser<?>> preDeleteQueryConsumer;
    private final PostDeleteQueryConsumer<T, IQuery, IUser<?>> postDeleteQueryConsumer;
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

        preInsertConsumer.accept(entities, user);
        int result = repository.insert(entities);
        postInsertConsumer.accept(entities, user);
        return result;
    }

    @Override
    public List<T> list(@NonNull IQuery query, @NonNull IUser<?> user) {
        preQueryConsumer.accept(query, user);
        List<T> list = repository.select(query);
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        postQueryConsumer.accept(list, query, user);

        return list;
    }

    @Override
    public T detail(@NonNull IQuery query, @NonNull IUser<?> user) {
        preDetailQueryConsumer.accept(query, user);
        T entity = repository.selectOne(IView.Detail.class, query);
        if (Objects.nonNull(entity)) {
            postDetailQueryConsumer.accept(entity, query, user);
            postDetailConsumer.accept(entity, user);
        }
        return entity;
    }

    @Override
    public T detail(@NonNull ID id, @NonNull IUser<?> user) {
        T entity = repository.selectOne(id);
        if (Objects.nonNull(entity)) {
            postDetailConsumer.accept(entity, user);
        }
        return entity;
    }

    @Override
    public Long count(@NonNull IQuery query, @NonNull IUser<?> user) {
        preCountQueryConsumer.accept(query, user);
        return repository.selectCount(query);
    }

    @Override
    public int delete(@NonNull IQuery query, @NonNull IUser<?> user) {
        preDeleteQueryConsumer.accept(query, user);
        List<T> entities = repository.select(query);
        if (CollectionUtils.isEmpty(entities)) {
            return 0;
        }
        preDeleteConsumer.accept(entities, user);
        int delete = repository.delete(entities.stream().map(T::getId).collect(Collectors.toList()));
        postDeleteQueryConsumer.accept(entities, query, user);
        postDeleteConsumer.accept(entities, user);
        return delete;
    }

    @Override
    public int delete(@NonNull ID id, @NonNull IUser<?> user) {
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
    public int update(@NonNull T entity, @NonNull ID id, boolean selective, @NonNull IUser<?> user) {
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
