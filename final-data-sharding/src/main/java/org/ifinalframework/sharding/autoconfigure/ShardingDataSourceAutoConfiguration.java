package org.ifinalframework.sharding.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import org.ifinalframework.sharding.config.ShardingDataSourceSupport;

/**
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = ShardingDataSourceProperties.DEFAULT_DATASOURCE_PREFIX, name = "enable", havingValue = "true")
public class ShardingDataSourceAutoConfiguration extends ShardingDataSourceSupport {

}
