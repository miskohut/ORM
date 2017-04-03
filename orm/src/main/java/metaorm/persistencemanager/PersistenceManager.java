package metaorm.persistencemanager;


import metaorm.exceptions.PersistenceException;

import java.util.List;

public interface PersistenceManager {

    void initializeDatabase() throws PersistenceException;

    <T> List<T> getAll(Class<T> clazz) throws PersistenceException;

    <T> T get(Class<T> type, int id) throws PersistenceException;

    <T> List<T> getBy(Class<T> type, String fieldName, Object value);

    int save(Object value) throws PersistenceException;
}