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

package org.ifinal.finalframework.auto.mybatis.processor;

import org.ifinal.auto.service.annotation.AutoProcessor;
import org.ifinal.auto.service.processor.AbsServiceProcessor;

import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
@AutoProcessor
@SuppressWarnings("unused")
@SupportedAnnotationTypes({
    "org.apache.ibatis.annotations.Mapper"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AutoMapperProcessor extends AbsServiceProcessor {

    private TypeElement mapperElement;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {

        super.init(processingEnv);
        this.mapperElement = processingEnv.getElementUtils().getTypeElement(Mapper.class.getCanonicalName());
    }

    @Override
    protected boolean doProcess(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {

        roundEnv.getElementsAnnotatedWith(Mapper.class)
            .forEach(mapper -> addService(mapperElement, (TypeElement) mapper));

        return false;
    }

}

