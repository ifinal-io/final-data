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

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IStatus;
import org.ifinalframework.core.IUser;
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.spi.PreInsertFunction;

/**
 * DomainService.
 *
 * @author ilikly
 * @version 1.4.3
 * @since 1.4.3
 */
public interface DomainService<ID extends Serializable, T extends IEntity<ID>> {

    @NonNull
    Class<T> entityClass();

    @Nullable
    Class<?> domainEntityClass(Class<?> prefix);

    Class<? extends IQuery> domainQueryClass(Class<?> prefix);

    PreInsertFunction<Object, IUser<?>, T> preInsertFunction();

    /**
     * @param entities
     * @param user
     * @return
     * @see org.ifinalframework.data.spi.PreInsertConsumer
     * @see org.ifinalframework.data.spi.PostInsertConsumer
     */
    Integer create(List<T> entities, IUser<?> user);

    /**
     * @param query
     * @param user
     * @return
     * @see org.ifinalframework.data.spi.PreQueryConsumer
     * @see org.ifinalframework.data.spi.PostQueryConsumer
     */
    List<T> list(IQuery query, IUser<?> user);

    T detail(ID id, IUser<?> user);

    Long count(IQuery query, IUser<?> user);

    /**
     * @param id
     * @param user
     * @return
     * @see org.ifinalframework.data.spi.PreDeleteConsumer
     * @see org.ifinalframework.data.spi.PostDeleteConsumer
     */
    int delete(ID id, IUser<?> user);

    /**
     * @param entity
     * @param id
     * @param selective
     * @param user
     * @return
     * @see org.ifinalframework.data.spi.PreUpdateConsumer
     * @see org.ifinalframework.data.spi.PostUpdateConsumer
     */
    int update(T entity, ID id, boolean selective, IUser<?> user);

    /**
     * @param id
     * @param yn
     * @param user
     * @see org.ifinalframework.data.spi.PreUpdateYnValidator
     */
    int yn(ID id, YN yn, IUser<?> user);

    int status(ID id, IStatus<?> status, IUser<?> user);


}
