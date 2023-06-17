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
import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IUser;
import org.ifinalframework.core.Viewable;
import org.ifinalframework.data.spi.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ListDomainAction.
 *
 * @author ilikly
 * @version 1.5.0
 * @since 1.5.0
 */
@Setter
@RequiredArgsConstructor
public class SelectDomainDispatcher<ID extends Serializable, T extends IEntity<ID>, P, U extends IUser<?>, R> implements DomainActionDispatcher<P, Void, U> {

    private final SpiAction spiAction;
    private final Class<?> defaultView;
    private final SelectFunction<P, U, R> selectFunction;

    private PreQueryConsumer<P, U> preQueryConsumer;
    private Consumer<T, U> postConsumer;
    private BiConsumer<T, P, U> postQueryConsumer;
    private Function<R, P, U> postQueryFunction;
    private AfterThrowingQueryConsumer<T, P, U> afterThrowingQueryConsumer;
    private AfterReturningQueryConsumer<T, P, U> afterReturningQueryConsumer;

    @Override
    public Object doAction(P param, Void value, U user) {
        R result = null;
        List<T> list = null;
        Throwable throwable = null;

        if (param instanceof Viewable) {
            final Viewable viewable = (Viewable) param;
            if (Objects.isNull(viewable.getView())) {
                viewable.setView(defaultView);
            }
        }

        try {
            if (Objects.nonNull(preQueryConsumer)) {
                preQueryConsumer.accept(spiAction, param, user);
            }
            result = selectFunction.select(param, user);
            list = map(result);
            if (Objects.nonNull(postConsumer)) {
                postConsumer.accept(spiAction, SpiAction.Advice.POST, list, user);
            }
            if (Objects.nonNull(postQueryConsumer)) {
                postQueryConsumer.accept(spiAction, SpiAction.Advice.POST, list, param, user);
            }
            if (Objects.nonNull(postQueryFunction)) {
                return postQueryFunction.map(result, param, user);
            }

            return result;
        } catch (Exception e) {
            throwable = e;
            if (Objects.nonNull(afterThrowingQueryConsumer)) {
                afterThrowingQueryConsumer.accept(spiAction, list, param, user, e);
            }
            throw e;
        } finally {
            if (Objects.nonNull(afterReturningQueryConsumer)) {
                afterReturningQueryConsumer.accept(spiAction, list, param, user, throwable);
            }
        }
    }


    @SuppressWarnings("unchecked")
    protected List<T> map(R result) {
        if (Objects.isNull(result)) {
            return Collections.emptyList();
        }
        return result instanceof Collection ? (List<T>) result : (List<T>) Collections.singletonList(result);
    }
}
