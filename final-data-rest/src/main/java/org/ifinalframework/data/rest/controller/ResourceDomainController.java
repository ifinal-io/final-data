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

package org.ifinalframework.data.rest.controller;

import javax.annotation.Resource;
import javax.servlet.ServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.bind.support.WebRequestDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;

import org.ifinalframework.context.exception.BadRequestException;
import org.ifinalframework.context.exception.NotFoundException;
import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IEnum;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IStatus;
import org.ifinalframework.core.IUser;
import org.ifinalframework.core.IView;
import org.ifinalframework.data.annotation.DomainResource;
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.domain.DefaultDomainServiceFactory;
import org.ifinalframework.data.domain.DomainService;
import org.ifinalframework.data.domain.DomainServiceFactory;
import org.ifinalframework.data.rest.validation.NoValidationGroupsProvider;
import org.ifinalframework.data.rest.validation.ValidationGroupsProvider;
import org.ifinalframework.data.security.ResourceSecurity;
import org.ifinalframework.data.service.AbsService;
import org.ifinalframework.data.spi.PreResourceAuthorize;
import org.ifinalframework.data.spi.QueryConsumer;
import org.ifinalframework.data.spi.composite.QueryConsumerComposite;
import org.ifinalframework.json.Json;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ResourceDomainController.
 *
 * @author ilikly
 * @version 1.4.2
 * @since 1.4.2
 */
@Transactional
@RestController
@RequestMapping("/api/{resource}")
public class ResourceDomainController implements ApplicationContextAware, SmartInitializingSingleton {
    private static final Logger logger = LoggerFactory.getLogger(ResourceDomainController.class);

    @Resource
    private ValidationGroupsProvider validationGroupsProvider = new NoValidationGroupsProvider();

    private final Map<String, DomainService<Long, IEntity<Long>>> domainServiceMap = new LinkedHashMap<>();
    private PreResourceAuthorize<IUser<?>> preResourceAuthorize;

    private final QueryConsumerComposite queryConsumerComposite;

    public ResourceDomainController(ObjectProvider<QueryConsumer<?, ?>> queryConsumerProvider) {
        List consumers = queryConsumerProvider.orderedStream().collect(Collectors.toList());
        this.queryConsumerComposite = new QueryConsumerComposite(consumers);
    }

    @Setter
    private ApplicationContext applicationContext;

    private void applyPreResourceAuthorize(ResourceSecurity resourceSecurity, String resource, IUser<?> user) {
        if (Objects.nonNull(preResourceAuthorize)) {
            preResourceAuthorize.auth(resourceSecurity.format(resource), user);
        }
    }


    @GetMapping
    public List<? extends IEntity<Long>> query(@PathVariable String resource, NativeWebRequest request, WebDataBinderFactory binderFactory, IUser<?> user) throws Exception {
        logger.info("==> GET /api/{}", resource);
        applyPreResourceAuthorize(ResourceSecurity.QUERY, resource, user);
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        Class<? extends IQuery> queryClass = domainService.domainQueryClass(IView.List.class);
        Class<IEntity<Long>> entityClass = domainService.entityClass();
        IQuery query = bindQuery(request, binderFactory, entityClass, queryClass);
        return domainService.list(query, user);
    }

    @GetMapping("/detail")
    public IEntity<Long> detail(@PathVariable String resource, NativeWebRequest request, WebDataBinderFactory binderFactory, IUser<?> user) throws Exception {
        logger.info("==> GET /api/{}/detail", resource);
        applyPreResourceAuthorize(ResourceSecurity.QUERY, resource, user);
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        Class<? extends IQuery> queryClass = domainService.domainQueryClass(IView.Detail.class);
        Class<IEntity<Long>> entityClass = domainService.entityClass();
        IQuery query = bindQuery(request, binderFactory, entityClass, queryClass);
        return domainService.detail(query, user);
    }

    @GetMapping("/{id}")
    public IEntity<Long> query(@PathVariable String resource, @PathVariable Long id, IUser<?> user) {
        logger.info("==> GET /api/{}/{}", resource, id);
        applyPreResourceAuthorize(ResourceSecurity.QUERY, resource, user);
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        return domainService.detail(id, user);
    }

    // delete

    @DeleteMapping
    public Integer delete(@PathVariable String resource, @RequestBody String requestBody, IUser<?> user) {
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        Class<? extends IQuery> queryClass = domainService.domainQueryClass(IView.Delete.class);
        IQuery query = Json.toObject(requestBody, queryClass);
        return domainService.delete(query, user);
    }

