package org.ifinalframework.data.jdbc;

import com.alibaba.druid.pool.DruidDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataSourceFactoryManagerTest.
 *
 * @author ilikly
 * @version 1.3.0
 * @since 1.3.0
 */
class DataSourceFactoryManagerTest {

    @Test
    void getDataSourceFactory() {
        DataSourceFactoryManager manager = new DataSourceFactoryManager();
        DataSourceFactory<? extends DataSource> factory = manager.getDataSourceFactory(DruidDataSource.class);
        Assertions.assertTrue(factory instanceof DruidDataSourceFactory);

        factory = manager.getDataSourceFactory(DataSource.class);
        assertFalse(factory instanceof DruidDataSourceFactory);

    }
}