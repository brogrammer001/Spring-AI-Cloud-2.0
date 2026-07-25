package com.mall.chatmcp;

import com.mall.common.security.annotation.EnableRyFeignClients;
import com.mall.common.security.feign.FeignAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Import;


@EnableRyFeignClients
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
})
@Import({ FeignAutoConfiguration.class })
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