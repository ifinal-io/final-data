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

package org.ifinalframework.data.auto.generator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.Objects;

import org.ifinalframework.data.auto.annotation.AutoService;

import com.squareup.javapoet.JavaFile;
import lombok.SneakyThrows;

/**
 * AutoServiceGenerator.
 *
 * @author ilikly
 * @version 1.0.0
 * @since 1.0.0
 */
public class AutoServiceGenerator implements AutoGenerator<AutoService, TypeElement> {

    private final QueryJavaFileGenerator queryJavaFileGenerator = new QueryJavaFileGenerator();
    private final ServiceJavaFileGenerator serviceJavaFileGenerator = new ServiceJavaFileGenerator();
    private final ServiceImplJavaFileGenerator serviceImplJavaFileGenerator = new ServiceImplJavaFileGenerator();

    private final ProcessingEnvironment processingEnv;

    public AutoServiceGenerator(final ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    @Override
    @SneakyThrows
    public void generate(final AutoService ann, final TypeElement entity) {

        final Class<?> clazz = Class.forName(entity.getQualifiedName().toString());

        final String queryName = String.join(".", AutoNameHelper.queryPackage(clazz), AutoNameHelper.queryName(clazz));
        final String serviceName = String.join(".", AutoNameHelper.servicePackage(clazz), AutoNameHelper.serviceName(clazz));
        final String serviceImplName = String.join(".", AutoNameHelper.servicePackage(clazz) + ".impl", AutoNameHelper.serviceName(clazz) + "Impl");

        doGenerate(ann, clazz, queryName, queryJavaFileGenerator);
        doGenerate(ann, clazz, serviceName, serviceJavaFileGenerator);
        doGenerate(ann, clazz, serviceImplName, serviceImplJavaFileGenerator);


    }

    @SneakyThrows
    private void doGenerate(final AutoService ann, Class<?> clazz, String elementName, JavaFileGenerator<AutoService> javaFileGenerator) {
        final TypeElement element = processingEnv.getElementUtils().getTypeElement(elementName);
        if (Objects.isNull(element)) {
            final JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(elementName);
            try (Writer writer = sourceFile.openWriter()) {
                JavaFile javaFile = javaFileGenerator.generate(ann, clazz);
                javaFile.writeTo(writer);
                writer.flush();
            }
        }
    }


}
