package com.mall.chatmcp;

import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;


@EnableFeignClients(basePackages="com.mall")
//@SpringBootApplication(exclude = {
//    DataSourceAutoConfiguration.class,      // 排除 Spring 默认数据源
//})
public class MallChatMcpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MallChatMcpServerApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  ai服务模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
            " .-------.       ____     __        \n" +
            " |  _ _   \\      \\   \\   /  /    \n" +
            " | ( ' )  |       \\  _. /  '       \n" +
            " |(_ o _) /        _( )_ .'         \n" +
            " | (_,_).' __  ___(_ o _)'          \n" +
            " |  |\\ \\  |  ||   |(_,_)'         \n" +
            " |  | \\ `'   /|   `-'  /           \n" +
            " |  |  \\    /  \\      /           \n" +
            " ''-'   `'-'    `-..-'              ");
    }
}