package com.maoren.plugin;

import org.flywaydb.core.Flyway;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by maoren on 16-9-18.
 */
public class MetaDataExporterTest {
    @Test
    public void  ss() throws ClassNotFoundException, SQLException {
        Class.forName( "org.h2.Driver" );
        Connection connection= DriverManager.getConnection( "jdbc:h2:mem:tmp-domain-db;DB_CLOSE_DELAY=-1" );
        initdb(connection);
        ScalikejdbcMetaDataExporter metaDataExporter=new ScalikejdbcMetaDataExporter();
        new File("/tmp/aaaaa").delete();
        metaDataExporter.setTargetFolder(new File("/tmp/aaaaa"));
        metaDataExporter.setPackageName("com");
        metaDataExporter.setCreateScalaSources(true);
        metaDataExporter.export(connection.getMetaData());
        System.out.print(connection.createStatement().execute("SELECT * FROM PERSON"));
    }

    private void initdb(Connection connection) throws SQLException {
        Flyway flyway = new Flyway();
        flyway.setDataSource("jdbc:h2:mem:tmp-domain-db;DB_CLOSE_DELAY=-1", null, null);
        flyway.migrate();
        connection.createStatement().execute("");
    }
}
