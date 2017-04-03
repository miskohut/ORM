package metaorm.analyzer;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import metaorm.exceptions.PersistenceException;
import metaorm.persistencemanager.PersistenceManagerImpl;

import javax.persistence.Id;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by miskohut on 3.4.2017.
 */
public class SQLManager {

    private ClassAnalyzer classAnalyzer;
    private PersistenceManagerImpl persistenceManager;

    public SQLManager(ClassAnalyzer classAnalyzer, PersistenceManagerImpl persistenceManager) {
        this.classAnalyzer = classAnalyzer;
        this.persistenceManager = persistenceManager;
    }

    public PreparedStatement save(Object object) throws IllegalAccessException, PersistenceException, SQLException {
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
                ClassAnalyzer classAnalyzer = new ClassAnalyzer(ref.getClass());
                Map.Entry<Field, ClassAnalyzer.FieldData> id = Iterables.find(classAnalyzer.getColumns().entrySet(), new Predicate<Map.Entry<Field, ClassAnalyzer.FieldData>>() {
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
}
