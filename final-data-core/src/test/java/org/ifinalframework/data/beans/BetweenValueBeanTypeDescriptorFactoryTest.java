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

package org.ifinalframework.data.beans;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;

import org.ifinalframework.data.query.BetweenValue;

import org.junit.jupiter.api.Test;

/**
 * BetweenValueBeanTypeDescriptorFactoryTest.
 *
 * @author ilikly
 * @version 1.5.0
 * @since 1.5.0
 */
class BetweenValueBeanTypeDescriptorFactoryTest {

    private BetweenValueBeanTypeDescriptorFactory beanTypeDescriptorFactory = new BetweenValueBeanTypeDescriptorFactory();

    @Test
    void create() {
        final ResolvableType resolvableType = ResolvableType.forClassWithGenerics(BetweenValue.class, Integer.class);
        final BetweenValue betweenValue = beanTypeDescriptorFactory.create(Integer.class, new TypeDescriptor(resolvableType, null, null));
        betweenValue.setMax(1);
        betweenValue.setMin(2);
    }
}