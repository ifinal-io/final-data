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

package org.ifinalframework.data.auto.generator;

import lombok.experimental.UtilityClass;

/**
 * AutoNameHelper.
 *
 * @author ilikly
 * @version 1.4.1
 * @since 1.4.1
 */
@UtilityClass
public class AutoNameHelper {
    public static String mapperPackage(Class<?> entity) {
        return entity.getPackage().getName().replace(".entity", ".dao.mapper");
    }

    public static String mapperName(Class<?> entity) {
        return entity.getSimpleName() + "Mapper";
    }

    public static String queryEntityPackage(Class<?> entity) {
        return entity.getPackage().getName().replace(".entity", ".dao.query");
    }

    public static String queryEntityName(Class<?> entity) {
        return "Q" + entity.getSimpleName();
    }

    public static String queryPackage(Class<?> entity) {
        return entity.getPackage().getName().replace(".entity", ".query");
    }

    public static String queryName(Class<?> entity) {
        return entity.getSimpleName() + "Query";
    }

    public static String servicePackage(Class<?> entity) {
        return entity.getPackage().getName().replace(".entity", ".service");
    }

    public static String serviceName(Class<?> entity) {
        return entity.getSimpleName() + "Service";
    }

    public static String dtoClassName(Class<?> entity, String action) {
        final String dtoPackageName = entity.getPackage().getName().replace(".entity", ".domain.dto");
        final String dtoName = action + entity.getSimpleName();
        return String.join(".", dtoPackageName, dtoName);
    }

    public static String controllerPackage(Class<?> entity) {
        return entity.getPackage().getName().replace(".entity", ".web.controller");
    }

    public static String controllerName(Class<?> entity) {
        return entity.getSimpleName() + "RestController";
    }
}


