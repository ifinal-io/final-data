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

package org.ifinalframework.data.mapping;

import java.util.Collections;
import java.util.Objects;

import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultEntityFactoryTest.
 *
 * @author ilikly
 * @version 1.3.0
 * @since 1.3.0
 */
class DefaultEntityFactoryTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"vendor","_tenant"})
    void create(String value) {
        StandardEnvironment standardEnvironment = new StandardEnvironment();
        standardEnvironment.getPropertySources().addLast(new MapPropertySource("map",Collections.singletonMap("spring.tenant",value)));
        DefaultEntityFactory factory = new DefaultEntityFactory(standardEnvironment);
        Entity<MyEntity> entity = factory.create(MyEntity.class);
        Property tenant = entity.getRequiredPersistentProperty("tenant");
        if(Objects.isNull(value)) {
            Assertions.assertEquals("haha", tenant.getColumn());
        }else {
            Assertions.assertEquals(value, tenant.getColumn());
        }

    }
}