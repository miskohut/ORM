package metaorm.persistencemanager;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import metaorm.analyzer.ClassAnalyzer;
import metaorm.analyzer.SQLManager;
import metaorm.exceptions.NotEntityException;
import metaorm.exceptions.PersistenceException;

import javax.persistence.Id;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PersistenceManagerImpl implements PersistenceManager {

    private Connection connection;

    public PersistenceManagerImpl(Connection connection) throws PersistenceException {
        this.connection = connection;
        initializeDatabase();
    }


    @Override
    public void initializeDatabase() throws PersistenceException {

        try {

            Statement statement = connection.createStatement();

            for (String query : FileReader.getQueries()) {

                try {

                    statement.execute(query);

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            throw new PersistenceException(e.getMessage());
        } catch (SQLException e) {
            throw new PersistenceException("Cannot create statement");
        }

    }

    @Override
    public <T> List<T> getAll(Class<T> clazz) throws PersistenceException {
        List<T> objects = new ArrayList<T>();

        try {
            Statement statement = connection.createStatement();

            ClassAnalyzer classAnalyzer = new ClassAnalyzer(clazz);

            //get all ids from database
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + classAnalyzer.getTableName());

            //get ID field name
            ClassAnalyzer.FieldData fieldData = Iterables.find(classAnalyzer.getColumns().values(), fieldData1 -> fieldData1.getAnnotation() instanceof Id, null);

            if (fieldData != null) {

                while (resultSet.next()) {
                    objects.add(get(clazz, Integer.parseInt(resultSet.getString(fieldData.getColumnName()))));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return objects;
    }

    @Override
    public <T> T get(Class<T> type, int id) throws PersistenceException {

        Object object = null;

        try {
            ResultSet resultSet = SQLManager.select(type, id, this).executeQuery();
            if (resultSet.next()) {
                object = SQLManager.load(type, resultSet, this);

                resultSet.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return (T) object;
    }

    @Override
    public <T> List<T> getBy(Class<T> type, String fieldName, Object value) {

        List<T> objects = new ArrayList<T>();

        try {
            ClassAnalyzer classAnalyzer = new ClassAnalyzer(type.getClass());
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM " + classAnalyzer.getTableName() + " WHERE " + fieldName + " = " + ((value instanceof String ) ? "'" + value.toString() + "'" : value.toString()));

            //get ID field name
            ClassAnalyzer.FieldData fieldData = Iterables.find(classAnalyzer.getColumns().values(), fieldData1 -> fieldData1.getAnnotation() instanceof Id, null);

            if (fieldData != null) {

                while (resultSet.next()) {
                    objects.add(get(type, Integer.parseInt(resultSet.getString(fieldData.getColumnName()))));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (PersistenceException e) {
            e.printStackTrace();
        }

        return objects;
    }

    @Override
    public int save(Object value) throws PersistenceException {

        //check if object already exits
        int id = getEntityId(value);
        Object object = get(value.getClass(), id);

        try {

            if (object != null) {
                if (SQLManager.update(value, id, this).execute()) {
                    return 4;
                }
            }
            else {
                if (SQLManager.save(value, this).execute()) {
                    return 4;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 4;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return 4;
        }

        return 0;
    }

    public Connection getConnection() {
        return connection;
    }

    private int getEntityId(Object object) {
        ClassAnalyzer classAnalyzer = null;
        try {

            classAnalyzer = new ClassAnalyzer(object.getClass());

        } catch (NotEntityException e) {
            e.printStackTrace();
            return -1;
        }

        Map.Entry<Field, ClassAnalyzer.FieldData> entry = Iterables.find(classAnalyzer.getColumns().entrySet(), fieldFieldDataEntry -> fieldFieldDataEntry.getValue().getAnnotation() instanceof Id, null);

        if (entry != null) {
            entry.getKey().setAccessible(true);
            try {
                return Integer.valueOf(entry.getKey().get(object).toString());
            } catch (IllegalAccessException e) {
                return -1;
            }
        }

        return -1;
    }

}
