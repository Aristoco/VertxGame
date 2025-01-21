package cn.co.a;

import com.aristoco.core.annotation.Value;
import lombok.Data;

/**
 * @author chenguowei
 * @date 2024/6/21
 * @description
 **/
@Data
public class T1234 {

    @Value("${c44}")
    private int c44;

    public T1234() {
    }

    public T1234(Integer a1, Integer a2, Integer a3){

    }
}
