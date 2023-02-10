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

package org.ifinalframework.data.spi.composite;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.data.spi.QueryConsumer;
import org.ifinalframework.data.spi.QueryPredicate;

/**
 * QueryConsumerComposite.
 *
 * @author ilikly
 * @version 1.4.2
 * @since 1.4.2
 */
public class QueryConsumerComposite implements QueryConsumer<IQuery, IEntity<Long>> {

    private final List<QueryConsumer<IQuery, IEntity<Long>>> list;

    public QueryConsumerComposite() {
        this(new ArrayList<>());
    }

    public QueryConsumerComposite(List<QueryConsumer<IQuery, IEntity<Long>>> list) {
        this.list = list;
    }

    @Override
    public void accept(@NonNull IQuery query, @NonNull Class<IEntity<Long>> clazz) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        for (QueryConsumer<IQuery, IEntity<Long>> consumer : list) {
            if (consumer instanceof QueryPredicate) {
                if (((QueryPredicate) consumer).test(query, clazz)) {
                    consumer.accept(query, clazz);
                }
            } else {
                consumer.accept(query, clazz);
            }
        }

    }
}


