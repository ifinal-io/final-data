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
import org.ifinalframework.data.spi.SpiAction;
import org.ifinalframework.data.spi.UpdateFunction;

import java.io.Serializable;
import java.util.List;

import lombok.Setter;

/**
 * AbsUpdateDomainAction.
 *
 * @author ilikly
 * @version 1.5.0
 * @since 1.5.0
 */
@Setter
public class UpdateDomainActionDispatcher<K extends Serializable, T extends IEntity<K>, P, V, U extends IUser<?>>
        extends AbsUpdateDeleteDomainActionDispatcher<K, T, P, Void, V, U> {
    private final UpdateFunction<T, P, V, U> updateAction;

    public UpdateDomainActionDispatcher(SpiAction spiAction, Repository<K, T> repository, UpdateFunction<T, P, V, U> updateAction) {
        super(spiAction, repository);
        this.updateAction = updateAction;
    }

    @Override
    protected Integer doInterAction(List<T> entities, P param, Void p2, V value, U user) {
        return updateAction.update(entities, param, value, user);
    }
}
