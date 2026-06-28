package com.mall.aichat;

import com.mall.common.security.annotation.EnableCustomConfig;
import com.mall.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@EnableRyFeignClients
@EnableCustomConfig
public class MallAiChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(MallAiChatApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  ai服务模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
            "                       .   *        .       .\n" +
            "       *      -0-\n" +
            "          .                .  *       - )-\n" +
            "       .      *       o       .       *\n" +
            " o                |\n" +
            "           .     -O-\n" +
            ".                 |        *      .     -0-\n" +
            "       *  o     .    '       *      .        o\n" +
            "              .         .        |      *\n" +
            "   *             *              -O-          .\n" +
            "         .             *         |     ,\n" +
            "                .           o\n" +
            "        .---.\n" +
            "  =   _/__~0_\\_     .  *            o       '\n" +
            " = = (_________)             .\n" +
            "                 .                        *\n" +
            "       *               - ) -       *");
    }

}