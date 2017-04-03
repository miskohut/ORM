package metaorm.analyzer;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import metaorm.exceptions.NotEntityException;
import metaorm.exceptions.PersistenceException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

public class ClassAnalyzer {

    public static final String INT = "int";
    public static final String STRING = "String";
    public static final String DOUBLE = "double";

    private Class aClass;
    private HashMap<Field, FieldData> columns;

    private String tableName;

    public ClassAnalyzer(Class aClass) throws NotEntityException {
        this.aClass = aClass;
        checkEntity();

        columns = new HashMap<Field, FieldData>();
        fillColumns();

        setTableName();
    }

    private void fillColumns() {
        for (Field field : aClass.getDeclaredFields()) {

            if (!isSupportedType(field)) {
                continue;
            }

            Annotation annotation = Iterables.find(Arrays.asList(field.getAnnotations()), new Predicate<Annotation>() {
                @Override
                public boolean apply(Annotation annotation) {
                    return annotation instanceof Id || annotation instanceof Column;
                }
            }, null);

            if (annotation != null && isSupportedType(field)) {
                columns.put(field, new FieldData(field, annotation, getFieldType(field)));
            }
        }
    }

    private void setTableName() {
        Annotation annotation = Iterables.find(Arrays.asList(aClass.getAnnotations()), new Predicate<Annotation>() {
            @Override
            public boolean apply(Annotation annotation) {
                return annotation instanceof Entity;
            }
        }, null);

        if (annotation != null) {
            Entity entity = (Entity) annotation;

            if (!entity.name().equals("")) {
                tableName = entity.name();
            }
            else {
                tableName = aClass.getSimpleName();
            }
        }
    }

    public String createTable() throws PersistenceException {
        //check if class has entity annotation
        checkEntity();

        String statement = "CREATE TABLE " + tableName + "( ";

        //go through INTEGER, FLOAT, TEXT columns
        Iterator iterator = Iterables.filter(columns.entrySet(), new Predicate<Map.Entry<Field, FieldData>>() {
            @Override
            public boolean apply(Map.Entry<Field, FieldData> fieldFieldDataEntry) {
                return fieldFieldDataEntry.getValue().getType() instanceof  String;
            }
        }).iterator();

        while (iterator.hasNext()) {
            Map.Entry<Field, FieldData> fieldDataEntry = (Map.Entry<Field, FieldData>) iterator.next();
            statement += fieldDataEntry.getValue().createColumn() + ",";
        }

        return new StringBuilder(statement).replace(statement.length() - 1, statement.length(), "").toString() + ")";
    }

    public boolean hasReferences() {
        return Iterables.find(columns.values(), new Predicate<FieldData>() {
            @Override
            public boolean apply(FieldData fieldData) {
                return !(fieldData.getType() instanceof String);
            }
        }, null) != null;
    }

    public List<String> createReferences(List<ClassAnalyzer> classAnalyzers) {
        List<String> alters = new ArrayList<String>();

        Iterator iterator = Iterables.filter(columns.entrySet(), new Predicate<Map.Entry<Field, FieldData>>() {
            @Override
            public boolean apply(Map.Entry<Field, FieldData> fieldFieldDataEntry) {
                return !(fieldFieldDataEntry.getValue().getType() instanceof String);
            }
        }).iterator();

        while (iterator.hasNext()) {
            final Map.Entry<Field, FieldData> fieldDataEntry = (Map.Entry<Field, FieldData>) iterator.next();

            //get class analyzer for a field type
            ClassAnalyzer classAnalyzer = Iterables.find(classAnalyzers, new Predicate<ClassAnalyzer>() {
                @Override
                public boolean apply(ClassAnalyzer classAnalyzer) {
                    return fieldDataEntry.getKey().getType().equals(classAnalyzer.aClass);
                }
            }, null);

            if (classAnalyzer != null) {

                //get Id field of a referencing class
                Map.Entry<Field, FieldData> referencingFieldEntry = Iterables.find(classAnalyzer.getColumns().entrySet(), new Predicate<Map.Entry<Field, FieldData>>() {
                    @Override
                    public boolean apply(Map.Entry<Field, FieldData> fieldFieldDataEntry) {
                        return fieldDataEntry.getValue().getAnnotation() instanceof Id;
                    }
                }, null);

                if (referencingFieldEntry != null) {

                    alters.add("ALTER TABLE " + tableName + " ADD " + referencingFieldEntry.getValue().getColumnName() +
                            referencingFieldEntry.getValue().getType().toString() + " REFERENCING " + classAnalyzer.getTableName() +
                            "(" + referencingFieldEntry.getValue().getColumnName() + ")");

                }
            }
        }

        return alters;
    }

    private void checkEntity() throws NotEntityException {
        if (Iterables.find(Arrays.asList(aClass.getAnnotations()), new Predicate<Annotation>() {
            @Override
            public boolean apply(Annotation annotation) {
                return annotation instanceof Entity;
            }
        }, null) == null) {
            throw new NotEntityException("Class + " + aClass.getName() + " is not entity");
        }
    }

    private boolean isSupportedType(Field field) {
        return field.getType().getSimpleName().equals(INT) || field.getType().getSimpleName().equals(STRING) || field.getType().getSimpleName().equals(DOUBLE) || (!field.getType().isPrimitive() && isFieldEntity(field));
    }

    private Object getFieldType(Field field) {

        if (field.getType().getSimpleName().equals(INT)) {
            return "INTEGER";
        } else if (field.getType().getSimpleName().equals(STRING)) {
            return "TEXT";
        } else if (field.getType().getSimpleName().equals(DOUBLE)) {
            return "REAL";
        } else if (!field.getType().isPrimitive()) {
            return field.getType();
        }

        return "TEXT";
    }

    private boolean isFieldEntity(Field field) {
        return !Iterables.find(Arrays.asList(field.getType().getAnnotations()), annotation -> annotation instanceof Entity, null).equals(null);
    }

    public HashMap<Field, FieldData> getColumns() {
        return columns;
    }

    public String getTableName() {
        return tableName;
    }

    public class FieldData {
        private Annotation annotation;
        private Object type;
        private String columnName;
        private boolean nullable;
        private int maxLength;

        public FieldData(Field field, Annotation annotation, Object type) {
            this.annotation = annotation;
            this.type = type;

            setColumnName(field);
            setRestrictions();
        }

        private void setRestrictions() {
            if (annotation instanceof Column) {
                Column column = (Column) annotation;
                maxLength = column.length();
                nullable = column.nullable();
            }
        }

        private void setColumnName(Field field) {
            if (annotation instanceof Column) {
                Column column = (Column) annotation;
                columnName = column.name();
            }
            else {
                Id id = (Id) annotation;
                columnName = "id";
            }

            if (columnName.equals("")) {
                columnName = field.getName();
            }
        }

        public Annotation getAnnotation() {
            return annotation;
        }

        public Object getType() {
            return type;
        }

        public String getColumnName() {
            return columnName;
        }

        public boolean isNullable() {
            return nullable;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public String createColumn() {
            String statement = "";

            if (type instanceof String) {
                if (annotation instanceof Id) {
                    statement = columnName + " " + type.toString() + " PRIMARY KEY";
                }
                else {
                    statement = columnName + " " + type.toString() + " " + (type.toString().equals("TEXT") ? "(" + maxLength + ")" : "") +((!nullable) ? " NOT NULL " : "");
                }
            }

            return statement;
        }
    }
}