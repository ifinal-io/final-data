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

package org.ifinalframework.data.spi.composite;

import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.spi.PreUpdateYnValidator;

/**
 * PreUpdateYnValidatorComposite.
 *
 * @author ilikly
 * @version 1.4.2
 * @since 1.4.2
 */
public class PreUpdateYnValidatorComposite<T, U> implements PreUpdateYnValidator<T, U> {
    private final List<PreUpdateYnValidator<T, U>> validators;

    public PreUpdateYnValidatorComposite(List<PreUpdateYnValidator<T, U>> validators) {
        this.validators = validators;
    }

    @Override
    public void validate(@NonNull T entity, @NonNull YN yn, @NonNull U user) {
        if (CollectionUtils.isEmpty(validators)) {
            return;
        }

        for (PreUpdateYnValidator<T, U> validator : validators) {
            validator.validate(entity, yn, user);
        }

    }
}


