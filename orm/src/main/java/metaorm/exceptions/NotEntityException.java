package metaorm.exceptions;

/**
 * Created by miskohut on 3.4.2017.
 */
public class NotEntityException extends PersistenceException {

    public NotEntityException(String message) {
        super(message);
    }
}
