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

package org.finalframework.data.auto.processor;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.lang.Nullable;

import org.finalframework.core.IEntity;
import org.finalframework.core.lang.Transient;
import org.finalframework.data.auto.generator.AutoGenerator;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

/**
 * AutoMapperGeneratorProcessor.
 *
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
public abstract class AbsAutoGeneratorProcessor<A extends Annotation, E extends Element> extends AbstractProcessor {

    private Set<Element> elements = new LinkedHashSet<>();

    private Set<Element> autoMappers = new LinkedHashSet<>();

    private static final String ENTITY = IEntity.class.getName();

    private static final String TRANSIENT = Transient.class.getName();

    public AbsAutoGeneratorProcessor(final Class<A> autoAnnotation) {
        this.autoAnnotation = autoAnnotation;
    }

    private final Class<A> autoAnnotation;

    private TypeElementFilter typeElementFilter;

    private TypeElement typeElement;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.typeElement = processingEnv.getElementUtils().getTypeElement(ENTITY);
        this.typeElementFilter = new TypeElementFilter(processingEnv, typeElement,
            processingEnv.getElementUtils().getTypeElement(TRANSIENT));
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {

        if (roundEnv.processingOver()) {
            ElementFilter.packagesIn(autoMappers)
                .forEach(it -> {

                    A autoMapper = it.getAnnotation(autoAnnotation);

                    String packageName = it.getQualifiedName().toString();

                    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
                        false);

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

                    ElementFilter.typesIn(elements)
                        .stream()
                        .filter(element -> element.getQualifiedName().toString().startsWith(packageName))
                        .forEach(element -> set.add(element.getQualifiedName().toString()));

                    set.stream()
                        .map(element -> processingEnv.getElementUtils().getTypeElement(element))
                        .forEachOrdered(element -> getAutoGenerator().generate(autoMapper, (E) element));

                });
        } else {
            ElementFilter.typesIn(roundEnv.getRootElements())
                .stream()
                .filter(typeElementFilter::matches)
                .forEach(elements::add);

            autoMappers.addAll(roundEnv.getElementsAnnotatedWith(autoAnnotation));
        }

        return false;
    }

    protected abstract AutoGenerator<A, E> getAutoGenerator();

}
