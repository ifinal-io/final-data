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

package org.ifinalframework.data.web.core;

import org.ifinalframework.context.FinalContext;
import org.ifinalframework.context.exception.BadRequestException;
import org.ifinalframework.context.exception.InternalServerException;
import org.ifinalframework.core.IAudit;
import org.ifinalframework.core.IEntity;
import org.ifinalframework.core.IEnum;
import org.ifinalframework.core.IQuery;
import org.ifinalframework.core.IStatus;
import org.ifinalframework.core.ITenant;
import org.ifinalframework.core.IUser;
import org.ifinalframework.core.IView;
import org.ifinalframework.data.annotation.YN;
import org.ifinalframework.data.domain.DomainService;
import org.ifinalframework.data.domain.excel.ClassPathDomainResourceExcelExportProvider;
import org.ifinalframework.data.domain.excel.DomainResourceExcelExportProvider;
import org.ifinalframework.data.domain.model.AuditValue;
import org.ifinalframework.data.query.PageQuery;
import org.ifinalframework.data.security.DomainResourceAuth;
import org.ifinalframework.data.spi.SpiAction;
import org.ifinalframework.json.Json;
import org.ifinalframework.poi.Excel;
import org.ifinalframework.poi.Excels;
import org.ifinalframework.web.annotation.bind.RequestEntity;
import org.ifinalframework.web.annotation.bind.RequestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
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

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ResourceDomainController.
 *
 * @author ilikly
 * @version 1.4.2
 * @since 1.4.2
 */
@Configuration
@Transactional
@RestController
@Validated
@RequestMapping("/api/{resource}")
@ConditionalOnWebApplication
public class DomainResourceDispatchController {
    private static final Logger logger = LoggerFactory.getLogger(DomainResourceDispatchController.class);

    private DomainResourceExcelExportProvider domainResourceExcelExportProvider = new ClassPathDomainResourceExcelExportProvider();

    @GetMapping
    @DomainResourceAuth(action = SpiAction.LIST)
    public Object query(@PathVariable String resource, @Valid @RequestQuery(view = IView.List.class) IQuery query,
                        IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> query={}", Json.toJson(query));
        }

