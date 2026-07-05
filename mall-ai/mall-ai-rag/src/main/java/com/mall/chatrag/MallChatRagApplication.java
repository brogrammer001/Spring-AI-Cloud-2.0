package com.mall.chatrag;

import com.mall.common.security.annotation.EnableCustomConfig;
import com.mall.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRyFeignClients
@EnableCustomConfig
public class MallChatRagApplication {
    public static void main(String[] args) {
        SpringApplication.run(MallChatRagApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  知识库服务模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
            "   |^^^|\n" +
            "    }_{\n" +
            "    }_{\n" +
            "/|_/---\\_|\\\n" +
            "I _|\\_/|_ I\n" +
            "\\| |   | |/\n" +
            "   |   |\n" +
            "   |   |\n" +
            "   |   |\n" +
            "   |   |\n" +
            "   |   |\n" +
            "   |   |\n" +
            "   |   |\n" +
            "   |   |\n" +
            "   |   |\n" +
            "   |   |\n" +
            "   |   |\n" +
            "   |   |\n" +
            "   \\   /\n" +
            "    \\ /\n" +
            "     Y");
    }
}
