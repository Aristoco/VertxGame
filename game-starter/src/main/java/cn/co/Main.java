package cn.co;

import ch.qos.logback.classic.LoggerContext;
import com.aristoco.core.GameApplication;
import com.aristoco.core.annotation.VertxGameApplication;
import org.slf4j.LoggerFactory;

/**
 * @author chenguowei
 * @date 2024/6/14
 * @description
 **/
@VertxGameApplication
public class Main {

    public static void main(String[] args) {
        GameApplication application = new GameApplication(Main.class, new TestModule());
        application.setCustomAfterStartingVertx(vertx -> {
            vertx.setTimer(10000L,tid->{
                //GameApplicationContextFactory.GAME_APPLICATION_CONTEXT_MAP
                //        .values().forEach(context->{
                //            if (context instanceof BootstrapApplicationContext) {
                //                return;
                //            }
                //            System.out.println("========================");
                //            Injector injector = context.getInjector();
                //            T123 instance = injector.getInstance(T123.class);
                //            instance.asd();
                //        });
                //injector.getBindings().forEach((k,b)->{
                //    System.out.println("------  " +k.toString()+"   "+b.toString());
                //});

            });
        });
        //关闭logback
        application.setVertxAfterStopHandle(()->{
            System.out.println("vertx 停止");
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.stop();

            // 可选：打印状态信息，有助于诊断问题
            //StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        });

        application.run(args);

        System.out.println("Hello world!");
    }

}
