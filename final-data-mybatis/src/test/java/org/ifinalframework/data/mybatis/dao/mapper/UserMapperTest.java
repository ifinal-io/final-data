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

package org.ifinalframework.data.mybatis.dao.mapper;

import org.ifinalframework.data.mybatis.dao.query.PersonQuery;
import org.ifinalframework.data.mybatis.entity.User;
import org.ifinalframework.data.repository.Repository;
import org.ifinalframework.query.Direction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ResolvableType;

import javax.annotation.Resource;

/**
 * UserMapperTest.
 *
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
@SpringBootTest
class UserMapperTest {

    @Resource
    private UserMapper userMapper;

    @Test
    void test() {
        User user = new User();
        user.setName("name");
        user.setAge(1);
        int save = userMapper.insert(user);
        Assertions.assertEquals(1, save);
        User db = userMapper.selectOne(1L);
        Assertions.assertEquals(user.getName(), db.getName());
    }

    @Test
    void orderQuery() {
        User user = new User();
        user.setName("name");
        user.setAge(1);
        userMapper.insert(user);
        user.setAge(100);
        userMapper.insert(user);
        PersonQuery query = new PersonQuery();
        query.setOrderByName(Direction.ASC);
        query.setOrderByAge(Direction.DESC.name());
        query.setLimit(1);
        User result = userMapper.selectOne(query);

        Assertions.assertEquals(100, result.getAge());

    }

    @Test
    void test2() {
        Class<?> entity = ResolvableType.forClass(UserMapper.class).as(Repository.class).resolveGeneric(1);
        Assertions.assertEquals(User.class, entity);
    }

}
