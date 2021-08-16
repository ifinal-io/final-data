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

import org.ifinalframework.core.IQuery;
import org.ifinalframework.data.mybatis.dao.mapper.UserService.UserOrPasswordNotMatchedException;
import org.ifinalframework.data.mybatis.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * UserServiceImplTest.
 *
 * @author likly
 * @version 1.2.2
 * @since 1.2.2
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserMapper userMapper;

    @Test
    void should_login_success_when_found_user() {

        when(userMapper.selectOne(any(IQuery.class))).thenReturn(new User("user", "password"));

        final User user = userService.login("user", "password");

        assertNotNull(user);
        assertEquals("user", user.getName());

    }


    @Test
    void should_throw_exception_when_not_found_user() {

        final UserOrPasswordNotMatchedException exception = assertThrows(UserOrPasswordNotMatchedException.class, () -> userService.login("user", "password"));
        assertEquals("用户名或密码不正确", exception.getMessage());

    }
}
