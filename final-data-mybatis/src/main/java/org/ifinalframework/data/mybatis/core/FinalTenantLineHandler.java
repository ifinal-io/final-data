/*
 * Copyright 2020-2023 the original author or authors.
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

package org.ifinalframework.data.mybatis.core;

import org.ifinalframework.data.core.TenantSupplier;
import org.ifinalframework.data.core.TenantTableService;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;

/**
 * FinalTenantLineHandler.
 *
 * @author ilikly
 * @version 1.4.3
 * @since 1.4.3
 */
@Slf4j
@RequiredArgsConstructor
public class FinalTenantLineHandler implements TenantLineHandler {
    private final String tenantColumn;
    private final TenantSupplier<?> tenantSupplier;

    private final TenantTableService tenantTableService;

    @Override
    public Expression getTenantId() {
        Object tenant = tenantSupplier.get();
        if (tenant instanceof Expression) {
            return (Expression) tenant;
        }
        throw new IllegalArgumentException("tenant not instanceof Expression:" + tenant);
    }

    @Override
    public String getTenantIdColumn() {
        return tenantColumn;
    }

    @Override
    public boolean ignoreTable(String tableName) {
        boolean isTenantTable = !tenantTableService.isTenantTable(tableName);
        if (logger.isDebugEnabled()) {
            logger.info("{}:isTenantTable:{}", tableName, isTenantTable);
        }
        return isTenantTable;
    }
}
