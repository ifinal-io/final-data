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
import lombok.Setter;
import org.ifinalframework.context.exception.NotFoundException;
import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IUser;
import org.ifinalframework.data.domain.action.DomainAction;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.data.spi.*;
import org.ifinalframework.json.Json;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * AbsUpdateDomainAction.
 *
 * @author ilikly
 * @version 1.5.0
 * @since 1.5.0
 */
@Setter
@RequiredArgsConstructor
public class UpdateDomainActionDispatcher<ID extends Serializable, T extends IEntity<ID>, Q, V, U extends IUser<?>>
        implements DomainAction<Q, V, U> {
    private final SpiAction spiAction;
    private final Repository<ID, T> repository;

    private PreQueryConsumer<Q, U> preQueryConsumer;

    private BiValidator<T, V, U> preUpdateValidator;
    private Consumer<T, U> preConsumer;
    private BiConsumer<T, V, U> preUpdateConsumer;
    private UpdateAction<T, Q, V, U> updateAction;
    private BiConsumer<T, V, U> postUpdateConsumer;
    private Consumer<T, U> postConsumer;

    private BiConsumer<T, Q, U> postQueryConsumer;
    private Function<Integer, Q, U> postQueryFunction;
    private AfterThrowingQueryConsumer<T, Q, U> afterThrowingQueryConsumer;
    private AfterReturningQueryConsumer<T, Q, U> afterReturningQueryConsumer;

    @Override
    public Object doAction(Q query, V value, U user) {

        Integer result = null;
        List<T> list = null;
        Throwable throwable = null;
        try {

            if (Objects.nonNull(preQueryConsumer)) {
                preQueryConsumer.accept(spiAction, query, user);
            }

            list = doActionPrepare(query, value, user);

            if (CollectionUtils.isEmpty(list)) {
                throw new NotFoundException("not found target entities: {}", Json.toJson(query));
            }

            if (Objects.nonNull(preUpdateValidator)) {
                preUpdateValidator.validate(list, value, user);
            }

            if (Objects.nonNull(preConsumer)) {
                preConsumer.accept(spiAction, SpiAction.Advice.PRE, list, user);
            }

            if (Objects.nonNull(preUpdateConsumer)) {
                preUpdateConsumer.accept(spiAction, SpiAction.Advice.PRE, list, value, user);
            }

            result = updateAction.update(list, query, value, user);


            if (Objects.nonNull(postUpdateConsumer)) {
                postUpdateConsumer.accept(spiAction, SpiAction.Advice.POST, list, value, user);
            }

            if (Objects.nonNull(postConsumer)) {
                postConsumer.accept(spiAction, SpiAction.Advice.POST, list, user);
            }


            if (Objects.nonNull(postQueryConsumer)) {
                postQueryConsumer.accept(spiAction, SpiAction.Advice.POST, list, query, user);
            }

            if (Objects.nonNull(postQueryFunction)) {
                return postQueryFunction.map(result, query, user);
            }

            return result;
        } catch (Exception e) {
            throwable = e;
            if (Objects.nonNull(afterThrowingQueryConsumer)) {
                afterThrowingQueryConsumer.accept(spiAction, list, query, user, e);
            }
            throw e;
        } finally {
            if (Objects.nonNull(afterReturningQueryConsumer)) {
                afterReturningQueryConsumer.accept(spiAction, list, query, user, throwable);
            }
        }


    }

    protected List<T> doActionPrepare(Q query, V value, U user) {
        if (query instanceof IQuery) {
            return repository.select((IQuery) query);
        } else {
            return repository.select((ID) query);
        }
    }

}
