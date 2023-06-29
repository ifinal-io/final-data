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

package org.ifinalframework.data.mybatis.javassist;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.loader.ResultLoaderMap;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetWrapper;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.reflection.MetaObject;
import org.ifinalframework.auto.service.annotation.AutoService;
import org.ifinalframework.java.JvmDriver;
import org.ifinalframework.javassist.JavaAssistProcessor;

import java.util.Objects;

/**
 * DefaultResultSetHandlerJavaAssistProcessor.
 *
 * @author ilikly
 * @version 1.5.1
 * @see DefaultResultSetHandler
 * @since 1.5.1
 */
@Slf4j
@AutoService(JavaAssistProcessor.class)
public class DefaultResultSetHandlerJavaAssistProcessor implements JavaAssistProcessor {
    @Override
    public void process(ClassPool classPool) throws Throwable {
        logger.debug("start modify class: DefaultResultSetHandler");
        final CtClass ctClass = classPool.get("org.apache.ibatis.executor.resultset.DefaultResultSetHandler");
        modifyMethodApplyPropertyMappings(ctClass);
        final Class<?> aClass = ctClass.toClass();
        logger.debug("finish modify class: DefaultResultSetHandler");
    }

    /**
     * @param ctClass
     * @throws Throwable
     * @see DefaultResultSetHandler#applyPropertyMappings(ResultSetWrapper, ResultMap, MetaObject, ResultLoaderMap, String) 
     */
    private void modifyMethodApplyPropertyMappings(CtClass ctClass) throws Throwable {
        final CtMethod method = ctClass.getDeclaredMethod("applyPropertyMappings");
        method.insertBefore("System.out.println($2.getId());\n");

        method.setBody(
                //                "    private boolean applyPropertyMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject,\n" +
                //                "                                          ResultLoaderMap lazyLoader, String columnPrefix) throws SQLException {\n" +
                "{       " +
                        "        final java.util.List mappedColumnNames = $1.getMappedColumnNames($2, $5);\n" +
                        "        boolean foundValues = false;\n" +
                        "        final java.util.List propertyMappings = $2.getPropertyResultMappings();\n" +
                        "        for (int i = 0; i < propertyMappings.size(); i++) {\n" +
                        "            org.apache.ibatis.mapping.ResultMapping propertyMapping =  (org.apache.ibatis.mapping.ResultMapping)propertyMappings.get(i);\n" +
                        "            String column = prependPrefix(propertyMapping.getColumn(), $5);\n" +
                        "            if (propertyMapping.getNestedResultMapId() != null) {\n" +
                        "                // the user added a column attribute to a nested result map, ignore it\n" +
                        "                column = null;\n" +
                        "            }\n" +
                        "            if (propertyMapping.isCompositeResult()\n" +
                        "                    || column != null && mappedColumnNames.contains(column.toUpperCase(java.util.Locale.ENGLISH))\n" +
                        "                    || propertyMapping.getResultSet() != null) {\n" +
                        "                Object value = null;\n" +
                        "                if (propertyMapping.isCompositeResult()) {\n" +
                        "                    final java.util.List resultMappings = propertyMapping.getComposites();\n" +
                        "                    final org.apache.ibatis.mapping.ResultMap resultMap2 = new org.apache.ibatis.mapping.ResultMap$Builder(\n" +
                        "                            this.configuration, propertyMapping.getJavaType().getSimpleName(),\n" +
                        "                            propertyMapping.getJavaType(), resultMappings, java.lang.Boolean.FALSE\n" +
                        "                    ).build();\n" +
                        "                    value = getRowValue($1, resultMap2, propertyMapping.getColumnPrefix());\n" +
                        "                } else {\n" +
                        "                    value = getPropertyMappingValue($1.getResultSet(), $3, propertyMapping, $4, $5);\n" +
                        "                }\n" +
                        "                // issue #541 make property optional\n" +
                        "                final String property = propertyMapping.getProperty();\n" +
                        "                if (property == null) {\n" +
                        "                    continue;\n" +
                        "                }\n" +
                        "                if (value == DEFERRED) {\n" +
                        "                    foundValues = true;\n" +
                        "                    continue;\n" +
                        "                }\n" +
                        "                if (value != null) {\n" +
                        "                    foundValues = true;\n" +
                        "                }\n" +
                        "                if (value != null\n" +
                        "                        || $0.configuration.isCallSettersOnNulls() && !$3.getSetterType(property).isPrimitive()) {\n" +
                        "                    // gcode issue #377, call setter on nulls (value is not 'found')\n" +
                        "                    $3.setValue(property, value);\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }\n" +
                        "        return foundValues;" +
                        "    }");

    }
}
