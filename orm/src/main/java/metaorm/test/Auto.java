package metaorm.test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by miskohut on 3.4.2017.
 */
@Entity(name = "Car")
public class Auto {

    @Id
    private int id;

    @Column
    private String name;

    @Column
    private Driver driver;

    public Auto(int id, String name, Driver driver) {
        this.id = id;
        this.name = name;
        this.driver = driver;
    }
}
