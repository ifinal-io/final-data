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
import org.ifinalframework.data.service.AbsService;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ResourceEntity.
 *
 * @author ilikly
 * @version 1.4.2
 * @since 1.4.2
 */
@Getter
@RequiredArgsConstructor
public class ResourceEntity implements Serializable {
    private final String resource;
    private final Class<? extends IQuery> queryClass;
    private final AbsService<Long, IEntity<Long>> service;
    private final Class<? extends IEntity<Long>> entityClass;

}


