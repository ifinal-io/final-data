/*
 * Copyright 2020-2021 the original author or authors.
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

package org.ifinalframework.sharding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * ShardingHelperTest.
 *
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
class ShardingHelperTest {

    @Test
    void parseDataNodes() {

        List<String> nodes = Arrays.asList(
            "ds0.person_0",
            "ds0.person_1",
            "ds1.person_0",
            "ds1.person_1"
        );

        List<String> dataNodes = ShardingHelper.parseDataNodes("ds${0..1}.person_${0..1}");
        assertEquals(nodes.size(), dataNodes.size());
        assertTrue(dataNodes.containsAll(nodes));

    }

}