        try {
            setFinalContext(query);
            return processResult(domainService.list(query, user));
        } finally {
            clearFinalContext(query);
        }

    }

    @GetMapping("/export")
    @DomainResourceAuth(action = SpiAction.EXPORT)
    public void export(@PathVariable String resource, @Valid @RequestQuery(view = IView.List.class) IQuery query,
                       IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService, HttpServletResponse response) throws Exception{
        if (logger.isDebugEnabled()) {
            logger.debug("==> query={}", Json.toJson(query));
        }

        try {
            setFinalContext(query);

            if(query instanceof PageQuery pageQuery){
                pageQuery.setCount(false);
                pageQuery.setPage(null);
                pageQuery.setSize(null);
            }

            final Object result = processResult(domainService.list(query, user));

            if(result instanceof List<?> list){

                response.setContentType("application/vnd.ms-excel;charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment;filename=\""+ URLEncoder.encode( domainService.entityClass().getSimpleName() + ".xlsx")+"\"");
                response.addHeader("Pargam", "no-cache");
                response.addHeader("Cache-Control", "no-cache");

                final Excel excel = domainResourceExcelExportProvider.getResourceExcel(resource, domainService.entityClass());

                Excels.newWriter(excel)
                        .append(list)
                        .write(response.getOutputStream());
            }

            throw new InternalServerException("导出的结果必须是Collection");




        } finally {
            clearFinalContext(query);
        }
    }


    @GetMapping("/detail")
    @DomainResourceAuth(action = SpiAction.DETAIL)
    public Object detail(@PathVariable String resource, @Valid @RequestQuery(view = IView.Detail.class) IQuery query,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> query={}", Json.toJson(query));
        }
        final Object result = domainService.detail(query, user);
        return processResult(result);
    }

    @GetMapping("/{id}")
    @DomainResourceAuth(action = SpiAction.DETAIL)
    public Object query(@PathVariable String resource, @PathVariable Long id,
                        IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return processResult(domainService.detail(id, user));
    }

    // delete
    @DeleteMapping
    @DomainResourceAuth(action = SpiAction.DELETE)
    public Object delete(@PathVariable String resource, @Valid @RequestQuery(view = IView.Delete.class) IQuery query,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> query={}", Json.toJson(query));
        }
        try {
            setFinalContext(query);
            return processResult(domainService.delete(query, user));
        } finally {
            clearFinalContext(query);
        }
    }

    @DeleteMapping("/{id}")
    @DomainResourceAuth(action = SpiAction.DELETE)
    public Object delete(@PathVariable String resource, @PathVariable Long id,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return processResult(domainService.delete(id, user));
    }


    @PostMapping
    @Validated({IView.Create.class})
    @DomainResourceAuth(action = SpiAction.CREATE)
    public Object create(@PathVariable String resource, @Validated({IView.Create.class}) @Valid @RequestEntity(view = IView.Create.class) Object requestEntity,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("==> entity={}", Json.toJson(requestEntity));
        }

        try {
            setFinalContext(requestEntity);
            Class<?> createEntityClass = domainService.domainEntityClass(IView.Create.class);
            if (Objects.nonNull(createEntityClass)) {
                List<IEntity<Long>> entities = domainService.preInsertFunction().map(requestEntity, user);
                return processResult(domainService.create(entities, user));
            } else if (requestEntity instanceof List<?>) {
                return processResult(domainService.create((List<IEntity<Long>>) requestEntity, user));
            } else if (requestEntity instanceof IEntity<?> entity) {
                final Object result = domainService.create(Collections.singletonList((IEntity<Long>) entity), user);
                if (result instanceof Number) {
                    return processResult(entity.getId());
                }
                return processResult(result);
            }


            throw new BadRequestException("unsupported requestBody format of " + requestEntity);
        } finally {
            clearFinalContext(requestEntity);
        }
    }

    @PutMapping("/{id}")
    @Validated(IView.Update.class)
    @DomainResourceAuth(action = SpiAction.UPDATE)
    public Object update(@PathVariable String resource, @PathVariable Long id, @Valid @RequestEntity(view = IView.Update.class) Object requestEntity,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("==> entity={}", Json.toJson(requestEntity));
        }

        try {
            setFinalContext(requestEntity);

            if (requestEntity instanceof IEntity<?> entity) {
                return processResult(domainService.update((IEntity<Long>) entity, id, false, user));
            }

            throw new BadRequestException("unsupported update requestEntity of " + requestEntity);
        } finally {
            clearFinalContext(requestEntity);
        }

    }

    @PatchMapping("/{id}")
    @Validated(IView.Patch.class)
    @DomainResourceAuth(action = SpiAction.UPDATE)
    public Object patch(@PathVariable String resource, @PathVariable Long id, @Valid @RequestEntity(view = IView.Update.class) Object requestEntity,
                        IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> entity={}", Json.toJson(requestEntity));
        }
        try {
            setFinalContext(requestEntity);

            if (requestEntity instanceof IEntity<?> entity) {
                return processResult(domainService.update((IEntity<Long>) entity, id, true, user));
            }

            throw new BadRequestException("unsupported update requestEntity of " + requestEntity);
        } finally {
            clearFinalContext(requestEntity);
        }
    }

    // status
    @PatchMapping("/{id}/status")
    @DomainResourceAuth(action = SpiAction.UPDATE_STATUS)
    public Object status(@PathVariable String resource, @PathVariable Long id, @RequestParam String status,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {

        if (logger.isDebugEnabled()) {
            logger.debug("==> status={}", status);
        }

        Class<IEntity<Long>> entityClass = domainService.entityClass();

        if (!IStatus.class.isAssignableFrom(entityClass)) {
            throw new BadRequestException("resource is not supports status");
        }

        Class<? extends IEnum<?>> statusClass = (Class<? extends IEnum<?>>) ResolvableType.forClass(entityClass).as(IStatus.class).resolveGeneric();
        final IEnum<?> statusValue = Arrays.stream(statusClass.getEnumConstants())
                .filter(it -> it.getCode().toString().equals(status))
                .findFirst().orElse(null);


        if (Objects.isNull(statusValue)) {
            throw new BadRequestException("not status of " + status);
        }

        return processResult(domainService.status(id, statusValue, user));

    }

    // audit
    @PatchMapping("/{id}/audit")
    @DomainResourceAuth(action = SpiAction.UPDATE_AUDIT_STATUS)
    public Object audit(@PathVariable String resource, @PathVariable Long id, @Valid @RequestBody AuditValue auditValue,
                        IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> auditValue={}", Json.toJson(auditValue));
        }
        if (Objects.equals(auditValue.getStatus(), IAudit.AuditStatus.CANCELED)) {
            throw new BadRequestException("审核不能撤销");
        }
        return processResult(domainService.audit(id, auditValue, user));
    }

    @PatchMapping("/{id}/cancel")
    @DomainResourceAuth(action = SpiAction.UPDATE_AUDIT_STATUS)
    public Object cancel(@PathVariable String resource, @PathVariable Long id, @RequestParam String content,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> content={}", content);
        }
        final AuditValue auditValue = new AuditValue();
        auditValue.setStatus(IAudit.AuditStatus.CANCELED);
        auditValue.setContent(content);
        return processResult(domainService.audit(id, auditValue, user));
    }


    // lock
    @PatchMapping("/{id}/lock")
    @DomainResourceAuth(action = SpiAction.UPDATE_LOCKED)
    public Object lock(@PathVariable String resource, @PathVariable Long id,
                       IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return processResult(domainService.lock(id, false, true, user));
    }

    @PatchMapping("/{id}/unlock")
    @DomainResourceAuth(action = SpiAction.UPDATE_LOCKED)
    public Object unlock(@PathVariable String resource, @PathVariable Long id,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return processResult(domainService.lock(id, true, false, user));
    }


    // yn
    @PatchMapping("/{id}/yn")
    @DomainResourceAuth(action = SpiAction.UPDATE_YN)
    public Object update(@PathVariable String resource, @PathVariable Long id, @RequestParam YN yn,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> yn={}", yn);
        }

        YN current = null;
        switch (yn) {
            case YES -> current = YN.NO;
            case NO -> current = YN.YES;
        }


        return processResult(domainService.yn(id, current, yn, user));
    }

    @PutMapping("/{id}/disable")
    @DomainResourceAuth(action = SpiAction.UPDATE_YN)
    public Object disable(@PathVariable String resource, @PathVariable Long id,
                          IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return processResult(domainService.yn(id, YN.YES, YN.NO, user));
    }

    @PutMapping("/{id}/enable")
    @DomainResourceAuth(action = SpiAction.UPDATE_YN)
    public Object enable(@PathVariable String resource, @PathVariable Long id,
                         IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        return processResult(domainService.yn(id, YN.NO, YN.YES, user));
    }

    // count
    @GetMapping("/count")
    @DomainResourceAuth(action = SpiAction.COUNT)
    public Long count(@PathVariable String resource, @RequestQuery(view = IView.Count.class) IQuery query,
                      IUser<?> user, DomainService<Long, IEntity<Long>, IUser<?>> domainService) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> query={}", Json.toJson(query));
        }
        try {
            setFinalContext(query);
            return processResult(domainService.count(query, user));
        } finally {
            clearFinalContext(query);
        }
    }

    private <R> R processResult(R result) {
        if (logger.isDebugEnabled()) {
            logger.debug("<== {}", Json.toJson(result));
        }
        return result;
    }

    private void setFinalContext(Object value) {
        if (value instanceof ITenant tenant) {
            if (Objects.nonNull(tenant.getTenant())) {
                FinalContext.TENANT.set(tenant.getTenant());
            }
        }
    }

    private void clearFinalContext(Object value) {
        if (value instanceof ITenant) {
            FinalContext.TENANT.remove();
        }
    }


}


