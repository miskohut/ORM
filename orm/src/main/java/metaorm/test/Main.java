package metaorm.test;

import metaorm.exceptions.PersistenceException;
import metaorm.persistencemanager.PersistenceManagerImpl;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Created by miskohut on 3.4.2017.
 */
public class Main {

    public static void main(String[] args) {
        try {
            PersistenceManagerImpl persistenceManager = new PersistenceManagerImpl(getConnection());

            persistenceManager.save(new Auto(1, "Nissan", new Driver(1, "Miško")));
        } catch (PersistenceException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        Connection c = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:test.db");
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Opened database successfully");

        return c;
    }
}
