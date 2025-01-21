package cn.co.a;

import cn.co.TestVerticleConfig;
import cn.co.TreeVerticleConfig;
import com.aristoco.core.annotation.Component;
import com.aristoco.core.annotation.Value;
import com.aristoco.core.config.VerticleBaseConfig;
import com.google.inject.name.Named;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

/**
 * @author chenguowei
 * @date 2024/6/21
 * @description
 **/
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Component
@Data
public class T123 extends T1234 {

    @Inject
    @Named("a")
    I ia;

    @Value("${a:10}")
    Integer c1;

    @Value("${b}")
    String c2;

    @Value("${c.d:oooo}")
    String c3;

    @Value("${e.f.g.y}")
    Integer c4;

    @Inject
    TestController testController;

    @Inject
    TestVerticleConfig testVerticleConfig;

    @Inject
    @Named("123")
    TestVerticleConfig testVerticleConfig1;

    @Inject
    Map<String, VerticleBaseConfig> verticleBaseConfigMap;

    @Inject
    TreeVerticleConfig treeVerticleConfig;

    @Inject
    @Named("tre")
    TreeVerticleConfig treeVerticleConfig1;

    final Set<I> is;

    final D d;

    @Inject
    @Named("d1")
    D d1;

    @Inject
    public T123(Set<I> is, D d) {
        this.is = is;
        this.d = d;
        log.info("加载了");
    }

    public void asd() {
        System.out.println(is.toString());
        System.out.println(d.toString());
        System.out.println(d1.toString());
        System.out.println(ia.toString());
        System.out.println(c1);
        System.out.println(c2);
        System.out.println(c3);
        System.out.println(c4);
        System.out.println("c44: " + getC44());
        System.out.println(testController);
        System.out.println(testVerticleConfig);
        System.out.println(testVerticleConfig1);
        System.out.println(treeVerticleConfig);
        System.out.println(treeVerticleConfig1);
        //System.out.println(verticleBaseConfigMap);
        //verticleBaseConfigMap.values().forEach(verticleBaseConfig -> System.out.println(verticleBaseConfig.getClass().getName() + "  " + verticleBaseConfig.getDeploymentOptions().getInstances()));
    }

    @PostConstruct
    public void init(){
        System.out.println("T123开始初始化");
        System.out.println("c1 "+getC1());
        System.out.println("c44 "+getC44());
    }

}