    @DeleteMapping("/delete")
    public Integer deleteFromParam(@PathVariable String resource, @RequestParam Long id, IUser<?> user) {
        return this.delete(resource, id, user);
    }

    @DeleteMapping("/{id}")
    public Integer delete(@PathVariable String resource, @PathVariable Long id, IUser<?> user) {
        logger.info("==> GET /api/{}/{}", resource, id);
        applyPreResourceAuthorize(ResourceSecurity.DELETE, resource, user);
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        return domainService.delete(id, user);
    }


    @PostMapping
    public Object create(@PathVariable String resource, @RequestBody String requestBody, IUser<?> user, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        logger.info("==> POST /api/{}", resource);
        applyPreResourceAuthorize(ResourceSecurity.INSERT, resource, user);
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        Class<IEntity<Long>> entityClass = domainService.entityClass();

        Class<?> createEntityClass = domainService.domainEntityClass(IView.Create.class);
        if (Objects.nonNull(createEntityClass)) {
            Object createEntity = Json.toObject(requestBody, createEntityClass);
            WebDataBinder binder = binderFactory.createBinder(request, createEntity, "entity");
            Class<?>[] validationGroups = validationGroupsProvider.getEntityValidationGroups(entityClass);
            validate(entityClass, createEntity, binder, validationGroups);
            List<IEntity<Long>> entities = domainService.preInsertFunction().map(createEntity, user);
            return domainService.create(entities, user);
        } else if (requestBody.startsWith("{")) {
            IEntity<Long> entity = Json.toObject(requestBody, entityClass);
            WebDataBinder binder = binderFactory.createBinder(request, entity, "entity");
            Class<?>[] validationGroups = validationGroupsProvider.getEntityValidationGroups(entityClass);
            validate(entityClass, entity, binder, validationGroups);
            domainService.create(Collections.singletonList(entity), user);
            return entity.getId();
        } else if (requestBody.startsWith("[")) {
            List<IEntity<Long>> entities = Json.toList(requestBody, entityClass);
            WebDataBinder binder = binderFactory.createBinder(request, entities, "entities");
            Class<?>[] validationGroups = validationGroupsProvider.getEntityValidationGroups(entityClass);
            validate(entityClass, entities, binder, validationGroups);
            return domainService.create(entities, user);
        }

        throw new BadRequestException("unsupported requestBody format of " + requestBody);


    }

    @PutMapping
    public Integer update(@PathVariable String resource, @RequestBody String requestBody, IUser<?> user, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        return doUpdate(resource, null, requestBody, user, request, binderFactory);
    }

    @PutMapping("/{id}")
    public Integer update(@PathVariable String resource, @PathVariable Long id, @RequestBody String requestBody, IUser<?> user, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        return doUpdate(resource, id, requestBody, user, request, binderFactory);
    }

    private Integer doUpdate(String resource, Long id, String body, IUser<?> user, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        logger.info("==> PUT /api/{}", resource);
        applyPreResourceAuthorize(ResourceSecurity.UPDATE, resource, user);
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        Class<IEntity<Long>> entityClass = domainService.entityClass();
        IEntity<Long> entity = Json.toObject(body, entityClass);
        if (Objects.nonNull(id)) {
            entity.setId(id);
        } else {
            Assert.notNull(entity.getId(), "update id is null");
        }
        WebDataBinder binder = binderFactory.createBinder(request, entity, "entity");

        validate(entityClass, entity, binder);
        return domainService.update(entity, id, true, user);
    }

    @PatchMapping("/{id}/status")
    public Integer status(@PathVariable String resource, @PathVariable Long id, @RequestParam String status, IUser<?> user) {
        return doUpdateStatus(resource, id, status, user);
    }

    @PatchMapping("/status")
    public Integer status2(@PathVariable String resource, @RequestParam Long id, @RequestParam String status, IUser<?> user) {
        return doUpdateStatus(resource, id, status, user);
    }


    @SuppressWarnings("unchecked")
    private Integer doUpdateStatus(String resource, Long id, String status, IUser<?> user) {
        applyPreResourceAuthorize(ResourceSecurity.UPDATE, resource, user);

        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        Class<IEntity<Long>> entityClass = domainService.entityClass();

        if (!IStatus.class.isAssignableFrom(entityClass)) {
            throw new BadRequestException("resource is not supports status");
        }

        Class<?> statusClass = ResolvableType.forClass(entityClass).as(IStatus.class).resolveGeneric();
        Object statusValue = Json.toObject(status, statusClass);

        if (Objects.isNull(statusValue)) {
            throw new BadRequestException("not status of " + status);
        }

        return domainService.status(id, (IEnum<?>) statusValue, user);

    }


