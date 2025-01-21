package cn.co;

import com.aristoco.core.annotation.Component;
import com.aristoco.core.config.VerticleBaseConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author chenguowei
 * @date 2024/6/25
 * @description
 **/
@EqualsAndHashCode(callSuper = true)
@Component("tre")
@Data
public class TreeVerticleConfig extends VerticleBaseConfig {
}
