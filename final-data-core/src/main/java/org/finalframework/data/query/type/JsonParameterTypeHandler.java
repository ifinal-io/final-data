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

package org.finalframework.data.query.type;

import org.finalframework.json.Json;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.ibatis.type.JdbcType;

/**
 * Converter the parameter to a {@code json} String.
 *
 * @author likly
 * @version 1.0.0
 * @see org.ifinal.finalframework.data.annotation.Json
 * @see java.util.Collection
 * @see java.util.Map
 * @see java.util.List
 * @see java.util.Set
 * @since 1.0.0
 */
public class JsonParameterTypeHandler extends ParameterTypeHandler<Object> {

    @Override
    public void setNonNullParameter(final PreparedStatement ps, final int i, final Object parameter,
        final JdbcType jdbcType) throws SQLException {

        ps.setString(i, Json.toJson(parameter));
    }

}

