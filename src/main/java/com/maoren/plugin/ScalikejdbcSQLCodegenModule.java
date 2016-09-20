package com.maoren.plugin;

import com.mysema.query.codegen.Serializer;
import com.mysema.query.sql.codegen.SQLCodegenModule;
import scalikejdbc.SQLSyntaxSupportFeature;

/**
 * Created by maoren on 16-9-19.
 */
public class ScalikejdbcSQLCodegenModule extends SQLCodegenModule {
    @Override
    protected void configure() {
        super.configure();
        bind(Serializer.class, ScalikejdbcMetaDataSerializer.class);
        bindInstance(ENTITYPATH_TYPE, SQLSyntaxSupportFeature.SQLSyntaxSupport.class);
    }
}
