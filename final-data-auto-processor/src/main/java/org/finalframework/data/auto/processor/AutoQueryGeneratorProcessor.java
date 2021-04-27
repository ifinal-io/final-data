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

import org.finalframework.data.auto.annotation.AutoQuery;
import org.finalframework.data.auto.generator.AutoGenerator;
import org.finalframework.data.auto.generator.AutoQueryGenerator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

import org.ifinal.auto.service.annotation.AutoProcessor;

/**
 * AutoMapperGeneratorProcessor.
 *
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
@AutoProcessor
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
public class AutoQueryGeneratorProcessor extends AbsAutoGeneratorProcessor<AutoQuery, TypeElement> {

    private AutoQueryGenerator autoQueryGenerator;

    public AutoQueryGeneratorProcessor() {
        super(AutoQuery.class);
    }

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.autoQueryGenerator = new AutoQueryGenerator(processingEnv);
    }

    @Override
    protected AutoGenerator<AutoQuery, TypeElement> getAutoGenerator() {
        return autoQueryGenerator;
    }

}
