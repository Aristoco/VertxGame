import com.aristoco.core.jackson.JsonObjectDeserializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author chenguowei
 * @date 2024/6/17
 * @description
 **/
public class Test {

    static {
        configureJson();
    }

    public static void configureJson() {
        var module = new SimpleModule();
        var pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(pattern));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(pattern));
        module.addDeserializer(JsonObject.class, new JsonObjectDeserializer());
        DatabindCodec.mapper().registerModule(module);
        DatabindCodec.mapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        DatabindCodec.mapper().configure(SerializationFeature.WRITE_ENUMS_USING_INDEX, true);
        DatabindCodec.mapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static void main(String[] args) {
        //Component component = AnnotationUtil.getAnnotationAlias(TestModule.class, Component.class);
        //Object value = AnnotationUtil.getAnnotationValue(TestModule.class, Configuration.class);
        //System.out.println(value);
        //Object value1 = AnnotationUtil.getAnnotationValue(TestModule.class, Component.class);
        //System.out.println(value1);
        //boolean b = AnnotationUtil.hasAnnotation(TestModule.class, Component.class);
        //System.out.println(b);

        ////扫描需要的类
        //ScanResult scanResult = new ClassGraph()
        //        .enableAllInfo()
        //        .verbose()
        //        .whitelistPackages(Main.class.getPackageName())
        //        .scan();
        //
        //for (ClassInfo routeClassInfo : scanResult.getClassesWithAnnotation(Component.class.getName()).filter(classInfo -> !classInfo.isAnnotation())) {
        //    Class<?> clazz = routeClassInfo.loadClass();
        //    System.out.println("simpleName:"+routeClassInfo.getSimpleName().toLowerCase(Locale.US));
        //    Component component1 = AnnotationUtil.getSynthesizedAnnotation(clazz, Component.class);
        //    System.out.println(component1.value());
        //    AnnotationInfo routeAnnotationInfo = routeClassInfo.getAnnotationInfo(Component.class.getName());
        //    List<AnnotationParameterValue> routeParamVals = routeAnnotationInfo.getParameterValues();
        //    String route = (String) routeParamVals.get(0).getValue();
        //    System.out.println(routeClassInfo.getName() + " is annotated with route " + route);
        //}
        //String config = ResourceUtil.readStr("application1.yml", StandardCharsets.UTF_8);
        //Yaml yaml = new Yaml();
        //Object jsonObject = yaml.loadAs(config, Object.class);
        //JsonObject innerJson = JsonObject.mapFrom(jsonObject);
        //System.out.println(innerJson);
        //Vertx vertx = Vertx.vertx();
        //ConfigStoreOptions storeOptions = new ConfigStoreOptions()
        //        .setType("file")
        //        .setFormat("yaml")
        //        .setConfig(new JsonObject()
        //                .put("path", "application.yml"));
        //ConfigRetrieverOptions options = new ConfigRetrieverOptions()
        //        .addStore(storeOptions);
        //ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
        //// 获取配置
        //retriever.getConfig(json -> {
        //    JsonObject result = json.result();
        //    System.out.println(result);
        //    //JsonObject jsonObject1 = innerJson.mergeIn(result,true);
        //    //System.out.println(jsonObject1);
        //    // 关闭vertx对象，我们再也用不到它了
        //    vertx.close();
        //});
        //
        //VertxOptionsConverter vertxOptionsConverter = new VertxOptionsConverter();

        String userDir = System.getProperty("user.dir");
        System.out.println(userDir);
    }

}
