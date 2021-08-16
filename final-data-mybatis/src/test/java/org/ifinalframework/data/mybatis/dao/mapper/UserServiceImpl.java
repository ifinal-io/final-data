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

import org.ifinalframework.data.mybatis.dao.query.UserQuery;
import org.ifinalframework.data.mybatis.entity.User;
import org.springframework.lang.NonNull;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import java.util.Objects;

/**
 * UserServiceImpl.
 *
 * @author likly
 * @version 1.2.2
 * @since 1.2.2
 */
class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @NonNull
    @Override
    public User login(@NotBlank String name, @NotBlank String password) {

        final User user = userMapper.selectOne(new UserQuery(name, password));

        if (Objects.isNull(user)) {
            throw new UserOrPasswordNotMatchedException();
        }

        return user;

    }
}
