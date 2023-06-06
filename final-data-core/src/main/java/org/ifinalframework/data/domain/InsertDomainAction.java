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
import java.util.Objects;
import java.util.stream.Collectors;

import org.ifinalframework.data.domain.DomainActionDispatcher;
import org.springframework.util.CollectionUtils;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IUser;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.data.spi.AfterReturningConsumer;
import org.ifinalframework.data.spi.AfterThrowingConsumer;
import org.ifinalframework.data.spi.Consumer;
import org.ifinalframework.data.spi.Filter;
import org.ifinalframework.data.spi.Function;
import org.ifinalframework.data.spi.SpiAction;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * InsertDomainAction.
 *
 * @author ilikly
 * @version 1.5.0
 * @since 1.5.0
 */
@Setter
@RequiredArgsConstructor
public class InsertDomainAction<ID extends Serializable, T extends IEntity<ID>, U extends IUser<?>> implements DomainActionDispatcher<Void, List<T>, U> {
    private final Repository<ID, T> repository;
    private final boolean insertIgnore;

    // create
    private Filter<T, U> preInsertFilter;
    private Consumer<T, U> preInsertConsumer;
    private Consumer<T, U> postInsertConsumer;
    private Function<Integer, List<T>, U> postInsertFunction;
    private AfterThrowingConsumer<T, U> afterThrowingInsertConsumer;
    private AfterReturningConsumer<T, Integer, U> afterReturningInsertConsumer;

    @Override
    public Object doAction(Void query, List<T> entities, U user) {
        Integer result = null;
        Throwable exception = null;

        try {

            if (Objects.nonNull(preInsertFilter)) {
                entities = entities.stream().filter(item -> preInsertFilter.test(SpiAction.CREATE, item, user)).collect(Collectors.toList());
            }


            if (CollectionUtils.isEmpty(entities)) {
                return result = 0;
            }

            if (Objects.nonNull(preInsertConsumer)) {
                preInsertConsumer.accept(SpiAction.CREATE, SpiAction.Advice.PRE, entities, user);
            }
            result = repository.insert(insertIgnore, entities);
            if (Objects.nonNull(postInsertConsumer)) {
                postInsertConsumer.accept(SpiAction.CREATE, SpiAction.Advice.POST, entities, user);
            }

            if (Objects.nonNull(postInsertFunction)) {
                return postInsertFunction.map(result, entities, user);
            }

            return result;
        } catch (Throwable e) {
            exception = e;
            if (Objects.nonNull(afterThrowingInsertConsumer)) {
                afterThrowingInsertConsumer.accept(SpiAction.CREATE, entities, user, exception);
            }
            throw e;
        } finally {
            if (Objects.nonNull(afterReturningInsertConsumer)) {
                afterReturningInsertConsumer.accept(SpiAction.CREATE, entities, result, user, exception);
            }
        }
    }
}
