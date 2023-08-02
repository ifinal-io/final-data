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

package org.ifinalframework.data.domain.action;

import org.ifinalframework.context.exception.NotFoundException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * DefaultDomainActionRegistry
 *
 * @author mik
 * @since 1.5.2
 **/
@Component
public class DefaultDomainActionRegistry implements DomainActionRegistry {

    private final Map<String, DomainActions> resourceDomainActionMap = new LinkedHashMap<>(128);

    @Override
    public DomainActions get(String resource) {
        final DomainActions domainActions = resourceDomainActionMap.get(resource);
        if (Objects.isNull(domainActions)) {
            throw new NotFoundException("Not found domainAction for resource of " + resource);
        }
        return domainActions;
    }

    @Override
    public <T extends DomainAction> T get(String resource, DomainActions.ActionType actionType) {

        final DomainActions domainActions = get(resource);

        final DomainAction domainAction = domainActions.getDomainActions().get(actionType);

        if (Objects.isNull(domainAction)) {
            throw new NotFoundException("Not found domainAction for resource of " + resource + " and type of " + actionType);
        }

        return (T) domainAction;
    }


    @Override
    public void registry(String resource, DomainActions domainActions) {
        resourceDomainActionMap.put(resource, domainActions);
    }
}
