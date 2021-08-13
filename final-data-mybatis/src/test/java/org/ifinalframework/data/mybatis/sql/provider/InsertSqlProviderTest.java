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

package org.ifinalframework.data.mybatis.sql.provider;

import lombok.extern.slf4j.Slf4j;
import org.ifinalframework.data.mybatis.sql.util.SqlHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;

/**
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
class InsertSqlProviderTest {

    @Test
    void insert() {

        final HashMap<String, Object> parameters = new HashMap<>();

        parameters.put("table", "person");
        parameters.put("view", null);
        parameters.put("ignore", false);
        parameters.put("list", Arrays.asList(new Person()));

        String xml = SqlHelper.xml(PersonMapper.class, "insert", parameters);
        logger.info(xml);

        final String sql = SqlHelper.sql(PersonMapper.class, "insert", parameters);
        logger.info(sql);
        Assertions.assertNotNull(sql);

    }

    @Test
    void replace() throws NoSuchMethodException {


        final HashMap<String, Object> parameters = new HashMap<>();

        parameters.put("table", "person");
        parameters.put("view", null);
        parameters.put("ignore", false);
        parameters.put("list", Arrays.asList(new Person()));

        final String sql = SqlHelper.sql(PersonMapper.class, "replace", parameters);
        logger.info(sql);
        Assertions.assertNotNull(sql);

        logger.info(SqlHelper.sql(PersonMapper.class, "replace", parameters));

    }

    @Test
    void save() throws NoSuchMethodException {

        final HashMap<String, Object> parameters = new HashMap<>();

        parameters.put("table", "person");
        parameters.put("view", null);
        parameters.put("view", null);
        parameters.put("ignore", false);
        parameters.put("list", Arrays.asList(new Person()));

        final String sql = SqlHelper.sql(PersonMapper.class, "save", parameters);
        Assertions.assertNotNull(sql);

        logger.info(sql);
    }

}
