package annotationprocessor;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.persistence.Id;
import java.util.Map;

/**
 * Created by miskohut on 29.3.2017.
 */
public class ClassAnalyzerHolder {

    private Element element;
    private ClassAnalyzer classAnalyzer;

    public ClassAnalyzerHolder(Element element) throws ProcessingException {
        this.element = element;

        //check if field annotated with Id annotation is present
        if (Iterables.find(element.getEnclosedElements(), new Predicate<Element>() {
            @Override
            public boolean apply(Element element) {
                return element.getKind() == ElementKind.FIELD && element.getAnnotation(Id.class) != null;
            }
        }, null) == null) {
            throw new ProcessingException("Class " + this.element.getSimpleName().toString() + " has no Id field");
        }
    }

    public void setClassAnalyzer(ClassAnalyzer classAnalyzer) {
        this.classAnalyzer = classAnalyzer;
    }

    public Element getElement() {
        return element;
    }

    public ClassAnalyzer getClassAnalyzer() {
        return classAnalyzer;
    }
}
