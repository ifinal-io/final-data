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

package org.ifinalframework.data.security;

import org.ifinalframework.core.IEnum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ResourceSecurity.
 *
 * @author ilikly
 * @version 1.4.2
 * @since 1.4.2
 */
@Getter
@RequiredArgsConstructor
public enum ResourceSecurity implements IEnum<String> {
    INSERT("%s:insert", "新建"),
    UPDATE("%s:update", "更新"),
    DELETE("%s:delete", "删除"),
    QUERY("%s:query", "查询"),
    ;
    private final String code;
    private final String desc;

    public String format(String resource) {
        return String.format(code, resource);
    }


}
