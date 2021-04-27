package org.ifinalframework.sharding.autoconfigure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

import org.ifinalframework.sharding.config.ShardingConfigurer;
import org.ifinalframework.sharding.config.ShardingTableRegistry;

import java.util.Map;
import java.util.Objects;

/**
 * SharingRuleConfigurer.
 *
 * @author likly
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ShardingRuleProperties.class)
public class SharingRuleConfigurer implements ShardingConfigurer {

    private final ShardingRuleProperties rule;

    public SharingRuleConfigurer(final ShardingRuleProperties rule) {
        this.rule = rule;
    }

    @Override
    public void addShardingTable(@NonNull final ShardingTableRegistry registry) {

        if (Objects.isNull(rule) || CollectionUtils.isEmpty(rule.getTables())) {
            return;
        }

        for (final Map.Entry<String, TableRuleProperties> entry : rule.getTables().entrySet()) {
            String table = entry.getKey();
            TableRuleProperties tableRuleProperties = entry.getValue();
            if (Objects.isNull(tableRuleProperties.getLogicTable())) {
                tableRuleProperties.setLogicTable(table);
            }

        }

    }

}
