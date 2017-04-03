package annotationprocessor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.persistence.Entity;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


@AutoService(Processor.class)
public class EntityProcessor extends AbstractProcessor {

    private Types types;
    private Elements elements;
    private Filer filer;
    private Messager messager;
    private Map<String, ClassAnalyzerHolder> classAnalyzerMap = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        types = processingEnv.getTypeUtils();
        elements = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new HashSet<>();
        annotations.add(Entity.class.getCanonicalName());

        return annotations;
    }

    @Override
    public Set<String> getSupportedOptions() {
        return super.getSupportedOptions();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        //get all entities
        for (Element element : roundEnv.getElementsAnnotatedWith(Entity.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                try {

                    classAnalyzerMap.put(element.getSimpleName().toString(), new ClassAnalyzerHolder(element));

                } catch (ProcessingException e) {
                    e.printStackTrace();

                    messager.printMessage(Diagnostic.Kind.WARNING, e.getMessage());
                }
            }
        }

        for (Map.Entry<String, ClassAnalyzerHolder> holderEntry : classAnalyzerMap.entrySet()) {
            holderEntry.getValue().setClassAnalyzer(new ClassAnalyzer(holderEntry.getValue().getElement(), this));
        }

        try {

           /* ClassGenerator classGenerator = new ClassGenerator(this);

            FileWriter fileWriter = new FileWriter("PersistenceManagerImpl.java");
            JavaFile javaFile = JavaFile.builder("orm.analyzer.orm.persistance", classGenerator.getCl()).build();

            javaFile.writeTo(System.out);*/

            FileWriter fileWriter = new FileWriter("script.sql");
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            for (ClassAnalyzerHolder holder : classAnalyzerMap.values()) {
                bufferedWriter.append(holder.getClassAnalyzer().createTable());
                bufferedWriter.newLine();
            }

            bufferedWriter.newLine();

            for (ClassAnalyzerHolder holder : classAnalyzerMap.values()) {
                for (String query : holder.getClassAnalyzer().createReferences()) {
                    bufferedWriter.append(query);
                    bufferedWriter.newLine();
                }
            }

            bufferedWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    public Map<String, ClassAnalyzerHolder> getClassAnalyzerMap() {
        return classAnalyzerMap;
    }
}
