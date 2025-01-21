package cn.co;

import com.aristoco.core.annotation.Component;
import com.aristoco.core.annotation.ConfigurationProperties;
import com.aristoco.core.config.VerticleBaseConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author chenguowei
 * @date 2024/6/25
 * @description
 **/
@EqualsAndHashCode(callSuper = true)
@Component("123")
@ConfigurationProperties("testVerticle")
@Data
public class TestVerticleConfig extends VerticleBaseConfig {
}
