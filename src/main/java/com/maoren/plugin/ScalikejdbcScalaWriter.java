package com.maoren.plugin;

import com.google.common.base.Function;
import com.mysema.codegen.AbstractCodeWriter;
import com.mysema.codegen.ScalaWriter;
import com.mysema.codegen.StringUtils;
import com.mysema.codegen.model.Parameter;
import com.mysema.codegen.model.Type;
import com.mysema.codegen.support.ScalaSyntaxUtils;
import com.mysema.query.codegen.*;
import com.mysema.query.sql.ColumnMetadata;
import com.mysema.query.sql.Configuration;
import com.mysema.query.sql.SQLTemplatesRegistry;
import com.mysema.query.sql.codegen.KeyDataFactory;
import com.mysema.query.sql.codegen.MetaDataExporter;
import com.mysema.query.sql.codegen.NamingStrategy;
import com.mysema.query.sql.codegen.SQLCodegenModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;

import static com.mysema.codegen.Symbols.ASSIGN;
import static com.mysema.codegen.Symbols.COMMA;

/**
 * Created by maoren on 16-9-19.
 *
 *
 import org.joda.time.DateTime
 case class WorkflowNode(
 id: Long,
 name: String,
 workflowId:Long,
 x: Int,
 y: Int,
 `type`: String,
 remark:Option[String],
 transferVersion:String,
 transferPkNodeId:Option[Long])

 object  WorkflowNode extends SQLSyntaxSupport[WorkflowNode] {

 override val columns = Seq(
 "id", "name", "workflow_id", "x", "y", "type", "remark","reserved","transfer_version","transfer_pk_node_id"
 )

 def apply(c: SyntaxProvider[WorkflowNode])(rs: WrappedResultSet): WorkflowNode = apply(c.resultName)(rs)

 def apply(c: ResultName[WorkflowNode])(rs: WrappedResultSet): WorkflowNode = new WorkflowNode(
 id = rs.get[Long](c.id),
 name = rs.get[Option[String]](c.name).get,
 workflowId = rs.get[Long](c.workflowId),
 x = rs.get[Int](c.x),
 y = rs.get[Int](c.y),
 `type` = rs.get[String](c.`type`),
 remark = rs.get[Option[String]](c.remark),
 transferVersion = rs.get[Option[String]](c.transferVersion).get,
 transferPkNodeId = rs.get[Option[Long]](c.transferPkNodeId)
 )
 }
 */
public class ScalikejdbcScalaWriter extends AbstractCodeWriter<ScalikejdbcScalaWriter> {
    private static final Set<String> PRIMITIVE_TYPES = new HashSet<String>(Arrays.asList("boolean",
            "byte", "char", "int", "long", "short", "double", "float"));

    private static final String DEF = "def ";

    private static final String OVERRIDE_DEF = "override " + DEF;

    private static final String EXTENDS = " extends ";

    private static final String WITH = " with ";

    private static final String IMPORT = "import ";

    private static final String IMPORT_STATIC = "import ";

    private static final String PACKAGE = "package ";

    private static final String PRIVATE = "private ";

    private static final String PRIVATE_VAL = "private val ";

    private static final String PROTECTED = "protected ";

    private static final String PROTECTED_VAL = "protected val ";

    private static final String PUBLIC = "public ";

    private static final String PUBLIC_CLASS = "class ";

    private static final String PUBLIC_OBJECT = "object ";

    private static final String CASE_CLASS = "case class ";

    private static final String VAR = "var ";

    private static final String VAL = "val ";

    private static final String THIS = "this";

    private static final String TRAIT = "trait ";

    private static final String APPLY="apply";

    private final Set<String> classes = new HashSet<String>();

    private final Set<String> packages = new HashSet<String>();

    private Type type;

    private final boolean compact;

    public ScalikejdbcScalaWriter(Appendable appendable) {
        this(appendable, false);
    }

    public ScalikejdbcScalaWriter(Appendable appendable, boolean compact) {
        super(appendable, 2);
        this.classes.add("java.lang.String");
        this.classes.add("java.lang.Object");
        this.classes.add("java.lang.Integer");
        this.classes.add("java.lang.Comparable");
        this.compact = compact;
    }

    @Override
    public String getRawName(Type type) {
        String fullName = type.getFullName();
        if (PRIMITIVE_TYPES.contains(fullName)) {
            fullName = StringUtils.capitalize(fullName);
        }
        String packageName = type.getPackageName();
        if (packageName != null && packageName.length() > 0) {
            fullName = packageName + "." + fullName.substring(packageName.length()+1).replace('.', '$');
        } else {
            fullName = fullName.replace('.', '$');
        }
        String rv = fullName;
        if (type.isPrimitive() && packageName.isEmpty()) {
            rv = Character.toUpperCase(rv.charAt(0)) + rv.substring(1);
        }
        if (packages.contains(packageName) || classes.contains(fullName)) {
            if (packageName.length() > 0) {
                rv = fullName.substring(packageName.length() + 1);
            }
        }
        if (rv.endsWith("[]")) {
            rv = rv.substring(0, rv.length() - 2);
            if (PRIMITIVE_TYPES.contains(rv)) {
                rv = StringUtils.capitalize(rv);
            } else if (classes.contains(rv)) {
                rv = rv.substring(packageName.length() + 1);
            }
            return "Array[" + rv + "]";
        } else {
            return rv;
        }
    }

