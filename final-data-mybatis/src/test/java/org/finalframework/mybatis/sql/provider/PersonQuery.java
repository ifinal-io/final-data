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

package org.finalframework.mybatis.sql.provider;

import org.finalframework.query.Direction;
import org.finalframework.query.annotation.Equal;
import org.finalframework.query.annotation.LessThan;
import org.finalframework.query.annotation.Limit;
import org.finalframework.query.annotation.Offset;
import org.finalframework.query.annotation.Order;

import java.awt.Point;

import lombok.Data;

/**
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class PersonQuery {

    @Equal
    private String name;

    private Point location;

    @LessThan(property = "name")
    private Long distance;

    @Offset
    private Integer offset;

    @Limit
    private Integer limit;

    @Order(property = "name", order = 1)
    private Direction orderByName = Direction.DESC;

    @Order(property = "age", order = 2)
    private String orderByAge = "ASC";

}

