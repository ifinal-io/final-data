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

package org.ifinalframework.data.rest.model;

import java.io.Serializable;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IUser;
import org.ifinalframework.data.service.AbsService;
import org.ifinalframework.data.spi.PostDeleteConsumer;
import org.ifinalframework.data.spi.PostInsertConsumer;
import org.ifinalframework.data.spi.PostQueryConsumer;
import org.ifinalframework.data.spi.PostUpdateConsumer;
import org.ifinalframework.data.spi.PreDeleteConsumer;
import org.ifinalframework.data.spi.PreInsertConsumer;
import org.ifinalframework.data.spi.PreInsertFunction;
import org.ifinalframework.data.spi.PreQueryConsumer;
import org.ifinalframework.data.spi.PreUpdateConsumer;
import org.ifinalframework.data.spi.PreUpdateYnValidator;

import lombok.Builder;
import lombok.Getter;

/**
 * ResourceEntity.
 *
 * @author ilikly
 * @version 1.4.2
 * @since 1.4.2
 */
@Getter
@Builder
public class ResourceEntity implements Serializable {
    private final String resource;
    private final Class<? extends IQuery> queryClass;
    private final AbsService<Long, IEntity<Long>> service;
    private final Class<? extends IEntity<Long>> entityClass;

    private final PreQueryConsumer<IQuery, IUser<?>> preQueryConsumer;
    private final PostQueryConsumer<IEntity<Long>, IQuery, IUser<?>> postQueryConsumer;
    private final PreInsertConsumer<IEntity<Long>, IUser<?>> preInsertConsumer;
    private final PostInsertConsumer<IEntity<Long>, IUser<?>> postInsertConsumer;
    private final PreUpdateYnValidator<IEntity<Long>, IUser<?>> preUpdateYnValidator;

    private final PreUpdateConsumer<IEntity<Long>, IUser<?>> preUpdateConsumer;
    private final PostUpdateConsumer<IEntity<Long>, IUser<?>> postUpdateConsumer;

    private final PreDeleteConsumer<IEntity<Long>, IUser<?>> preDeleteConsumer;
    private final PostDeleteConsumer<IEntity<Long>, IUser<?>> postDeleteConsumer;


    private final Class<?> createEntityClass;
    private final PreInsertFunction preInsertFunction;

}