    @Override
    public String getGenericName(boolean asArgType, Type type) {
        if (type.getFullName().equals(Integer.class.getCanonicalName())){
            return "Int";
        }
        if(type.getFullName().equals("java.sql.Timestamp")){
            return "DateTime";
        }
        Set<String> scalat=new HashSet<>();
        scalat.add(Long.class.getCanonicalName());
        scalat.add(Boolean.class.getCanonicalName());
        scalat.add(Byte.class.getCanonicalName());
        scalat.add(Double.class.getCanonicalName());
        scalat.add(Float.class.getCanonicalName());
        scalat.add(Short.class.getCanonicalName());
        if (scalat.contains(type.getFullName())){
            return type.getFullName().substring("java.lang".length()+1);
        }

        if (type.getParameters().isEmpty()) {
            return getRawName(type);
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append(getRawName(type));
            builder.append("[");
            boolean first = true;
            String fullName = type.getFullName();
            for (Type parameter : type.getParameters()) {
                if (!first) {
                    builder.append(", ");
                }
                if (parameter == null || parameter.getFullName().equals(fullName)) {
                    builder.append("_");
                } else {
                    builder.append(getGenericName(false, parameter));
                }
                first = false;
            }
            builder.append("]");
            return builder.toString();
        }
    }

    @Override
    public String getClassConstant(String className) {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter annotation(Annotation annotation) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter annotation(Class<? extends Annotation> annotation) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter beginClass(Type type) throws IOException {
        return beginClass(type, null);
    }

    @Override
    public ScalikejdbcScalaWriter beginClass(Type type, Type superClass, Type... interfaces) throws IOException {
        //构造calss class
        beginLine(CASE_CLASS, getGenericName(false, type));
        append("(").nl();
        if (!((EntityType)type).getProperties().isEmpty()){
            boolean first = true;
            for (Property pro: ((EntityType)type).getProperties()) {
                if (!first) {
                    append(COMMA);
                }

                String optionType=getGenericName(true, pro.getType());
                if (((ColumnMetadata)pro.getData().get("COLUMN")).isNullable())
                    optionType="Option["+getGenericName(true, pro.getType())+"]";
                line("        ",VAR, escape(pro.getEscapedName()), ": ",optionType);
                first = false;
            }
        }
        append(")").nl().nl();

        packages.add(type.getPackageName());
        beginLine(PUBLIC_OBJECT, getGenericName(false, type));
        if (superClass != null) {
            if (getGenericName(false,superClass).startsWith("scalikejdbc")){
                String[] s=getGenericName(false,superClass).split("\\$");
                append(EXTENDS).append(s[s.length-1]);
            }else {
                append(EXTENDS).append(getGenericName(false,superClass));
            }

        }
        if (interfaces.length > 0) {
            if (superClass == null) {
                append(EXTENDS);
                append(getGenericName(false, interfaces[0]));
                append(WITH);
                for (int i = 1; i < interfaces.length; i++) {
                    if (i > 1) {
                        append(COMMA);
                    }
                    append(getGenericName(false, interfaces[i]));
                }
            } else {
                append(WITH);
                for (int i = 0; i < interfaces.length; i++) {
                    if (i > 0) {
                        append(COMMA);
                    }
                    append(getGenericName(false, interfaces[i]));
                }
            }
        }
        append(" {").nl().nl();

        //加入override val columns = Seq("id", "name")
        StringBuffer sb=new StringBuffer();
        if (!((EntityType)type).getProperties().isEmpty()){
            boolean first = true;
            for (Property pro: ((EntityType)type).getProperties()) {
                if (!first) {
                    sb.append(COMMA);
                }
                sb.append("\"").append(((ColumnMetadata)pro.getData().get("COLUMN")).getName()).append("\"");
                first = false;
            }
        }
        append("  override val columns = Seq(").append(sb.subSequence(0,sb.length())).append(")").nl();

        //加入 def apply(c: SyntaxProvider[WorkflowNode])(rs: WrappedResultSet): WorkflowNode = apply(c.resultName)(rs)
        append("  def apply(c: SyntaxProvider[").append(getGenericName(false, type))
                .append("])(rs: WrappedResultSet): ")
                .append(getGenericName(false, type)).append(" = apply(c.resultName)(rs)").nl();

        goIn();
        this.type = type;
        return this;
    }

    @Override
    public <T> ScalikejdbcScalaWriter beginConstructor(Collection<T> parameters, Function<T, Parameter> transformer) throws IOException {
        beginLine("").params(parameters, transformer);
        return goIn();
    }

    @Override
    public ScalikejdbcScalaWriter beginConstructor(Parameter... params) throws IOException {
        //beginLine(DEF, APPLY).params(params).append(" {").nl();
        return goIn();
    }

    @Override
    public ScalikejdbcScalaWriter beginInterface(Type type, Type... interfaces) throws IOException {
        return null;
    }

    @Override
    public <T> ScalikejdbcScalaWriter beginPublicMethod(Type returnType, String methodName, Collection<T> parameters, Function<T, Parameter> transformer) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter beginPublicMethod(Type returnType, String methodName, Parameter... args) throws IOException {
        return null;
    }

    @Override
    public <T> ScalikejdbcScalaWriter beginStaticMethod(Type type, String name, Collection<T> params, Function<T, Parameter> transformer) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter beginStaticMethod(Type returnType, String methodName, Parameter... args) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter end() throws IOException {
        goOut();
        return line("}").nl();
    }

    @Override
    public ScalikejdbcScalaWriter field(Type type, String name) throws IOException {
        line(VAR, escape(name), ": ", getGenericName(true, type));
        return compact ? this : nl();
    }

    private ScalikejdbcScalaWriter field(String modifier, Type type, String name) throws IOException {
        line(modifier, escape(name), ": ", getGenericName(true, type));
        return compact ? this : nl();
    }

    private ScalikejdbcScalaWriter field(String modifier, Type type, String name, String value)
            throws IOException {
        line(modifier, escape(name), ": ", getGenericName(true, type), ASSIGN, value);
        return compact ? this : nl();
    }

    @Override
    public ScalikejdbcScalaWriter imports(Class<?>... imports) throws IOException {
        for (Class<?> cl : imports) {
            classes.add(cl.getName());
            line(IMPORT, cl.getName());
        }
        nl();
        return this;
    }

    @Override
    public ScalikejdbcScalaWriter imports(Package... imports) throws IOException {
        for (Package p : imports) {
            packages.add(p.getName());
            line(IMPORT, p.getName(), "._");
        }
        nl();
        return this;
    }

    @Override
    public ScalikejdbcScalaWriter importClasses(String... imports) throws IOException {
        for (String cl : imports) {
            classes.add(cl);
            line(IMPORT, cl);
        }
        nl();
        return this;
    }

    @Override
    public ScalikejdbcScalaWriter importPackages(String... imports) throws IOException {
        for (String p : imports) {
            packages.add(p);
            line(IMPORT, p, "._");
        }
        nl();
        return this;
    }

    @Override
    public ScalikejdbcScalaWriter javadoc(String... lines) throws IOException {
        line("/**");
        for (String line : lines) {
            line(" * ", line);
        }
        return line(" */");
    }

    @Override
    public ScalikejdbcScalaWriter packageDecl(String packageName) throws IOException {
        packages.add(packageName);
        return line(PACKAGE, packageName).nl();
    }

    @Override
    public ScalikejdbcScalaWriter privateField(Type type, String name) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter privateFinal(Type type, String name) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter privateFinal(Type type, String name, String value) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter privateStaticFinal(Type type, String name, String value) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter protectedField(Type type, String name) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter protectedFinal(Type type, String name) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter protectedFinal(Type type, String name, String value) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter publicField(Type type, String name) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter publicField(Type type, String name, String value) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter publicFinal(Type type, String name) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter publicFinal(Type type, String name, String value) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter publicStaticFinal(Type type, String name, String value) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter staticimports(Class<?>... imports) throws IOException {
        for (Class<?> cl : imports) {
            line(IMPORT_STATIC, cl.getName(), "._;");
        }
        return this;
    }


    @Override
    public ScalikejdbcScalaWriter suppressWarnings(String type) throws IOException {
        return null;
    }

    @Override
    public ScalikejdbcScalaWriter suppressWarnings(String... types) throws IOException {
        return null;
    }

    private String escape(String token) {
        if (ScalaSyntaxUtils.isReserved(token)) {
            return "`" + token + "`";
        } else {
            return token;
        }
    }

    private ScalikejdbcScalaWriter param(Parameter parameter) throws IOException {
        //id = rs.get[Long](c.id),
        String name=parameter.getName().substring(1);
        String isNullable=parameter.getName().substring(0,1);

        append(escape(name));
        append(" = ").append("rs.get[");

        String optionType=getGenericName(true, parameter.getType());
        if (isNullable.equals("1"))
            optionType="Option["+optionType+"]";

        append(optionType);
        append("](c.");
        if (name.equals("c")){
            append("selectDynamic(\"c\")");
        }else{
            append(escape(name));
        }
        append(")");
        return this;
    }

    private <T> ScalikejdbcScalaWriter params(Collection<T> parameters, Function<T, Parameter> transformer)
            throws IOException {
        nl();
        boolean first = true;
        for (T param : parameters) {
            if (!first) {
                append(COMMA);
            }
            param(transformer.apply(param));
            first = false;
            nl();
        }
        return this;
    }

    private ScalikejdbcScalaWriter params(Parameter... params) throws IOException {
        append("(");
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                append(COMMA);
            }
            param(params[i]);
        }
        append(")");
        return this;
    }

}
