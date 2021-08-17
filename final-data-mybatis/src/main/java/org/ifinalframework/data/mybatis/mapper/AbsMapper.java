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

package org.ifinalframework.data.mybatis.mapper;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.builder.annotation.ProviderContext;
import org.ifinalframework.core.IEntity;
import org.ifinalframework.data.mybatis.sql.provider.*;
import org.ifinalframework.data.repository.Repository;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author likly
 * @version 1.0.0
 * @see InsertSqlProvider
 * @see UpdateSqlProvider
 * @see SelectSqlProvider
 * @see SelectIdsSqlProvider
 * @see SelectCountSqlProvider
 * @see DeleteSqlProvider
 * @since 1.0.0
 */
@SuppressWarnings("all")
public interface AbsMapper<I extends Serializable, T extends IEntity<I>> extends Repository<I, T> {

    /**
     * Use {@link Options#useGeneratedKeys()} to get the auto increment key.
     *
     * @param table    表名
     * @param view     视图,
     * @param ignore   是否忽略重复数据,{@literal INSERT IGNORE}
     * @param entities 实体集
     * @see InsertSqlProvider#insert(ProviderContext, Map)
     */
    @Override
    @Options(useGeneratedKeys = true, keyProperty = "list.id", keyColumn = "id")
    @InsertProvider(InsertSqlProvider.class)
    int insert(Map<String, Object> params);


    /**
     * @param table    表名
     * @param view     视图
     * @param entities 实体集
     * @see InsertSqlProvider#replace(ProviderContext, Map)
     */
    @Override
    @Options(useGeneratedKeys = true, keyProperty = "list.id", keyColumn = "id")
    @InsertProvider(InsertSqlProvider.class)
    int replace(Map<String, Object> params);

    /**
     * @param table    表名
     * @param view     视图
     * @param entities 实体集
     * @see InsertSqlProvider#save(ProviderContext, Map)
     */
    @Override
    @Options(useGeneratedKeys = true, keyProperty = "list.id", keyColumn = "id")
    @InsertProvider(InsertSqlProvider.class)
    int save(Map<String, Object> params);

    @Override
    @UpdateProvider(UpdateSqlProvider.class)
    int update(Map<String, Object> params);

    @Override
    @DeleteProvider(DeleteSqlProvider.class)
    int delete(Map<String, Object> params);

    @Override
    @SelectProvider(SelectSqlProvider.class)
    List<T> select(Map<String, Object> params);

    @Override
    @SelectProvider(SelectSqlProvider.class)
    T selectOne(Map<String, Object> params);

    @Override
    @SelectProvider(SelectIdsSqlProvider.class)
    List<I> selectIds(Map<String, Object> params);

    @Override
    @SelectProvider(SelectCountSqlProvider.class)
    long selectCount(Map<String, Object> params);

    @Override
    @UpdateProvider(TruncateSqlProvider.class)
    void truncate(Map<String, Object> params);

}
