/*
 * Copyright 2020-2022 the original author or authors.
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

package org.ifinalframework.data.spi;

import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IUser;

/**
 * PreQueryConsumerComposite.
 *
 * @author ilikly
 * @version 1.4.2
 * @since 1.4.2
 */
public class PreQueryConsumerComposite implements PreQueryConsumer<IQuery, IUser<?>> {
    private final List<PreQueryConsumer<IQuery, IUser<?>>> consumers;

    public PreQueryConsumerComposite(List<PreQueryConsumer<IQuery, IUser<?>>> consumers) {
        this.consumers = consumers;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void accept(@NonNull IQuery query, @Nullable IUser<?> iUser) {
        if (CollectionUtils.isEmpty(consumers)) {
            return;
        }

        for (PreQueryConsumer<IQuery, IUser<?>> consumer : consumers) {
            if (consumer instanceof PreQueryPredicate) {
                if (((PreQueryPredicate<IQuery, IUser<?>>) consumer).test(query, iUser)) {
                    consumer.accept(query, iUser);
                }
            } else {
                consumer.accept(query, iUser);
            }
        }

    }
}


