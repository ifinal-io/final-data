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
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import org.springframework.util.CollectionUtils;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IQuery;

/**
 * After entity found.
 *
 * @author ilikly
 * @version 1.4.2
 * @since 1.4.2
 */
public class EntityQueryConsumerComposite implements EntityQueryConsumer<IEntity<Long>, IQuery> {
    private final List<BiConsumer<List<IEntity<Long>>, IQuery>> consumers;

    public EntityQueryConsumerComposite(List<BiConsumer<List<IEntity<Long>>, IQuery>> consumers) {
        this.consumers = consumers;
    }

    @Override
    public void accept(List<IEntity<Long>> entities, IQuery query) {
        if (CollectionUtils.isEmpty(consumers)) {
            return;
        }

        for (BiConsumer<List<IEntity<Long>>, IQuery> consumer : consumers) {
            if (consumer instanceof BiPredicate) {
                boolean test = ((BiPredicate<List<IEntity<Long>>, IQuery>) consumer).test(entities, query);
                if (test) {
                    consumer.accept(entities, query);
                }
            } else {
                consumer.accept(entities, query);
            }
        }


    }
}
