package org.finalframework.mybatis;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;

import org.finalframework.mybatis.configuration.FinalMybatisConfigurationCustomizer;

import java.util.Arrays;
import java.util.Objects;
import javax.sql.DataSource;

import lombok.Setter;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;

/**
 * MyBatisDataSourceAutoConfiguration.
 *
 * @author likly
 * @version 1.0.0
 * @see org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration
 * @since 1.0.0
 */
public class MyBatisDataSourceConfigurationSupport implements BeanFactoryAware {

    @Setter
    private BeanFactory beanFactory;

    protected SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {

        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setPlugins(getObjectProvider(Interceptor.class).getIfAvailable());
        bean.setTypeHandlers(getObjectProvider(TypeHandler.class).getIfAvailable());
        bean.setScriptingLanguageDrivers(getObjectProvider(LanguageDriver.class).getIfAvailable());
        Configuration configuration = new Configuration();
        applyConfiguration(configuration);
        bean.setConfiguration(configuration);
        return bean.getObject();
    }

    private <T> ObjectProvider<T[]> getObjectProvider(Class<T> type) {
        return beanFactory.getBeanProvider(ResolvableType.forArrayComponent(ResolvableType.forClass(type)));
    }

    protected SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    protected void applyConfiguration(Configuration configuration) {
        beanFactory.getBeanProvider(ConfigurationCustomizer.class).forEach(it -> it.customize(configuration));

        Class<?> targetClass = AopUtils.getTargetClass(this);
        MapperScan mapperScan = AnnotationUtils.getAnnotation(targetClass, MapperScan.class);
        if (Objects.nonNull(mapperScan)) {
            FinalMybatisConfigurationCustomizer customizer = new FinalMybatisConfigurationCustomizer();
            customizer.setPackages(Arrays.asList(mapperScan.basePackages()));
        }

    }

}
