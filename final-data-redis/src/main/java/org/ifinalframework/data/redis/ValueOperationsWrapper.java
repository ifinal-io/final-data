/*
 * Copyright 2020-2021 the original author or authors.
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

package org.ifinalframework.data.redis;

import org.springframework.data.redis.core.ValueOperations;

import org.ifinalframework.json.Json;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * ValueOperations.
 *
 * @author likly
 * @version 1.2.0
 * @see ValueOperations
 * @since 1.2.0
 */
public final class ValueOperationsWrapper {

    private final ValueOperations<Object, Object> operations;

    public ValueOperationsWrapper(final ValueOperations<Object, Object> operations) {
        this.operations = Objects.requireNonNull(operations);
    }

    /**
     * @see ValueOperations#set(Object, Object)
     */
    public void set(Object key, Object value) {
        operations.set(key, value);
    }

    public void set(Object key, Object value, long ttl, TimeUnit unit) {
        operations.set(key, value, ttl, unit);
    }

    public void set(Object key, Object value, Duration ttl) {
        operations.set(key, value, ttl);
    }

    public Boolean setIfAbsent(Object key, Object value) {
        return operations.setIfAbsent(key, value);
    }

    public Boolean setIfAbsent(Object key, Object value, long ttl, TimeUnit unit) {
        return operations.setIfAbsent(key, value, ttl, unit);
    }

    public Boolean setIfAbsent(Object key, Object value, Duration ttl) {
        return operations.setIfAbsent(key, value, ttl);
    }

    public Boolean setIfPresent(Object key, Object value) {
        return operations.setIfPresent(key, value);
    }

    public Boolean setIfPresent(Object key, Object value, long ttl, TimeUnit unit) {
        return operations.setIfPresent(key, value, ttl, unit);
    }

    public Boolean setIfPresent(Object key, Object value, Duration ttl) {
        return operations.setIfPresent(key, value, ttl);
    }

    public void multiSet(Map<Object, Object> map) {
        operations.multiSet(map);
    }

    public void multiSetIfAbsent(Map<Object, Object> map) {
        operations.multiSetIfAbsent(map);
    }

    public String get(Object key) {
        return (String) operations.get(key);
    }

    public <T> T get(Object key, Class<T> type) {
        return Optional.ofNullable(key)
            .map(value -> Json.toObject((String) value, type))
            .orElse(null);
    }

    public <T> T getAndSet(Object key, T value, Class<T> type) {
        return Optional.ofNullable(operations.getAndSet(key, value))
            .map(v -> Json.toObject((String) v, type))
            .orElse(null);
    }

    public void set(Object key, Object value, long offset) {
        operations.set(key, value, offset);
    }

}
