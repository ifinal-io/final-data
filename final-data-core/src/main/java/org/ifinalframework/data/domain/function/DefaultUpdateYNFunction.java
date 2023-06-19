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

package org.ifinalframework.data.domain.function;

import lombok.RequiredArgsConstructor;
import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IUser;
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.query.Update;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.data.spi.UpdateFunction;

import java.io.Serializable;
import java.util.List;

/**
 * DefaultUpdateLockedAction.
 *
 * @author ilikly
 * @version 1.5.1
 * @since 1.5.1
 */
@RequiredArgsConstructor
public class DefaultUpdateYNFunction<ID extends Serializable, T extends IEntity<ID>, P, U extends IUser<?>> implements UpdateFunction<T, P, YN, U> {
    private final Repository<ID, T> repository;

    @Override
    public Integer update(List<T> entities, P param, YN value, U user) {
        Update update = Update.update().set("yn", value);
        if (param instanceof IQuery) {
            return repository.update(update, (IQuery) param);
        } else {
            return repository.update(update, (ID) param);
        }
    }
}
