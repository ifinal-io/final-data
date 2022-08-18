/*
 * Copyright 2020-2021 the original author or authors.
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

package org.ifinalframework.data.service;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.ParamsBuilder;
import org.ifinalframework.data.repository.Repository;

import lombok.Setter;

/**
 * 默认的{@link AbsService}实现，方便其子类通过 {@literal super}调用方法.
 *
 * @author ilikly
 * @version 1.0.0
 * @since 1.0.0
 */
public abstract class AbsServiceImpl<I extends Serializable, T extends IEntity<I>, R extends Repository<I, T>>
        implements AbsService<I, T, R>, SmartInitializingSingleton, ApplicationContextAware {

    private final R repository;
    private final List<ServiceListener<T>> listeners = new LinkedList<>();

    @Setter
    private ApplicationContext applicationContext;

    protected AbsServiceImpl(final R repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    @SuppressWarnings("unchecked")
    public int insert(Map<String, Object> params) {
        Collection<T> entities = (Collection<T>) params.get(ParamsBuilder.LIST_PARAM_NAME);
        listeners.forEach(it -> it.beforeInsert(entities));
        int rows = AbsService.super.insert(params);
        listeners.forEach(it -> it.afterInsert(entities, rows));
        return rows;
    }

    @Override
    public List<T> select(Map<String, Object> params) {
        List<T> entities = AbsService.super.select(params);
        listeners.forEach(listener -> listener.afterSelect(entities));
        return entities;
    }

    @Override
    @NonNull
    public final R getRepository() {
        return repository;
    }

    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public void afterSingletonsInstantiated() {
        Class<?> entityClass = ResolvableType.forInstance(this).as(Repository.class).resolveGeneric(1);
        ResolvableType resolvableType = ResolvableType.forClassWithGenerics(ServiceListener.class, entityClass);
        List objects = applicationContext.getBeanProvider(resolvableType)
                .orderedStream().collect(Collectors.toList());
        listeners.addAll(objects);
    }

    /*=========================================== Overridable ===========================================*/

}

