package metaorm.analyzer;


import metaorm.persistencemanager.PersistenceManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ORMInvocationHandler implements InvocationHandler {

    private final PersistenceManager manager;
    private final int id;
    private final Class aClass;
    private final Object object;
    private final Field field;

    public ORMInvocationHandler(PersistenceManager manager, int id, Class aClass, Object object, Field field){
        this.manager = manager;
        this.id = id;
        this.aClass = aClass;
        this.object = object;
        this.field = field;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        o = manager.get(aClass, id);
        field.set(object, o);
        Object returnObject = method.invoke(o, objects);
        return returnObject;
    }
}