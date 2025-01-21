import io.vertx.core.json.JsonObject;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.PropertyHandlerFactory;
import org.mvel2.integration.VariableResolverFactory;

import java.util.HashMap;

/**
 * @author chenguowei
 * @date 2024/7/23
 * @description
 **/
public class Test1 {

    @TestAnnotation1(value = "Foo")
    static class Foo {
    }

    public static void main(String[] args) {
        //TestAnnotation annotation = AnnotationUtil.getAnnotation(Foo.class, TestAnnotation.class);
        ////TestAnnotation testAnnotation = AnnotationUtil.getAnnotationAlias(Foo.class, TestAnnotation.class);
        //TestAnnotation testAnnotation = AnnotationUtil.getSynthesizedAnnotation(Foo.class, TestAnnotation.class);
        //System.out.println(testAnnotation.value());
        //System.out.println(testAnnotation.name());
        //TestAnnotation testAnnotation1 = AnnotationUtil.getSynthesizedAnnotation(TestAnnotation.class, testAnnotation);
        //System.out.println(testAnnotation1.value());
        //System.out.println(testAnnotation1.name());
        //SimpleVariableResolverFactory1 simpleVariableResolverFactory = new SimpleVariableResolverFactory1(new HashMap<>());
        //StopWatch stopWatch = StopWatch.create("MVEL测试");
        //ParserContext parserContext = new ParserContext();
        //stopWatch.start("不预编译表达式");
        //for (int i = 0; i < 10; i++) {
        //    T123 t1234 = new T123(Set.of(),new D());
        //    t1234.setC44(i);
        //    t1234.setC3("asdsadasdasdsdasd");
        //    t1234.setC2("111asdsadasdasdsdasd");
        //    t1234.setTestController(new TestController());
        //    t1234.setTestVerticleConfig(new TestVerticleConfig());
        //    TestVerticleConfig testVerticleConfig = new TestVerticleConfig();
        //    testVerticleConfig.getDeploymentOptions().setInstances(10);
        //    t1234.setTestVerticleConfig1(testVerticleConfig);
        //    Boolean eval = MVEL.eval("t123.c44 % 2 == 0 && t123.testVerticleConfig1.deploymentOptions.instances >= 9", parserContext, simpleVariableResolverFactory, Boolean.class);
        //    //System.out.println("1==> " + eval);
        //}
        //stopWatch.stop();
        //
        //stopWatch.start("预编译表达式");
        //Serializable expression = MVEL.compileExpression("t123.c44 % 2 == 0 && t123.testVerticleConfig1.deploymentOptions.instances >= 9");
        //for (int i = 0; i < 10; i++) {
        //    T123 t1234 = new T123(Set.of(),new D());
        //    t1234.setC44(i);
        //    t1234.setC3("asdsadasdasdsdasd");
        //    t1234.setC2("111asdsadasdasdsdasd");
        //    t1234.setTestController(new TestController());
        //    t1234.setTestVerticleConfig(new TestVerticleConfig());
        //    TestVerticleConfig testVerticleConfig = new TestVerticleConfig();
        //    testVerticleConfig.getDeploymentOptions().setInstances(10);
        //    t1234.setTestVerticleConfig1(testVerticleConfig);
        //    Boolean eval = MVEL.executeExpression(expression, parserContext, Map.of("t123", t1234), Boolean.class);
        //    //System.out.println("2==> " + eval);
        //}
        //stopWatch.stop();
        //
        //System.out.println(stopWatch.prettyPrint(TimeUnit.MILLISECONDS));
        ParserContext parserContext = new ParserContext();
        PropertyHandlerFactory.registerPropertyHandler(JsonObject.class,new JsonObjectPa());
        HashMap<String, Object> map = new HashMap<>();
        JsonObject jsonObject = JsonObject.of("b", JsonObject.of("c", 1));
        map.put("a", jsonObject);
        map.put("user", null);
        // 可以在这里添加一些配置项，或者保持为空

        // 使用MVEL的eval函数和空合并运算符
        Object defaultValue = MVEL.eval("a.b.c", map);
        System.out.println(defaultValue);
    }

    public static class User {
        String a;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }
    }

    public static class JsonObjectPa implements PropertyHandler {

        @Override
        public Object getProperty(String s, Object o, VariableResolverFactory variableResolverFactory) {
            JsonObject jsonObject = (JsonObject) o;
            return jsonObject.getValue(s);
        }

        @Override
        public Object setProperty(String s, Object o, VariableResolverFactory variableResolverFactory, Object o1) {
            JsonObject jsonObject = (JsonObject) o;
            return jsonObject.put(s, o1);
        }
    }
}
