package org.finalframework.sharding.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.Data;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;

/**
 * @author likly
 * @version 1.0.0
 * @see org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "final.sharding.rule")
public class ShardingRuleProperties implements Serializable {

    /**
     * @see ShardingRuleConfiguration#setTables(Collection)
     */
    private Map<String, TableRuleProperties> tables;

    private List<String> bindingTables;

    private List<String> broadcastTables;

    private ShardingStrategyProperties defaultDatabaseShardingStrategy;

    private ShardingStrategyProperties defaultTableShardingStrategy;

    private Map<String, MasterSlaveRuleProperties> masterSlaveRules;

    public Map<String, TableRuleProperties> getTables() {
        return tables;
    }

}

