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

package org.ifinalframework.data.mybatis.interceptor;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Map;

import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.defaults.DefaultSqlSession;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.ifinalframework.context.user.UserContextHolder;
import org.ifinalframework.data.mybatis.mapper.AbsMapper;
import org.ifinalframework.data.query.DefaultQEntityFactory;
import org.ifinalframework.query.QEntity;

/**
 * 参数注入拦截器
 *
 * @author ilikly
 * @version 1.0.0
 * @since 1.0.0
 */
@Intercepts(
        {
                @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class,
                        RowBounds.class, ResultHandler.class}),
                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class,
                        RowBounds.class, ResultHandler.class, CacheKey.class,
                        BoundSql.class}),
        }
)
@Order
@Component
@SuppressWarnings({"unchecked", "rawtypes"})
public class ParameterInjectionInterceptor extends AbsMapperInterceptor {

    private static final String TABLE_PARAMETER_NAME = "table";

    private static final String PROPERTIES_PARAMETER_NAME = "properties";


    @Override
    protected Object intercept(Invocation invocation, Class<?> mapper, Class<?> entityClass) throws Throwable {

        Object[] args = invocation.getArgs();

        Object parameter = args[1];

        if (parameter instanceof Map && AbsMapper.class.isAssignableFrom(mapper)) {
            Map<String, Object> parameters = (Map<String, Object>) parameter;

            final QEntity<?, ?> entity = DefaultQEntityFactory.INSTANCE.create(entityClass);
            parameters.computeIfAbsent(TABLE_PARAMETER_NAME, k -> entity.getTable());
            parameters.putIfAbsent(PROPERTIES_PARAMETER_NAME, entity);
            parameters.putIfAbsent("USER", UserContextHolder.getUser());

            /**
             * {@link org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator} only supports {@link Map} types:
             * <ul>
             *     <li>{@link org.apache.ibatis.binding.MapperMethod.ParamMap}</li>
             *     <li>{@link DefaultSqlSession.StrictMap}</li>
             * </ul>
             * @see org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator#assignKeys(Configuration, ResultSet, ResultSetMetaData, String[], Object)
             * @since 1.2.2
             */
            final MapperMethod.ParamMap<Object> paramMap = new MapperMethod.ParamMap<>();
            paramMap.putAll((Map<? extends String, ?>) parameter);
            args[1] = paramMap;

        }

        return invocation.proceed();


    }


}
