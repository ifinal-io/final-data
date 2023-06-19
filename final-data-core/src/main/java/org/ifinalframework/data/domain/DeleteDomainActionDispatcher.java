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

import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IUser;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.data.spi.DeleteFunction;
import org.ifinalframework.data.spi.SpiAction;

import java.io.Serializable;
import java.util.List;

/**
 * AbsUpdateDomainAction.
 *
 * @author ilikly
 * @version 1.5.0
 * @since 1.5.0
 */
public class DeleteDomainActionDispatcher<ID extends Serializable, T extends IEntity<ID>, P, U extends IUser<?>>
        extends AbsUpdateDeleteDomainActionDispatcher<ID, T, P, Void, U> {
    private final DeleteFunction<T, P, U> updateAction;

    public DeleteDomainActionDispatcher(SpiAction spiAction, Repository<ID, T> repository, DeleteFunction<T, P, U> updateAction) {
        super(spiAction, repository);
        this.updateAction = updateAction;
    }

    @Override
    protected Integer doInterAction(List<T> entities, P query, Void value, U user) {
        return updateAction.delete(entities, query, user);
    }
}
