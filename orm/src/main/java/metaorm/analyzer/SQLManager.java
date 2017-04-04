package metaorm.analyzer;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import metaorm.exceptions.MultipleInheritenceInterfaceException;
import metaorm.exceptions.NotEntityException;
import metaorm.exceptions.PersistenceException;
import metaorm.persistencemanager.PersistenceManagerImpl;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.reflections.Reflections;

import javax.persistence.Id;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SQLManager {

    public static PreparedStatement save(Object object, PersistenceManagerImpl persistenceManager) throws IllegalAccessException, PersistenceException, SQLException {
        ClassAnalyzer classAnalyzer = new ClassAnalyzer(object.getClass());
        List<Object> values = new ArrayList<>();

        String query = "INSERT INTO " + classAnalyzer.getTableName() + "(";

        String columns = "";
        String vals = "";

        for (Map.Entry<Field, ClassAnalyzer.FieldData> entry : classAnalyzer.getColumns().entrySet()) {

            entry.getKey().setAccessible(true);

            //create reference
            if (!(entry.getValue().getType() instanceof String)) {

                Object ref = entry.getKey().get(object);

                //get ID
                ClassAnalyzer cla = new ClassAnalyzer(ref.getClass());
                Map.Entry<Field, ClassAnalyzer.FieldData> id = Iterables.find(cla.getColumns().entrySet(), new Predicate<Map.Entry<Field, ClassAnalyzer.FieldData>>() {
                    @Override
                    public boolean apply(Map.Entry<Field, ClassAnalyzer.FieldData> fieldFieldDataEntry) {
                        return fieldFieldDataEntry.getValue().getAnnotation() instanceof Id;
                    }
                }, null);

                if (id != null) {

                    //save reference
                    if (persistenceManager.save(ref) != 0) {
                        throw new PersistenceException("Error while creating reference");
                    }

                    id.getKey().setAccessible(true);
                    values.add(id.getKey().get(ref));

                }
                else {
                    throw new PersistenceException("Reference has no ID field");
                }

            }
            else {
                values.add(entry.getKey().get(object));
            }

            columns += entry.getValue().getColumnName() + ",";
            vals += "?,";
        }

        columns = new StringBuilder(columns).replace(columns.length() - 1, columns.length(), "").toString();
        vals = new StringBuilder(vals).replace(vals.length() - 1, vals.length(), "").toString();

        query += columns + ") VALUES (" + vals + ")";


        PreparedStatement preparedStatement = persistenceManager.getConnection().prepareStatement(query);
        for (int i = 1; i <= values.size(); i++) {
            preparedStatement.setString(i, values.get(i - 1).toString());
        }

        return preparedStatement;
    }

    public static Object load(Class aClass, ResultSet resultSet, PersistenceManagerImpl persistenceManager) throws PersistenceException {

        Object object = null;
        int id = 0;

        try {

            object = aClass.newInstance();

        } catch (InstantiationException e) {
            e.printStackTrace();
            throw new PersistenceException("No parameter constructor is missing");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new PersistenceException("Could not create object");
        }

        ClassAnalyzer classAnalyzer = new ClassAnalyzer(aClass);

        //go through columns and fetch data
        for (Map.Entry<Field,ClassAnalyzer.FieldData> entry : classAnalyzer.getColumns().entrySet()) {

            //primitive field
            if (entry.getValue().getType() instanceof String) {

                entry.getKey().setAccessible(true);

                if (entry.getKey().getType().getSimpleName().equals(ClassAnalyzer.DOUBLE)) {

                    entry.getKey().setAccessible(true);
                    try {
                        FieldUtils.writeField(entry.getKey(), object, Float.parseFloat(resultSet.getString(entry.getValue().getColumnName())));

                    } catch (IllegalAccessException e) {
                        e.printStackTrace();

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                } else if (entry.getKey().getType().getSimpleName().equals(ClassAnalyzer.INT)) {

                    entry.getKey().setAccessible(true);
                    try {

                        FieldUtils.writeField(entry.getKey(), object, Integer.parseInt(resultSet.getString(entry.getValue().getColumnName())));

                        if (entry.getValue().getAnnotation() instanceof Id) {
                            id = Integer.parseInt(resultSet.getString(entry.getValue().getColumnName()));
                        }

                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                } else if (entry.getKey().getType().getSimpleName().equals(ClassAnalyzer.STRING)) {

                    entry.getKey().setAccessible(true);
                    try {

                        FieldUtils.writeField(entry.getKey(), object, resultSet.getString(entry.getKey().getName().toUpperCase()));

                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                }
            }
            //reference
            else {

                //lazy load
                if (entry.getValue().isLazyLoad() && entry.getKey().getType().isInterface()) {
                    ORMInvocationHandler handler = new ORMInvocationHandler(persistenceManager, id, getClassForInterface(entry.getKey().getType()), object, entry.getKey());
                    try {

                        entry.getKey().setAccessible(true);
                        entry.getKey().set(object, Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{entry.getKey().getType()}, handler));

                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    //get id
                    try {

                        int refId = Integer.parseInt(resultSet.getString(entry.getValue().getColumnName()));
                        try {

                            entry.getKey().setAccessible(true);
                            FieldUtils.writeField(entry.getKey(), object, persistenceManager.get(entry.getKey().getType(), refId));

                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return object;
    }

    public static Class getClassForInterface(Class cl) throws MultipleInheritenceInterfaceException {
        Reflections reflections = new Reflections(cl.getPackage().getName());
        Set<Class> classes = reflections.getSubTypesOf(cl);
        if (classes.size() == 1) {
            return classes.iterator().next();
        }
        else {
            throw new MultipleInheritenceInterfaceException();
        }
    }

    public static PreparedStatement select(Class cl, int id, PersistenceManagerImpl persistenceManager) throws SQLException, NotEntityException {

        PreparedStatement preparedStatement = persistenceManager.getConnection().prepareStatement("SELECT * FROM " + new ClassAnalyzer(cl).getTableName() + " WHERE ID = ?");
        preparedStatement.setString(1, id + "");

        return preparedStatement;
    }

    public static PreparedStatement update(Object object, int id, PersistenceManagerImpl persistenceManager) throws NotEntityException, SQLException {

        List<Object> values = new ArrayList<>();

        ClassAnalyzer classAnalyzer = new ClassAnalyzer(object.getClass());

        String query = "UPDATE " + classAnalyzer.getTableName() + " SET ";

        for (Map.Entry<Field, ClassAnalyzer.FieldData> entry : classAnalyzer.getColumns().entrySet()) {

            entry.getKey().setAccessible(true);
            try {

                if (entry.getValue().getType() instanceof String) {

                    if (!(entry.getValue().getAnnotation() instanceof Id)) {
                        values.add(entry.getKey().get(object));
                        query += entry.getValue().getColumnName() + " = ?,";
                    }

                }
                else {

                    try {

                        persistenceManager.save(entry.getKey().get(object));

                    } catch (PersistenceException e) {
                        e.printStackTrace();
                    }

                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        query = new StringBuilder(query).replace(query.length() - 1, query.length(), "").toString();
        query += " WHERE ID = ?";

        PreparedStatement preparedStatement = persistenceManager.getConnection().prepareStatement(query);

        for (int i = 1; i <= values.size(); i++) {
            preparedStatement.setString(i, values.get(i - 1).toString());
        }

        preparedStatement.setString(values.size() + 1, id + "");
        return preparedStatement;
    }
}
