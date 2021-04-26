/*
 * Copyright 2020-2021 the original author or authors.
 *
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

package org.ifinal.finalframework.mybatis.entity;

import org.ifinal.finalframework.core.IEntity;
import org.ifinal.finalframework.data.annotation.PrimaryKey;
import org.ifinal.finalframework.data.annotation.Reference;

import lombok.Getter;
import lombok.Setter;

/**
 * Person.
 *
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
@Setter
@Getter
public class Person implements IEntity<Long> {

    @PrimaryKey
    private Long id;

    private String name;

    private Integer age;

    @Reference(properties = {"id", "name"})
    private Person creator;

}
