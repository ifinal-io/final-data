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

import org.ifinalframework.data.mybatis.entity.User;
import org.springframework.lang.NonNull;

import javax.validation.constraints.NotBlank;

/**
 * UserService.
 *
 * @author ilikly
 * @version 1.2.2
 * @since 1.2.2
 */
public interface UserService {

    /**
     * 用户登录
     *
     * @param name     用户名
     * @param password 密码
     * @return 用户信息
     * @throws UserOrPasswordNotMatchedException 当用户名或密码不匹配时，抛出该异常。
     */
    @NonNull
    User login(@NotBlank String name, @NotBlank String password);

    class UserOrPasswordNotMatchedException extends RuntimeException {
        public UserOrPasswordNotMatchedException() {
            super("用户名或密码不正确");
        }
    }
}