    @PutMapping("/{id}/yn")
    public Integer update(@PathVariable String resource, @PathVariable Long id, @RequestParam YN yn, IUser<?> user) {
        logger.info("==> PUT /api/{}/{}/yn", resource, id);
        applyPreResourceAuthorize(ResourceSecurity.UPDATE, resource, user);
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        return domainService.yn(id, yn, user);
    }

    @PutMapping("/{id}/disable")
    public Integer disable(@PathVariable String resource, @PathVariable Long id, IUser<?> user) {
        return this.update(resource, id, YN.NO, user);
    }

    @PutMapping("/{id}/enable")
    public Integer enable(@PathVariable String resource, @PathVariable Long id, IUser<?> user) {
        return this.update(resource, id, YN.YES, user);
    }

    @PutMapping("/yn")
    public Integer yn(@PathVariable String resource, @RequestParam Long id, @RequestParam YN yn, IUser<?> user) {
        return this.update(resource, id, yn, user);
    }


    @GetMapping("/count")
    public Long count(@PathVariable String resource, IUser<?> user, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        logger.info("==> GET /api/{}", resource);
        applyPreResourceAuthorize(ResourceSecurity.QUERY, resource, user);
        DomainService<Long, IEntity<Long>> domainService = getDomainService(resource);
        Class<? extends IQuery> queryClass = domainService.domainQueryClass(IView.Count.class);
        Class<IEntity<Long>> entityClass = domainService.entityClass();
        IQuery query = bindQuery(request, binderFactory, entityClass, queryClass);
        return domainService.count(query, user);
    }

    @SuppressWarnings("unchecked")
    private IQuery bindQuery(NativeWebRequest request, WebDataBinderFactory binderFactory, Class<IEntity<Long>> entityClass, Class<? extends IQuery> queryClass) throws Exception {
        IQuery query = BeanUtils.instantiateClass(queryClass);
        WebDataBinder binder = binderFactory.createBinder(request, query, "query");
        if (binder instanceof WebRequestDataBinder) {
            ((WebRequestDataBinder) binder).bind(request);
        } else if (binder instanceof ExtendedServletRequestDataBinder && request.getNativeRequest() instanceof ServletRequest) {
            ((ExtendedServletRequestDataBinder) binder).bind((ServletRequest) request.getNativeRequest());
        }
        Class<?>[] validationGroups = validationGroupsProvider.getQueryValidationGroups(entityClass, queryClass);
        validate(queryClass, query, binder, validationGroups);

        queryConsumerComposite.accept(query, entityClass);
        logger.info("query={}", Json.toJson(query));
        return query;
    }

    private static void validate(Class<?> clazz, Object value, WebDataBinder binder, Class<?>... groups) throws BindException {
        BindingResult bindingResult = binder.getBindingResult();
        for (Validator validator : binder.getValidators()) {
            logger.info("validator:{},groups={}", validator.getClass(), groups);
            ValidationUtils.invokeValidator(validator, value, bindingResult, groups);
            if (bindingResult.hasErrors()) {
                break;
            }

        }
        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }
    }

    private DomainService<Long, IEntity<Long>> getDomainService(String resource) {
        DomainService<Long, IEntity<Long>> domainService = domainServiceMap.get(resource);

        if (Objects.isNull(domainService)) {
            throw new NotFoundException("not found resource for " + resource);
        }
        return domainService;
    }


    @Override
    @SuppressWarnings("unchecked,rawtypes")
    public void afterSingletonsInstantiated() {

        this.preResourceAuthorize = applicationContext.getBeanProvider(PreResourceAuthorize.class).getIfAvailable();


        String userClassName = applicationContext.getEnvironment().getRequiredProperty("final.security.user-class");

        Class<?> userClass = ClassUtils.resolveClassName(userClassName, getClass().getClassLoader());

        final DomainServiceFactory domainServiceFactory = new DefaultDomainServiceFactory((Class<? extends IUser<?>>) userClass, applicationContext);

        logger.info("userClass:{}", userClass);

        applicationContext.getBeanProvider(AbsService.class).stream()
                .forEach(service -> {
                    Class<?> entityClass = ResolvableType.forClass(AopUtils.getTargetClass(service)).as(AbsService.class).resolveGeneric(1);
                    final DomainResource domainResource = AnnotationUtils.findAnnotation(entityClass, DomainResource.class);

                    if (Objects.nonNull(domainResource)) {
                        DomainService domainService = domainServiceFactory.create(service);
                        for (final String resource : domainResource.value()) {
                            domainServiceMap.put(resource, domainService);
                        }
                    }


                });
    }


}


