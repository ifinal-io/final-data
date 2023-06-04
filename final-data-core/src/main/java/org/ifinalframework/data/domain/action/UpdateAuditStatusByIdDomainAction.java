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

package org.ifinalframework.data.domain.action;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IUser;
import org.ifinalframework.data.domain.model.AuditValue;
import org.ifinalframework.data.query.Update;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.data.spi.SpiAction;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * UpdateStatusByIdDomainAction.
 *
 * @author ilikly
 * @version 1.5.1
 * @since 1.5.1
 */
public class UpdateAuditStatusByIdDomainAction<ID extends Serializable, T extends IEntity<ID>, U extends IUser<?>>
        extends AbsUpdateDomainAction<ID, T, ID, AuditValue, Integer, U> {
    public UpdateAuditStatusByIdDomainAction(Repository<ID, T> repository) {
        super(SpiAction.UPDATE_STATUS, repository);
    }

    @Override
    protected List<T> doActionPrepare(ID query, AuditValue value, U user) {
        return repository.select(query);
    }

    @Override
    protected Integer doActionInternal(List<T> list, ID query, AuditValue value, U user) {
        Update update = Update.update()
                .set("audit_status", value.getStatus())
                .set("audit_content", value.getContent())
                .set("audit_date_time", LocalDateTime.now())
                .set("auditor_id", user.getId())
                .set("auditor_name", user.getName());
        return repository.update(update, query);
    }
}
