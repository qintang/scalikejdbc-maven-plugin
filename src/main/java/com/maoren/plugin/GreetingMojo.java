package com.maoren.plugin;

import com.mysema.query.sql.codegen.MetaDataExporter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by maoren on 16-9-18.
 */
@Mojo(
        name = "generate-domain-models",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES
)
public class GreetingMojo extends AbstractMojo
{
    @Parameter(
            defaultValue = "${project}"
    )
    private MavenProject mavenProject;

    @Parameter(
            defaultValue = "${project.basedir}/src/main/resources/domain-desc.sql"
    )
    private File domainDesc;

    /**
     *
     <jdbcDriver>com.mysql.jdbc.Driver</jdbcDriver>
     <jdbcUrl><![CDATA[jdbc:mysql://localhost:3306/ccms6?useUnicode=true&characterEncoding=utf8&autoReconnect=true&allowMultiQueries=true]]></jdbcUrl>
     <jdbcUser>root</jdbcUser>
     <jdbcPassword>root</jdbcPassword>
     * */
    @Parameter()
    private String jdbcDriver;
    @Parameter()
    private String jdbcUrl;
    @Parameter()
    private String jdbcUser;
    @Parameter()
    private String jdbcPassword;


    @Parameter(
            defaultValue = "${project.basedir}/target/generated-sources/scala"
    )
    private File modelDirectory;

    @Parameter(
            required = true
    )
    private String modelPackage;

    public void execute( ) throws MojoExecutionException, MojoFailureException {
        if( !this.modelDirectory.exists( ) ) {
            this.modelDirectory.mkdirs( );
        }
        if( !this.modelDirectory.isDirectory( ) ) {
            throw new MojoExecutionException( String.format( "'%s' is not a valid destination directory.", this.modelDirectory ) );
        }
        getLog().info(String.format( "domainDesc:'%s'", this.domainDesc  ));
        getLog().info(String.format( "modelDirectory:'%s'", this.modelDirectory  ));
        getLog().info(String.format( "modelPackage:'%s'", this.modelPackage  ));

        Connection con = null;
        try {
            if (isNullCustomerConnection(this.jdbcDriver,this.jdbcUrl,this.jdbcUser,this.jdbcPassword)){
                if( !this.domainDesc.exists( ) || !this.domainDesc.isFile( ) ) {
                    throw new MojoExecutionException( String.format( "'%s' is not a valid domain description file.", this.domainDesc ) );
                }
                con = this.createTemporaryH2Database();
                this.loadDomainModel( con );
            }else {
                con = this.createCustomerDatabase(this.jdbcDriver,this.jdbcUrl,this.jdbcUser,this.jdbcPassword);
            }
            this.writeModelSources( con );

            this.mavenProject.addCompileSourceRoot( this.modelDirectory.getAbsolutePath( ) );
        }
        catch( SQLException sqle ) {
            throw new MojoExecutionException( "Failed to load domain model into temporary H2 memory database.", sqle );
        }
        finally {
            if( con != null ) {
                try {
                    con.close( );
                }
                catch( SQLException sqle ) { }
            }
        }
    }
    private boolean isNullCustomerConnection(String jdbcDriver,String jdbcUrl,String jdbcUser,String jdbcPassword){
        boolean existsNull=false;
        if (StringUtils.isEmpty(jdbcDriver)){
            existsNull= true;
        }
        getLog().info(String.format( "jdbcDriver:'%s'",jdbcDriver ));

        if (StringUtils.isEmpty(jdbcUrl)){
            existsNull= true;
        }
        getLog().info(String.format( "jdbcUrl:'%s'",jdbcUrl ));

        if (StringUtils.isEmpty(jdbcUser)){
            existsNull= true;
        }
        getLog().info(String.format( "jdbcUser:'%s'",jdbcUser ));

        if (StringUtils.isEmpty(jdbcPassword)){
            existsNull= true;
        }
        getLog().info(String.format( "jdbcPassword:'%s'","******" ));
        return existsNull;
    }
    private Connection createCustomerDatabase(String jdbcDriver,String jdbcUrl,String jdbcUser,String jdbcPassword)
            throws SQLException, MojoExecutionException {
        try {
            Class.forName( jdbcDriver );
            return DriverManager.getConnection(jdbcUrl,jdbcUser,jdbcPassword );
        }
        catch( ClassNotFoundException cnfe ) {
            throw new MojoExecutionException( "Failed to load H2 driver class." );
        }
    }

    private Connection createTemporaryH2Database( ) throws MojoExecutionException, SQLException {
        try {
            Class.forName( "org.h2.Driver" );
            return DriverManager.getConnection( "jdbc:h2:mem:tmp-domain-db;DB_CLOSE_DELAY=-1" );
        }
        catch( ClassNotFoundException cnfe ) {
            throw new MojoExecutionException( "Failed to load H2 driver class." );
        }
    }

    private void loadDomainModel( Connection con ) throws MojoExecutionException, SQLException {
        String domainSQL = "";

        BufferedReader domainReader = null;
        try {
            domainReader = new BufferedReader( new FileReader( this.domainDesc ) );

            String line = "";
            while( ( line = domainReader.readLine( ) ) != null ) {
                domainSQL = domainSQL + line + "\n";
            }
        }
        catch( IOException ioe ) {
            throw new MojoExecutionException( "Failed to read domain model.", ioe );
        }
        finally {
            try {
                domainReader.close( );
            }
            catch( IOException ioe ) { }
        }

        Statement s = null;
        try {
            s = con.createStatement( );
            s.execute( domainSQL );
        }
        finally {
            try {
                s.close( );
            }
            catch( SQLException sqle ) { }
        }
    }

    private void writeModelSources( Connection con ) throws MojoExecutionException, SQLException {
        ScalikejdbcMetaDataExporter exporter = new ScalikejdbcMetaDataExporter();
        exporter.setPackageName(this.modelPackage);
        exporter.setTargetFolder(this.modelDirectory);
        exporter.setCreateScalaSources(true);

        exporter.export(con.getMetaData());
    }
}