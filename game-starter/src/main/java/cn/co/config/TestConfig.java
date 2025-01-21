package cn.co.config;

import cn.co.a.A;
import cn.co.a.D;
import com.aristoco.core.annotation.Bean;
import com.aristoco.core.annotation.Configuration;

/**
 * @author chenguowei
 * @date 2024/6/21
 * @description
 **/
@Configuration
public class TestConfig {

    @Bean("AAAAAAAAAAA")
    public A a1() {
        return new A();
    }

    @Bean("d1")
    public D d(){
        D d = new D();
        d.setIndex(-1);
        return d;
    }
}
