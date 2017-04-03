package annotationprocessor;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;


public class ClassGenerator {

    private EntityProcessor entityProcessor;
    private TypeSpec cl;

    public ClassGenerator(EntityProcessor entityProcessor) {
        this.entityProcessor = entityProcessor;

        generateClass();
    }

    public void generateClass() {

        cl = TypeSpec.classBuilder("PersistenceManagerImplTest")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(initializeDatabase()).build();


    }

    public TypeSpec getCl() {
        return cl;
    }

    private MethodSpec initializeDatabase() {
        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("initializeDatabase")
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addCode(
                        "Statement statement = null;\n\n" +
                        "try {\n" +
                        "statement = connection.createStatement();\n" +
                        "} catch (SQLException e) {\n" +
                        "throw new PersistenceException(\"Error while creating statement\");\n" +
                        "}\n\n" +
                        "try {\n\n"
                );

        //add statements for create table
        for (ClassAnalyzerHolder holder : entityProcessor.getClassAnalyzerMap().values()) {
            methodSpecBuilder.addStatement("statement.execute(\"" + holder.getClassAnalyzer().createTable() + "\");");
        }

        //add statements for creating references
        for (ClassAnalyzerHolder holder : entityProcessor.getClassAnalyzerMap().values()) {
            for (String query : holder.getClassAnalyzer().createReferences()) {
                methodSpecBuilder.addStatement("statement.execute(\"" + query + "\");");
            }
        }

        methodSpecBuilder.addCode(
                "\n\n} catch (SQLException e) {\n" +
                "e.printStackTrace()\n" +
                "}\n"
        ).build();

        return methodSpecBuilder.build();

    }
}
