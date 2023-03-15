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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;

import org.ifinalframework.core.IEntity;

/**
 * DomainResourceServiceFactoryBean.
 *
 * @author ilikly
 * @version 1.5.0
 * @since 1.5.0
 */
public class DomainResourceServiceFactoryBean<ID extends Serializable, T extends IEntity<ID>, S extends DomainResourceService<ID, T>>
        implements FactoryBean<S> {
    @Nullable
    @Override
    public S getObject() throws Exception {
        return null;
    }

    @Nullable
    @Override
    public Class<?> getObjectType() {
        return DomainResourceService.class;
    }
}
