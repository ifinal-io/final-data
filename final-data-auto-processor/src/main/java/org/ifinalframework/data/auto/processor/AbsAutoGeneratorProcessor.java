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

package org.ifinalframework.data.auto.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.lang.Transient;
import org.ifinalframework.data.auto.generator.JavaFileGenerator;

import com.squareup.javapoet.JavaFile;
import lombok.extern.slf4j.Slf4j;

/**
 * AutoMapperGeneratorProcessor.
 *
 * @author ilikly
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public abstract class AbsAutoGeneratorProcessor<A extends Annotation> extends AbstractProcessor {

    private final Class<A> autoAnnotation;
    private final List<JavaFileGenerator<A>> javaFileGenerators;

    @SuppressWarnings("unchecked")
    public AbsAutoGeneratorProcessor(JavaFileGenerator<A>... javaFileGenerators) {
        this.autoAnnotation = (Class<A>) ResolvableType.forClass(this.getClass()).as(AbsAutoGeneratorProcessor.class).getGeneric().resolve();
        this.javaFileGenerators = Arrays.asList(javaFileGenerators);
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {

//        if (roundEnv.processingOver()) {
            logger.info("try to found packages with annotation of {}", autoAnnotation);
            Set<PackageElement> packageElements = ElementFilter.packagesIn(roundEnv.getElementsAnnotatedWith(autoAnnotation));
            logger.info("found packages: {}",packageElements);
            packageElements
                    .forEach(it -> {

                        A ann = it.getAnnotation(autoAnnotation);

                        String packageName = it.getQualifiedName().toString();

                        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

                        scanner.setResourceLoader(new PathMatchingResourcePatternResolver() {
                            @Nullable
                            @Override
                            public ClassLoader getClassLoader() {
                                return AbsAutoGeneratorProcessor.this.getClass().getClassLoader();
                            }
                        });
                        scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> {
                            try {
                                Class<?> clazz = Class.forName(metadataReader.getClassMetadata().getClassName());
                                return IEntity.class.isAssignableFrom(clazz) && !clazz.isAnnotationPresent(Transient.class);
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                            return false;
                        });

                        Set<BeanDefinition> components = scanner.findCandidateComponents(packageName);

                        Set<String> set = components.stream().map(BeanDefinition::getBeanClassName)
                                .collect(Collectors.toSet());


                        set.stream()
                                .map(element -> {
                                    try {
                                        return ClassUtils.forName(element, getClass().getClassLoader());
                                    } catch (ClassNotFoundException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .forEachOrdered(element -> doGenerate(ann, element));

                    });
//        }

        return false;
    }

    private void doGenerate(A ann, Class<?> clazz) {
        javaFileGenerators.forEach(generator -> {
                    try {
                        String name = generator.getName(ann, clazz);
                        final TypeElement mapperElement = processingEnv.getElementUtils().getTypeElement(name);

                        if (mapperElement == null) {
                            final JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(name);

                            JavaFile javaFile = generator.generate(ann, clazz);
                            try (Writer writer = sourceFile.openWriter()) {
                                javaFile.writeTo(writer);
                                writer.flush();
                            }

                        }

                    } catch (IOException e) {
                        error(e.getMessage());
                    }
                }
        );
    }

    private void error(final String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
    }

}
