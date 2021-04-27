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

package org.finalframework.data.mybatis.sql.provider;

import org.finalframework.data.mybatis.mapper.AbsMapper;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.builder.annotation.ProviderSqlSource;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

        final String sql = SqlProviderHelper.sql(PersonMapper.class, "insert", parameters);
        logger.info(sql);
        Assertions.assertNotNull(sql);

    }

    @Test
    void replace() throws NoSuchMethodException {

        final Method replace = AbsMapper.class
            .getMethod("replace", new Class[]{String.class, Class.class, Collection.class});
        final ProviderSqlSource providerSqlSource = new ProviderSqlSource(new Configuration(),
            replace.getAnnotation(InsertProvider.class), PersonMapper.class,
            replace);
        final HashMap<String, Object> parameters = new HashMap<>();

        parameters.put("table", "person");
        parameters.put("view", null);
        parameters.put("ignore", false);
        parameters.put("list", Arrays.asList(new Person()));

        final BoundSql boundSql = providerSqlSource.getBoundSql(parameters);

        final String sql = boundSql.getSql();
        logger.info(sql);
        Assertions.assertNotNull(sql);

        logger.info(SqlProviderHelper.sql(PersonMapper.class, "replace", parameters));

    }

    @Test
    void save() throws NoSuchMethodException {
        final Method save = AbsMapper.class.getMethod("save", new Class[]{String.class, Class.class, Collection.class});
        final ProviderSqlSource providerSqlSource = new ProviderSqlSource(new Configuration(),
            save.getAnnotation(InsertProvider.class), PersonMapper.class,
            save);
        final HashMap<String, Object> parameters = new HashMap<>();

        parameters.put("table", "person");
        parameters.put("view", null);
        parameters.put("view", null);
        parameters.put("ignore", false);
        parameters.put("list", Arrays.asList(new Person()));

        final BoundSql boundSql = providerSqlSource.getBoundSql(parameters);

        final String sql = boundSql.getSql();
        Assertions.assertNotNull(sql);

        logger.info(sql);
    }

}
