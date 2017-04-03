package metaorm.persistencemanager;

import metaorm.analyzer.ClassAnalyzer;
import metaorm.analyzer.SQLManager;
import metaorm.exceptions.PersistenceException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

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
        return null;
    }

    @Override
    public <T> T get(Class<T> type, int id) throws PersistenceException {
        return null;
    }

    @Override
    public <T> List<T> getBy(Class<T> type, String fieldName, Object value) {

       return null;
    }

    @Override
    public int save(Object value) throws PersistenceException {

        SQLManager sqlManager = new SQLManager(new ClassAnalyzer(value.getClass()), this);

        try {
            sqlManager.save(value).execute();
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

}
