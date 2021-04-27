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

package org.finalframework.mybatis.dao.mapper;

import org.springframework.boot.test.context.SpringBootTest;

import org.finalframework.mybatis.dao.query.PersonQuery;
import org.finalframework.mybatis.entity.User;
import org.finalframework.query.Direction;

import javax.annotation.Resource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        User db = userMapper.selectOne(user.getId());
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

}
