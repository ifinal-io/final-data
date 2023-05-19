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

package org.ifinalframework.data.domain.action;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IUser;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.data.spi.AfterReturningQueryConsumer;
import org.ifinalframework.data.spi.AfterThrowingQueryConsumer;
import org.ifinalframework.data.spi.Consumer;
import org.ifinalframework.data.spi.Function;
import org.ifinalframework.data.spi.PostQueryConsumer;
import org.ifinalframework.data.spi.PreQueryConsumer;
import org.ifinalframework.data.spi.SpiAction;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * ListDomainAction.
 *
 * @author ilikly
 * @version 1.5.0
 * @since 1.5.0
 */
@Setter
@RequiredArgsConstructor
public abstract class AbsSelectDomainAction<ID extends Serializable, T extends IEntity<ID>, Q, R, U extends IUser<?>> implements DomainAction<Q, Void, U> {

    private final SpiAction spiAction;
    protected final Repository<ID, T> repository;

    private PreQueryConsumer<Q, U> preQueryConsumer;
    private Consumer<T, U> postConsumer;
    private PostQueryConsumer<T, Q, U> postQueryConsumer;
    private Function<R, Q, U> postQueryFunction;
    private AfterThrowingQueryConsumer<T, Q, IUser<?>> afterThrowingQueryConsumer;
    private AfterReturningQueryConsumer<T, Q, IUser<?>> afterReturningQueryConsumer;

    @Override
    public Object doAction(Q query, Void value, U user) {
        R result = null;
        List<T> list = null;
        Throwable throwable = null;
        try {
            if (Objects.nonNull(preQueryConsumer)) {
                preQueryConsumer.accept(spiAction, query, user);
            }
            result = doActionInternal(query, user);
            list = map(result);
            if (Objects.nonNull(postConsumer)) {
                postConsumer.accept(spiAction, SpiAction.Advice.POST, list, user);
            }
            if (Objects.nonNull(postQueryConsumer)) {
                postQueryConsumer.accept(spiAction, list, query, user);
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

    protected abstract R doActionInternal(Q query, U user);

    @SuppressWarnings("unchecked")
    protected List<T> map(R result) {
        if (Objects.isNull(result)) {
            return Collections.emptyList();
        }
        return result instanceof Collection ? (List<T>) result : (List<T>) Collections.singletonList(result);
    }
}
