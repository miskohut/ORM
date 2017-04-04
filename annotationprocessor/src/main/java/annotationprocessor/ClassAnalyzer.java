package annotationprocessor;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.lang.annotation.Annotation;
import java.util.*;

public class ClassAnalyzer {

    public static final String INT = "int";
    public static final String STRING = "java.lang.String";
    public static final String DOUBLE = "double";

    private Element element;
    private HashMap<String, FieldData> columns;

    private EntityProcessor entityProcessor;

    private String tableName;

    public ClassAnalyzer(Element element, EntityProcessor entityProcessor) {
        this.element = element;
        this.entityProcessor = entityProcessor;

        columns = new HashMap<>();
        fillColumns();

        setTableName();
    }

    private void fillColumns() {
        for (Element element : element.getEnclosedElements()) {

            if (element.getKind() != ElementKind.FIELD || !isSupportedType(element)) {
                continue;
            }

            //get column or id annotation
            Annotation annotation = element.getAnnotation(Id.class);
            if (annotation == null) {
                annotation = element.getAnnotation(Column.class);
            }

            if (annotation != null) {
                columns.put(element.toString(), new FieldData(element, annotation, getFieldType(element)));
            }
        }
    }

    private void setTableName() {
        Annotation annotation = element.getAnnotation(Entity.class);

        if (annotation != null) {
            Entity entity = (Entity) annotation;

            if (!entity.name().equals("")) {
                tableName = entity.name();
            }
            else {
                tableName = element.getSimpleName().toString();
            }
        }
    }

    public String createTable() {
        //check if class has entity annotation

        String statement = "CREATE TABLE " + tableName + "( ";

        //go through INTEGER, FLOAT, TEXT columns
        Iterator iterator = Iterables.filter(columns.entrySet(), entry -> entry.getValue().getType() instanceof String).iterator();

        while (iterator.hasNext()) {
            Map.Entry<Element, FieldData> fieldDataEntry = (Map.Entry<Element, FieldData>) iterator.next();
            statement += fieldDataEntry.getValue().createColumn() + ",";
        }

        return (new StringBuilder(statement).replace(statement.length() - 1, statement.length(), "").toString() + ")").toUpperCase();
    }

    public boolean hasReferences() {
        return Iterables.find(columns.values(), fieldData -> !(fieldData.getType() instanceof String), null) != null;
    }

    public List<String> createReferences() {
        List<String> alters = new ArrayList<String>();

        Iterator iterator = Iterables.filter(columns.values(), fieldData -> !(fieldData.getType() instanceof String)).iterator();


        while (iterator.hasNext()) {
            final FieldData fieldData = (FieldData) iterator.next();

            //get class analyzer for a field type
            ClassAnalyzerHolder holder = Iterables.find(entityProcessor.getClassAnalyzerMap().values(),
                    classAnalyzerHolder -> classAnalyzerHolder.getElement().asType().equals(fieldData.getType()), null);

            if (holder != null) {

                //get Id field of a referencing class
                FieldData fieldData1 = Iterables.find(holder.getClassAnalyzer().getColumns().values(), new Predicate<FieldData>() {
                    @Override
                    public boolean apply(FieldData fieldData) {
                        return fieldData.getAnnotation() instanceof Id;
                    }
                }, null);

                if (fieldData1 != null) {

                    alters.add(("ALTER TABLE " + tableName + " ADD " + holder.getClassAnalyzer().getTableName() + " " +
                            fieldData1.getType().toString() + " REFERENCES " + holder.getClassAnalyzer().getTableName() +
                            "(" + fieldData1.getColumnName() + ")").toUpperCase());

                }
            }
        }

        return alters;
    }

    private boolean isSupportedType(Element element) {
        return  element.asType().toString().equals(INT) ||
                element.asType().toString().equals(STRING) ||
                element.asType().toString().equals(DOUBLE) ||
                (!element.asType().getKind().isPrimitive() && isElementEntity(element));
    }

    private Object getFieldType(Element element) {

        if (element.asType().toString().equals(INT)) {
            return "INTEGER";
        } else if (element.asType().toString().equals(STRING)) {
            return "TEXT";
        } else if (element.asType().toString().equals(DOUBLE)) {
            return "REAL";
        } else if (!element.asType().getKind().isPrimitive()) {
            return element.asType();
        }

        return "TEXT";
    }

    private boolean isElementEntity(Element element) {
        return Iterables.find(entityProcessor.getClassAnalyzerMap().values(), classAnalyzerHolder -> classAnalyzerHolder.getElement().asType().equals(element.asType()), null) != null;
    }

    public HashMap<String, FieldData> getColumns() {
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

        public FieldData(Element element, Annotation annotation, Object type) {
            this.annotation = annotation;
            this.type = type;

            setColumnName(element);
            setRestrictions();
        }

        private void setRestrictions() {
            if (annotation instanceof Column) {
                Column column = (Column) annotation;
                maxLength = column.length();
                nullable = column.nullable();
            }
        }

        private void setColumnName(Element element) {
            if (annotation instanceof Column) {
                Column column = (Column) annotation;
                columnName = column.name();
            }
            else {
                Id id = (Id) annotation;
                columnName = "id";
            }

            if (columnName.equals("")) {
                columnName = element.toString();
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