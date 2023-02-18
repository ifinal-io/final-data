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
import org.ifinalframework.core.IEnum;
import org.ifinalframework.core.IQuery;
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

    @NonNull
    Class<? extends IQuery> domainQueryClass(Class<?> prefix);

    @Nullable
    PreInsertFunction<Object, IUser<?>, T> preInsertFunction();

    /**
     * @param entities the entities to create.
     * @param user     the current user.
     * @return created rows.
     * @see org.ifinalframework.core.IView.Create
     * @see org.ifinalframework.data.spi.PreInsertFilter
     * @see org.ifinalframework.data.spi.PreInsertConsumer
     * @see org.ifinalframework.data.spi.PostInsertConsumer
     */
    Integer create(@NonNull List<T> entities, @NonNull IUser<?> user);

    /**
     * @param query the query of list.
     * @param user  the current user.
     * @return entity list of matches query.
     * @see org.ifinalframework.core.IView.List
     * @see org.ifinalframework.data.spi.PreQueryConsumer
     * @see org.ifinalframework.data.spi.PostQueryConsumer
     */
    List<T> list(@NonNull IQuery query, @NonNull IUser<?> user);

    /**
     * @param query the query of detail.
     * @param user  the current user.
     * @return the detail entity of matches detail query.
     * @see org.ifinalframework.core.IView.Detail
     * @see org.ifinalframework.data.spi.PreDeleteQueryConsumer
     * @see org.ifinalframework.data.spi.PostDetailQueryConsumer
     * @see org.ifinalframework.data.spi.PostDetailConsumer
     */
    T detail(@NonNull IQuery query, @NonNull IUser<?> user);

    /**
     * @param id   the id of entity.
     * @param user the current user.
     * @return the detail entity of {@code id}.
     * @see org.ifinalframework.core.IView.Detail
     * @see org.ifinalframework.data.spi.PostDetailConsumer
     */
    T detail(@NonNull ID id, @NonNull IUser<?> user);

    /**
     * @param query the query of count.
     * @param user  the current user.
     * @return the count of query.
     * @see org.ifinalframework.core.IView.Count
     * @see org.ifinalframework.data.spi.PreCountQueryConsumer
     */
    Long count(@NonNull IQuery query, @NonNull IUser<?> user);

    /**
     * @param query the query of deleted.
     * @param user  the current user.
     * @return deleted rows.
     * @see org.ifinalframework.core.IView.Delete
     * @see org.ifinalframework.data.spi.PreDeleteQueryConsumer
     * @see org.ifinalframework.data.spi.PreDeleteConsumer
     * @see org.ifinalframework.data.spi.PostDeleteQueryConsumer
     * @see org.ifinalframework.data.spi.PostDeleteConsumer
     */
    int delete(@NonNull IQuery query, @NonNull IUser<?> user);

    /**
     * @param id   the id of deleted.
     * @param user the current user.
     * @return deleted rows.
     * @throws org.ifinalframework.context.exception.NotFoundException throw this exception when can not find entity by the {@code id}.
     * @see org.ifinalframework.core.IView.Delete
     * @see org.ifinalframework.data.spi.PreDeleteConsumer
     * @see org.ifinalframework.data.spi.PostDeleteConsumer
     */
    int delete(@NonNull ID id, @NonNull IUser<?> user);

    /**
     * @param entity
     * @param id
     * @param selective
     * @param user
     * @return
     * @see org.ifinalframework.core.IView.Update
     * @see org.ifinalframework.data.spi.PreUpdateConsumer
     * @see org.ifinalframework.data.spi.PostUpdateConsumer
     */
    int update(@NonNull T entity, @NonNull ID id, boolean selective, @NonNull IUser<?> user);

    /**
     * @param id
     * @param yn
     * @param user
     * @see org.ifinalframework.data.spi.PreUpdateYnValidator
     * @see org.ifinalframework.data.spi.PostUpdateYNConsumer
     */
    int yn(@NonNull ID id, @NonNull YN yn, @NonNull IUser<?> user);

    int status(@NonNull ID id, @NonNull IEnum<?> status, @NonNull IUser<?> user);


}
