package me.tadeas.collectanswer;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * This file was created by Tadeáš Drab on 6. 5. 2020
 */
public class DataSource {

    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;

    static {
        config.setJdbcUrl( "jdbc:mariadb:localhost" );
        config.setUsername( "database" );
        config.setPassword( "**********" );
        config.setMaxLifetime(60000);
        ds = new HikariDataSource( config );
    }

    private DataSource() {}

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}
