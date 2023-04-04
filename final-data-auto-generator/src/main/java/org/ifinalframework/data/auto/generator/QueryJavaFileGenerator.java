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

import javax.lang.model.element.Modifier;

import org.springframework.lang.NonNull;

import org.ifinalframework.core.PageQuery;
import org.ifinalframework.data.auto.annotation.AutoService;
import org.ifinalframework.data.domain.DomainNameHelper;
import org.ifinalframework.javapoets.JavaPoets;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import lombok.extern.slf4j.Slf4j;

/**
 * ServiceJavaFileGenerator.
 * <pre class="code">
 * public class EntityQuery extends PageQuery{
 * }
 * </pre>
 *
 * @author ilikly
 * @version 1.4.1
 * @since 1.4.1
 */
@Slf4j
public class QueryJavaFileGenerator implements JavaFileGenerator<AutoService> {
    @Override
    public String getName(@NonNull AutoService ann, @NonNull Class<?> clazz) {
        return String.join(".", DomainNameHelper.domainQueryPackage(clazz), DomainNameHelper.domainQueryName(clazz));
    }

    @NonNull
    @Override
    public JavaFile generate(@NonNull AutoService ann, @NonNull Class<?> clazz) {

        String queryPackage = DomainNameHelper.domainQueryPackage(clazz);
        String queryName = DomainNameHelper.domainQueryName(clazz);

        logger.info("start generate query for entity of {}.{}", queryPackage, queryName);

        try {

            // public class EntityQuery extends PageQuery
            TypeSpec service = TypeSpec.classBuilder(queryName)
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(PageQuery.class)
                    .addAnnotation(JavaPoets.generated(getClass()))
                    .addJavadoc(JavaPoets.Javadoc.author())
                    .addJavadoc(JavaPoets.Javadoc.version())
                    .build();

            return JavaFile.builder(queryPackage, service)
                    .skipJavaLangImports(true)
                    .indent("    ")
                    .build();
        } finally {
            logger.info("generated query: {}.{}", queryPackage, queryName);
        }
    }
}


