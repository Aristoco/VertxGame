package cn.co;

import com.aristoco.core.annotation.Component;
import com.aristoco.core.event.ApplicationListener;
import com.aristoco.core.event.PayloadApplicationEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * @author chenguowei
 * @date 2024/7/23
 * @description
 **/
@Slf4j
@Component
public class TestEvent implements ApplicationListener<PayloadApplicationEvent<String>> {

    @PostConstruct
    public void init() {
        System.out.println("TestEvent开始初始化");
    }

    /**
     * 执行事件
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(PayloadApplicationEvent<String> event) {
        log.info("thread:" + Thread.currentThread().getName());
        log.info("listener接口接收到事件：" + event.toString());
    }
}
