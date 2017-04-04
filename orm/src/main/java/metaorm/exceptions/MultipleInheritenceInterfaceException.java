package metaorm.exceptions;

/**
 * Created by miskohut on 4.4.2017.
 */
public class MultipleInheritenceInterfaceException extends PersistenceException{

    public MultipleInheritenceInterfaceException() {
        super("More or no classes extends from interface");
    }
}
