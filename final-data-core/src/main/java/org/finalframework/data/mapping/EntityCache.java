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

package org.finalframework.data.mapping;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
final class EntityCache {

    private static final EntityCache ourInstance = new EntityCache();

    private static final Map<Class<?>, Entity<?>> cache = new ConcurrentHashMap<>(1024);

    private EntityCache() {
    }

    public static EntityCache getInstance() {
        return ourInstance;
    }

    @SuppressWarnings("unchecked")
    public <T> Entity<T> get(final Class<T> entity) {

        if (!cache.containsKey(entity)) {
            synchronized (EntityCache.class) {
                cache.put(entity, new AnnotationEntity<>(entity));
            }
        }

        return (Entity<T>) cache.get(entity);
    }

}
