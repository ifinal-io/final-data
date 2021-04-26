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

package org.ifinal.finalframework.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.lang.NonNull;

import org.ifinal.finalframework.aop.AnnotationAttributesAnnotationBuilder;
import org.ifinal.finalframework.aop.multi.MultiAnnotationPointAdvisor;
import org.ifinal.finalframework.cache.annotation.Cache;
import org.ifinal.finalframework.cache.annotation.CacheDel;
import org.ifinal.finalframework.cache.annotation.CacheIncrement;
import org.ifinal.finalframework.cache.annotation.CacheLock;
import org.ifinal.finalframework.cache.annotation.CachePut;
import org.ifinal.finalframework.cache.annotation.CacheValue;
import org.ifinal.finalframework.cache.annotation.Cacheable;
import org.ifinal.finalframework.cache.handler.CacheDelInterceptorHandler;
import org.ifinal.finalframework.cache.handler.CacheIncrementInterceptorHandler;
import org.ifinal.finalframework.cache.handler.CacheLockInterceptorHandler;
import org.ifinal.finalframework.cache.handler.CachePutInterceptorHandler;
import org.ifinal.finalframework.cache.handler.CacheValueInterceptorHandler;
import org.ifinal.finalframework.cache.handler.CacheableInterceptorHandler;

import javax.annotation.Resource;

/**
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "final.cache", name = "enable", havingValue = "true", matchIfMissing = true)
public class CacheAnnotationPointAdvisor extends MultiAnnotationPointAdvisor<AnnotationAttributes, Cache> {

    @Resource
    private RedisCache redisCache;

    public CacheAnnotationPointAdvisor() {

        this.addAnnotation(CacheLock.class, new AnnotationAttributesAnnotationBuilder<>(),
            new CacheLockInterceptorHandler());
        this.addAnnotation(Cacheable.class, new AnnotationAttributesAnnotationBuilder<>(),
            new CacheableInterceptorHandler());
        this.addAnnotation(CachePut.class, new AnnotationAttributesAnnotationBuilder<>(),
            new CachePutInterceptorHandler());
        this.addAnnotation(CacheDel.class, new AnnotationAttributesAnnotationBuilder<>(),
            new CacheDelInterceptorHandler());
        this.addAnnotation(CacheIncrement.class, new AnnotationAttributesAnnotationBuilder<>(),
            new CacheIncrementInterceptorHandler());
        this.addAnnotation(CacheValue.class, new AnnotationAttributesAnnotationBuilder<>(),
            new CacheValueInterceptorHandler());

    }

    @Override
    @NonNull
    protected Cache getExecutor(final AnnotationAttributes annotation) {
        return redisCache;
    }

}
