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

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

import org.ifinalframework.auto.service.annotation.AutoProcessor;
import org.ifinalframework.data.auto.annotation.AutoRestController;
import org.ifinalframework.data.auto.generator.RestControllerJavaFileGenerator;

import lombok.extern.slf4j.Slf4j;

/**
 * AutoMapperGeneratorProcessor.
 *
 * @author ilikly
 * @version 1.4.1
 * @since 1.4.1
 */
@Slf4j
@AutoProcessor
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
public class AutoRestControllerGeneratorProcessor extends AbsAutoGeneratorProcessor<AutoRestController> {

    public AutoRestControllerGeneratorProcessor() {
        super(new RestControllerJavaFileGenerator());
    }

}
