package com.luckyframework.datasources.v2;

import com.alibaba.druid.pool.DruidDataSource;
import com.luckyframework.loosebind.FieldAlias;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Druid数据库连接池的构建者
 * @author fukang
 * @version 1.0.0
 * @date 2022/12/26 08:58
 */
public class DruidDataSourceBuilder extends DataSourceBuilder<DruidDataSource> {
    private static final Logger logger = LoggerFactory.getLogger(DruidDataSourceBuilder.class);

    public static DruidDataSourceBuilder create(){
        DruidDataSourceBuilder builder = new DruidDataSourceBuilder();
        builder.type(DruidDataSource.class);
        builder.dataSourceSupplier(DruidDataSource::new);

        builder.addRequiredFieldAlias(new FieldAlias("jdbcUrl").addAliases("url"));
        builder.addRequiredFieldAlias(new FieldAlias("username").addAliases("user"));
        builder.addRequiredFieldAlias(new FieldAlias("password").addAliases("pwd"));
        builder.addRequiredFieldAlias(new FieldAlias("driverClass").addAliases("driver", "driverName", "driverClassName"));

        builder.addFieldAlias(new FieldAlias("name").addAliases("poolName", "dbname", "dataSourceName"));
        builder.addFieldAlias(new FieldAlias("validationQuery").addAliases("connectionTestQuery"));
        builder.addFieldAlias(new FieldAlias("defaultAutoCommit").addAliases("autoCommit"));
        builder.addFieldAlias(new FieldAlias("minIdle").addAliases("minimumIdle", "minPoolSize"));
        builder.addFieldAlias(new FieldAlias("maxActive").addAliases("maxPoolSize", "maximumPoolSize"));
        return builder;
    }

    @Override
    public DruidDataSource build() {
        try {
            DruidDataSource build = super.build();
            build.init();
            return build;
        } catch (SQLException e) {
            throw new DataSourceBuilderException(e).printException(logger);
        }

    }
}
