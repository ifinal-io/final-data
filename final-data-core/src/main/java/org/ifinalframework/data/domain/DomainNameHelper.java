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

package org.ifinalframework.data.domain;

import lombok.experimental.UtilityClass;

/**
 * AutoNameHelper.
 *
 * <pre class="code">
 * **.***.entity.scope
 *       .repository.scope.query
 *       .repository.scope.mapper
 *       .domain.scope.query
 *       .domain.scope.service
 *       .domain.scope.entity
 *       .domain.scope.spi
 * </pre>
 *
 * @author ilikly
 * @version 1.4.1
 * @since 1.4.1
 */
@UtilityClass
public class DomainNameHelper {
    public static String mapperPackage(Class<?> entity) {
        return entity.getPackage().getName().replace(".entity", ".repository") + ".mapper";
    }

    public static String mapperName(Class<?> entity) {
        return entity.getSimpleName() + "Mapper";
    }

    public static String queryEntityPackage(Class<?> entity) {
        return entity.getPackage().getName().replace(".entity", ".repository") + ".query";
    }

    public static String queryEntityName(Class<?> entity) {
        return "Q" + entity.getSimpleName();
    }

    public static String domainQueryPackage(Class<?> entity) {
        return entity.getPackage().getName().replace(".entity", ".domain") + ".query";
    }

    public static String domainQueryName(Class<?> entity) {
        return domainQueryName(entity, null);
    }

    public static String domainQueryName(Class<?> entity, Class<?> action) {
        return action == null ? entity.getSimpleName() + "Query" : entity.getSimpleName() + action.getSimpleName() + "Query";
    }

    public static String servicePackage(Class<?> entity) {
        return entity.getPackage().getName().replace(".entity", ".domain") + ".service";
    }

    public static String serviceName(Class<?> entity) {
        return entity.getSimpleName() + "Service";
    }

    public static String modelClassName(Class<?> entity, String action) {
        final String dtoPackageName = entity.getPackage().getName().replace(".entity", ".domain") + ".model";
        final String dtoName = action + entity.getSimpleName();
        return String.join(".", dtoPackageName, dtoName);
    }

}


