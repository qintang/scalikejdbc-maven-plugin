package com.maoren.plugin;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mysema.codegen.CodeWriter;
import com.mysema.codegen.model.*;
import com.mysema.query.codegen.*;
import com.mysema.query.sql.ColumnMetadata;
import com.mysema.query.sql.ForeignKey;
import com.mysema.query.sql.PrimaryKey;
import com.mysema.query.sql.RelationalPathBase;
import com.mysema.query.sql.codegen.NamingStrategy;
import com.mysema.query.sql.codegen.SQLCodegenModule;
import com.mysema.query.sql.support.ForeignKeyData;
import com.mysema.query.sql.support.InverseForeignKeyData;
import com.mysema.query.sql.support.KeyData;
import com.mysema.query.sql.support.PrimaryKeyData;
import static com.mysema.codegen.Symbols.*;

/**
 * Created by maoren on 16-9-19.
 */
public class ScalikejdbcMetaDataSerializer extends EntitySerializer {
    private static final Map<Integer, String> typeConstants = Maps.newHashMap();

    static {
        try {
            for (Field field : java.sql.Types.class.getDeclaredFields()) {
                if (field.getType().equals(Integer.TYPE)) {
                    typeConstants.put(field.getInt(null), field.getName());
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

    }

    private final NamingStrategy namingStrategy;

    private final boolean innerClassesForKeys;

    private final Set<String> imports;

    private final Comparator<Property> columnComparator;

    private final Class<?> entityPathType;

    /**
     * Create a new MetaDataSerializer instance
     *
     * @param namingStrategy naming strategy for table to class and column to property conversion
     * @param innerClassesForKeys wrap key properties into inner classes (default: false)
     * @param imports java user imports
     */
    @Inject
    public ScalikejdbcMetaDataSerializer(
            TypeMappings typeMappings,
            NamingStrategy namingStrategy,
            @Named(SQLCodegenModule.INNER_CLASSES_FOR_KEYS) boolean innerClassesForKeys,
            @Named(SQLCodegenModule.IMPORTS) Set<String> imports,
            @Named(SQLCodegenModule.COLUMN_COMPARATOR) Comparator<Property> columnComparator,
            @Named(SQLCodegenModule.ENTITYPATH_TYPE) Class<?> entityPathType) {
        super(typeMappings,Collections.<String>emptyList());
        this.namingStrategy = namingStrategy;
        this.innerClassesForKeys = innerClassesForKeys;
        this.imports = new HashSet<String>(imports);
        this.columnComparator = columnComparator;
        this.entityPathType = entityPathType;
    }

    @Deprecated
    public ScalikejdbcMetaDataSerializer(
            TypeMappings typeMappings,
            NamingStrategy namingStrategy,
            boolean innerClassesForKeys,
            Set<String> imports,
            Comparator<Property> columnComparator) {
        this(typeMappings, namingStrategy, innerClassesForKeys,
                imports, columnComparator, RelationalPathBase.class);
    }

    @Override
    protected void constructorsForVariables(CodeWriter writer, EntityType model) throws IOException {
        //super.constructorsForVariables(writer, model);

        /*String localName = writer.getRawName(model);
        String genericName = writer.getGenericName(true, model);

        if (!localName.equals(genericName)) {
            writer.suppressWarnings("all");
        }
        writer.beginConstructor(new Parameter("variable", Types.STRING),
                new Parameter("schema", Types.STRING),
                new Parameter("table", Types.STRING));
        writer.line(SUPER,"(", writer.getClassConstant(localName) + COMMA
                + "forVariable(variable), schema, table);");
        constructorContent(writer, model);
        writer.end();*/
    }

    @Override
    protected void constructors(EntityType model, SerializerConfig config, CodeWriter writer) throws IOException {
        /*super.constructors(model, config, writer);*/
        writer.nl();
        writer.line("").append("  def apply(c: ResultName[").append(writer.getGenericName(false,model))
                .append("])(rs: WrappedResultSet): ").append(writer.getGenericName(false,model))
                .append(" = new ").append(writer.getGenericName(false,model)).append("(");

        writer.beginConstructor(model.getProperties(), new Function<Property, Parameter>() {
            @Nullable
            @Override
            public Parameter apply(@Nullable Property input) {
                String v="0";
                if (((ColumnMetadata)input.getData().get("COLUMN")).isNullable())
                    v="1";
                return new Parameter(v+input.getName(),input.getType());
            }
        });
        writer.nl().line(")");
    }

    @Override
    protected void constructorContent(CodeWriter writer, EntityType model) throws IOException {
        /*writer.line("addMetadata();");*/
    }

    @Override
    protected void introClassHeader(CodeWriter writer, EntityType model) throws IOException {
        Type queryType = typeMappings.getPathType(model, model, true);

        //writer.line("@Generated({\"", getClass().getName(), "\"})");

        TypeCategory category = model.getOriginalCategory();
        // serialize annotations only, if no bean types are used
        if (model.equals(queryType)) {
            for (Annotation annotation : model.getAnnotations()) {
                writer.annotation(annotation);
            }
        }
        writer.beginClass(queryType, new ClassType(category, entityPathType, model));
        writer.privateStaticFinal(Types.LONG_P, "serialVersionUID", String.valueOf(model.hashCode()));
    }

    @Override
    protected String getAdditionalConstructorParameter(EntityType model) {
        StringBuilder builder = new StringBuilder();
        if (model.getData().containsKey("schema")) {
            builder.append(", \"").append(model.getData().get("schema")).append("\"");
        } else {
            builder.append(", null");
        }
        builder.append(", \"").append(model.getData().get("table")).append("\"");
        return builder.toString();
    }

    @Override
    protected void introDefaultInstance(CodeWriter writer, EntityType entityType, String defaultName) throws IOException {
        String variableName = !defaultName.isEmpty() ? defaultName : namingStrategy.getDefaultVariableName(entityType);
        String alias = namingStrategy.getDefaultAlias(entityType);
        Type queryType = typeMappings.getPathType(entityType, entityType, true);
        writer.publicStaticFinal(queryType, variableName, NEW + queryType.getSimpleName() + "(\"" + alias + "\")");
    }

    @Override
    protected void introImports(CodeWriter writer, SerializerConfig config, EntityType model) throws IOException {
        Collection<ForeignKeyData> foreignKeys = (Collection<ForeignKeyData>)
                model.getData().get(ForeignKeyData.class);
        Collection<InverseForeignKeyData> inverseForeignKeys = (Collection<InverseForeignKeyData>)
                model.getData().get(InverseForeignKeyData.class);
        boolean addJavaUtilImport = false;
        if (foreignKeys != null) {
            for (ForeignKeyData keyData : foreignKeys) {
                if (keyData.getForeignColumns().size() > 1) {
                    addJavaUtilImport = true;
                }
            }
        }
        if (inverseForeignKeys != null) {
            for (InverseForeignKeyData keyData : inverseForeignKeys) {
                if (keyData.getForeignColumns().size() > 1) {
                    addJavaUtilImport = true;
                }
            }
        }

        if (addJavaUtilImport) {
            writer.imports(List.class.getPackage());
        }

        //writer.imports(ColumnMetadata.class, java.sql.Types.class);
        writer.importClasses("org.joda.time.DateTime","scalikejdbc._","javax.annotation.Generated");

        //不需要导入了
        /*if (!entityPathType.getPackage().equals(ColumnMetadata.class.getPackage())) {
            writer.imports(entityPathType);
        }*/

        writeUserImports(writer);
    }

    protected void writeUserImports(CodeWriter writer) throws IOException {
        Set<String> packages = new HashSet<String>();
        Set<String> classes = new HashSet<String>();

        for (String javaImport : imports){
            //true if the character next to the dot is an upper case or if no dot is found (-1+1=0) the first character
            boolean isClass = Character.isUpperCase(javaImport.charAt(javaImport.lastIndexOf(".") + 1));
            if (isClass){
                classes.add(javaImport);
            }else{
                packages.add(javaImport);
            }
        }

        String[] marker = new String[]{};
        writer.importPackages(packages.toArray(marker));
        writer.importClasses(classes.toArray(marker));
    }

    @Override
    protected void outro(EntityType model, CodeWriter writer) throws IOException {
        /*writer.beginPublicMethod(Types.VOID,"addMetadata");
        List<Property> properties = Lists.newArrayList(model.getProperties());
        if (columnComparator != null) {
            Collections.sort(properties, columnComparator);
        }
        for (Property property : properties) {
            String name = property.getEscapedName();
            ColumnMetadata metadata = (ColumnMetadata) property.getData().get("COLUMN");
            StringBuilder columnMeta = new StringBuilder();
            columnMeta.append("ColumnMetadata");
            columnMeta.append(".named(\"" + metadata.getName() + "\")");
            columnMeta.append(".withIndex(" + metadata.getIndex() + ")");
            String type = String.valueOf(metadata.getJdbcType());
            if (typeConstants.containsKey(metadata.getJdbcType())) {
                type = "Types." + typeConstants.get(metadata.getJdbcType());
            }
            columnMeta.append(".ofType(" + type + ")");
            if (metadata.hasSize()) {
                columnMeta.append(".withSize(" + metadata.getSize() + ")");
            }
            if (metadata.getDigits() > 0) {
                columnMeta.append(".withDigits(" + metadata.getDigits() + ")");
            }
            if (!metadata.isNullable()) {
                columnMeta.append(".notNull()");
            }
            writer.line("addMetadata(", name, ", ", columnMeta.toString(), ");");
        }
        writer.end();*/

        super.outro(model, writer);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void serializeProperties(EntityType model,  SerializerConfig config,
                                       CodeWriter writer) throws IOException {
        Collection<PrimaryKeyData> primaryKeys =
                (Collection<PrimaryKeyData>) model.getData().get(PrimaryKeyData.class);
        Collection<ForeignKeyData> foreignKeys =
                (Collection<ForeignKeyData>) model.getData().get(ForeignKeyData.class);
        Collection<InverseForeignKeyData> inverseForeignKeys =
                (Collection<InverseForeignKeyData>)model.getData().get(InverseForeignKeyData.class);

        if (innerClassesForKeys) {
            Type primaryKeyType = new SimpleType(namingStrategy.getPrimaryKeysClassName());
            Type foreignKeysType = new SimpleType(namingStrategy.getForeignKeysClassName());

            // primary keys
            if (primaryKeys != null) {
                writer.beginClass(primaryKeyType);
                serializePrimaryKeys(model, writer, primaryKeys);
                writer.end();
            }

            // foreign keys
            if (foreignKeys != null || inverseForeignKeys != null) {
                writer.beginClass(foreignKeysType);
                if (foreignKeys != null) {
                    serializeForeignKeys(model, writer, foreignKeys, false);
                }
                // inverse foreign keys
                if (inverseForeignKeys != null) {
                    serializeForeignKeys(model, writer, inverseForeignKeys, true);
                }
                writer.end();
            }

            super.serializeProperties(model, config, writer);

            if (primaryKeys != null) {
                writer.publicFinal(
                        primaryKeyType,
                        namingStrategy.getPrimaryKeysVariable(model),
                        "new "+primaryKeyType.getSimpleName()+"()");
            }
            if (foreignKeys != null || inverseForeignKeys != null) {
                writer.publicFinal(
                        foreignKeysType,
                        namingStrategy.getForeignKeysVariable(model),
                        "new "+foreignKeysType.getSimpleName()+"()");
            }

        } else {

            super.serializeProperties(model, config, writer);

            // primary keys
            if (primaryKeys != null) {
                serializePrimaryKeys(model, writer, primaryKeys);
            }

            // foreign keys
            if (foreignKeys != null) {
                serializeForeignKeys(model, writer, foreignKeys, false);
            }

            // inverse foreign keys
            if (inverseForeignKeys != null) {
                serializeForeignKeys(model, writer, inverseForeignKeys, true);
            }
        }
    }

    @Override
    protected void customField(EntityType model, Property field, SerializerConfig config,
                               CodeWriter writer) throws IOException {
        Type queryType = typeMappings.getPathType(field.getType(), model, false);
        String localRawName = writer.getRawName(field.getType());
        serialize(model, field, queryType, writer, "create" + field.getType().getSimpleName(),
                writer.getClassConstant(localRawName));
    }

    protected void serializePrimaryKeys(EntityType model, CodeWriter writer,
                                        Collection<PrimaryKeyData> primaryKeys) throws IOException {
        for (PrimaryKeyData primaryKey : primaryKeys) {
            String fieldName = namingStrategy.getPropertyNameForPrimaryKey(primaryKey.getName(), model);
            StringBuilder value = new StringBuilder("createPrimaryKey(");
            boolean first = true;
            for (String column : primaryKey.getColumns()) {
                if (!first) {
                    value.append(", ");
                }
                value.append(namingStrategy.getPropertyName(column, model));
                first = false;
            }
            value.append(")");
            Type type = new ClassType(PrimaryKey.class, model);
            writer.publicFinal(type, fieldName, value.toString());
        }

    }

    protected void serializeForeignKeys(EntityType model, CodeWriter writer,
                                        Collection<? extends KeyData> foreignKeys, boolean inverse) throws IOException {
        for (KeyData foreignKey : foreignKeys) {
            String fieldName;
            if (inverse) {
                fieldName = namingStrategy.getPropertyNameForInverseForeignKey(foreignKey.getName(), model);
            } else {
                fieldName = namingStrategy.getPropertyNameForForeignKey(foreignKey.getName(), model);
            }

            StringBuilder value = new StringBuilder();
            if (inverse) {
                value.append("createInvForeignKey(");
            } else {
                value.append("createForeignKey(");
            }
            if (foreignKey.getForeignColumns().size() == 1) {
                value.append(namingStrategy.getPropertyName(foreignKey.getForeignColumns().get(0), model));
                value.append(", \"" + foreignKey.getParentColumns().get(0) + "\"");
            } else {
                StringBuilder local = new StringBuilder();
                StringBuilder foreign = new StringBuilder();
                for (int i = 0; i < foreignKey.getForeignColumns().size(); i++) {
                    if (i > 0) {
                        local.append(", ");
                        foreign.append(", ");
                    }
                    local.append(namingStrategy.getPropertyName(foreignKey.getForeignColumns().get(i), model));
                    foreign.append("\"" +foreignKey.getParentColumns().get(i) + "\"");
                }
                value.append("Arrays.asList("+local+"), Arrays.asList("+foreign+")");
            }
            value.append(")");
            Type type = new ClassType(ForeignKey.class, foreignKey.getType());
            writer.publicFinal(type, fieldName, value.toString());
        }
    }

}
