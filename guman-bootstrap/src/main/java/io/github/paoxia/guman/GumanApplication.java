package io.github.paoxia.guman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Guman 服务的 Spring Boot 启动与模块装配入口。 */
@SpringBootApplication
public class GumanApplication {

    /**
     * 启动 Spring Boot 应用，无返回值。
     *
     * @param args JVM 传入的命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GumanApplication.class, args);
    }
}
