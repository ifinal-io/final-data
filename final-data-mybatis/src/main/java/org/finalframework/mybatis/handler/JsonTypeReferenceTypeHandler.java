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

package org.finalframework.mybatis.handler;

import org.finalframework.json.Json;

import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.JdbcType;

/**
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
public class JsonTypeReferenceTypeHandler<T> extends BaseTypeReferenceTypeHandler<T> {

    public JsonTypeReferenceTypeHandler(final Type type) {

        super(type);
    }

    @Override
    public void setNonNullParameter(final PreparedStatement ps, final int i, final T parameter, final JdbcType jdbcType)
        throws SQLException {

        ps.setString(i, Json.toJson(parameter));
    }

    @Override
    public T getNullableResult(final ResultSet rs, final String columnName) throws SQLException {

        return toObject(rs.getString(columnName));
    }

    @Override
    public T getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {

        return toObject(rs.getString(columnIndex));
    }

    @Override
    public T getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {

        return toObject(cs.getString(columnIndex));
    }

    private T toObject(final String json) {

        if (json == null) {
            return null;
        }
        return Json.toObject(json, getType());
    }

}
