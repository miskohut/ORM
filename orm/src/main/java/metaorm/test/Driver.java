package metaorm.test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by miskohut on 3.4.2017.
 */
@Entity
public class Driver {

    @Id
    private int id;

    @Column
    private String name;

    public Driver(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
