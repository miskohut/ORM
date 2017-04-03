package metaorm.persistencemanager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by miskohut on 3.4.2017.
 */
public class FileReader {

    public static List<String> getQueries() throws IOException {

        java.io.FileReader fileReader = new java.io.FileReader("script.sql");
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        List<String> queries = new ArrayList<>();

        String line = "";
        while ((line = bufferedReader.readLine()) != null) {
            if (!line.equals("")) {
                queries.add(line);
            }
        }

        return queries;
    }
}
