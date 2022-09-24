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
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;

import org.ifinalframework.data.auto.annotation.AutoMapper;

import com.squareup.javapoet.JavaFile;
import lombok.SneakyThrows;

/**
 * AutoMapperGenerator.
 *
 * <pre class="code">
 *      package xxx.dao.mapper;
 *
 *      &#64;Mapper
 *      public interface XxxMapper extends AbsMapper&lt;Long,XxxEntity&gt;{
 *
 *      }
 * </pre>
 *
 * @author ilikly
 * @version 1.0.0
 * @since 1.0.0
 */
public class AutoMapperGenerator implements AutoGenerator<AutoMapper, TypeElement> {

    private final AutoMapperJavaFileGenerator autoMapperJavaFileGenerator = new AutoMapperJavaFileGenerator();

    private final ProcessingEnvironment processingEnv;

    public AutoMapperGenerator(final ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    @Override
    @SneakyThrows
    public void generate(final AutoMapper ann, final TypeElement entity) {

        Class<?> clazz = Class.forName(entity.getQualifiedName().toString());

        String mapperName = String.join(".", AutoNameHelper.mapperPackage(clazz), AutoNameHelper.mapperName(clazz));


        try {
            final TypeElement mapperElement = processingEnv.getElementUtils().getTypeElement(mapperName);

            if (mapperElement == null) {
                final JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(mapperName);

                JavaFile javaFile = autoMapperJavaFileGenerator.generate(ann, clazz);
                try (Writer writer = sourceFile.openWriter()) {
                    javaFile.writeTo(writer);
                    writer.flush();
                }

            }

        } catch (IOException e) {
            error(e.getMessage());
        }
    }

    private void error(final String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
    }

}
