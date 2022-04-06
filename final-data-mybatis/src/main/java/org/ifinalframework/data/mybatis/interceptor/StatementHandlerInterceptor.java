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

package org.ifinalframework.data.mybatis.interceptor;

import java.sql.Connection;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.ifinalframework.context.user.UserContextHolder;

import lombok.extern.slf4j.Slf4j;

/**
 * StatementHandlerInterceptor.
 *
 * @author ilikly
 * @version 1.3.0
 * @since 1.3.0
 */

@Slf4j
@Order(0)
@Component
@Intercepts(
        {
                @Signature(type = StatementHandler.class, method = "prepare",args = {Connection.class,Integer.class}),
        }
)
public class StatementHandlerInterceptor implements Interceptor {

    public StatementHandlerInterceptor() {
        logger.info(getClass().getSimpleName());
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        StatementHandler handler = (StatementHandler) invocation.getTarget();

        BoundSql boundSql = handler.getBoundSql();
        boundSql.setAdditionalParameter("USER", UserContextHolder.getUser());


        return invocation.proceed();
    }
}
