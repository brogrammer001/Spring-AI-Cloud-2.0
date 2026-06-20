package com.mall.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.mall.common.security.annotation.EnableCustomConfig;
import com.mall.common.security.annotation.EnableRyFeignClients;

/**
 * 系统模块
 * 
 * @author mall
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class MallSystemApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(MallSystemApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  系统模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
            "                                         MMMM\n" +
            "                                      MMMMMMMMMM\n" +
            "                                    MMMMMMMMMMMMMM\n" +
            "        MMMM                        MMMMMMMMMMMMMMM\n" +
            "     MMMMMMMMMM                    MMMMMMMMMMMMMMMM\n" +
            "    MMMMMMMMMMMM                  MMMMMMMMMMMMMMMMMM\n" +
            "  MMMMMMMMMMMMMMM                 MMMMMMMMMMMMMMMMMM\n" +
            " MMMMMMMMMMMMMMMMM                MMMMMMMMMMMMMMMMMM\n" +
            " MMMMMMMMMMMMMMMMM                MMMMMMMMMMMMMMMMMM\n" +
            "MMMMMMMMMMMMMMMMMMM               MMMMMMMMMMMMMMMMMM\n" +
            "MMMMMMMMMMMMMMMMMMM               MMMMMMMMMMMMMMMMMM\n" +
            "MMMMMMMMMMMMMMMMMMM                MMMMMMMMMMMMMMMMM\n" +
            "MMMMMMMMMMMMMMMMMMM     MMMMMMM    MMMMMMMMMMMMMMMMM\n" +
            "MMMMMMMMMMMMMMMMMMM   MMMMMMMMMMMMM MMMMMMMMMMMMMMM\n" +
            "MMMMMMMMMMMMMMMMMMM  MMMMMMMMM----MMMMMMMMMMMMMMMM\n" +
            "MMMMMMMMMMMMMMMMMM MMMMMMMMMM------MMMMMMMMMMMMMM\n" +
            "MMMMMMMMMMMMMMMMMMMMM----MMM---/=\\--MMMMMMMMMMMM\n" +
            " MMMMMMMMMMMMMMMMMMM--==--MM------\\--MMM MMMMMM\n" +
            " MMMMMMMMMMMMMMMMMM--/-----M-------\\--MMM\n" +
            "  MMMMMMMMMMMMMMMM--/-----------------MMM\n" +
            "    MMMMMMMMM  MMM--|------------------MMM\n" +
            "      MMMMM   MMM------/..\\---/..\\-----MMM\n" +
            "              MMM-----/....\\-/....\\----MMMM\n" +
            "             MMMM-----......-......----MMMM\n" +
            "             MMMM-----......-......----MMMM\n" +
            "             MMMM-----......-.MM...---MMMMM\n" +
            "             MMMMM----...MM.-.MMM..---MMMMM\n" +
            "             MMMMM----..MMM.-\\MMM./---MMMMM\n" +
            "             MMMMMM---\\.MMM.--\\M./---/-----\n" +
            "             MMMMMM----\\\\M/=======\\---------\n" +
            "             MM----M--/====-MMMMM------------\n" +
            "               -----------MMMMMMMM-----------\n" +
            "              ------------MMMMMMMM-----------\n" +
            "              ------------MMMMMMMM----X------\n" +
            "              ------_/----MMMMMMM-----/-\\----\n" +
            "              -----/\\------MMMMM------|-----\n" +
            "              ----/--\\---------------/-----\n" +
            "               -------\\-------------M-----\n" +
            "               --------=-----------MM----\n" +
            "                 -------\\=========MMM---\n" +
            "                  ------MMMMMMMMMMMM---\n" +
            "                    -----MXXXXXXXXM---\n" +
            "                   MMMMM--XXXXXXXX--\n" +
            "                MMMMMMMMM---XXXX---M\n" +
            "           MMMMMMMMMMMMMMM--------MMM\n" +
            "          MMMMMM:++MMMMMMMMM----MMMMM\n" +
            "         MMMMMM:MMMMMMMMMMMMMMMMMMMMMM\n" +
            "         MMMMM:M////MMMMMMMMMMMMMMMMMM\n" +
            "         MMMMM//////////MMMMMMMMMMMMMMM\n" +
            "         MMMM/////////////MMMMMMMMMMMMM\n" +
            "         MMM///////////////MMMMMMMMMMMM\n" +
            "          M///...////////////MMMMMMMMMMM\n" +
            "          ///....////..///////MMMMMMMMMM\n" +
            "          ///....///....///////MM  MMMMMM\n" +
            "          ///....///....////////M  MMMMMM\n" +
            "         ////...///.....///////// MMMMMMMM\n" +
            "         ////..////...../////////MMMMMMMMM\n" +
            "         //////////...../////////MMMMMMMMM\n" +
            "         //////////....//////////MMMMMMMM\n" +
            "         ///////////..///////////MMMMMMM\n" +
            "         ////////////////////////MMMMM\n" +
            "         ////////////////////////MMM\n" +
            "          ///////////////////////\n" +
            "          ///////////////////////\n" +
            "          ///////////////////////\n" +
            "          ///////////////////////\n" +
            "         ////////////////////////\n" +
            "         ////////////////////////\n" +
            "         //////X//////////////////\n" +
            "          //////XXX///////////////\n" +
            "            ///////V//////////////\n" +
            "               /////X//////////:::\n" +
            "                MMMM/////MMMM::MM\n" +
            "                 MMMMM|MMMMMMM   MMM\n" +
            "                 MMMMMM|MMMMMM      MMMM____\n" +
            "                  MMMMM|MMMMMM              \\------\n" +
            "                   MMMM\\MMMMMM                     -_\n" +
            "                    MMMM|MMMMMM                      \\\n" +
            "                    MMMM\\MMMMMM                       \\\n" +
            "                     MMMM\\MMMMM                        |\n" +
            "                      MMM|MMMMMM                      /\n" +
            "       VVVVVVV      VVMMMM\\MMMMM                     /\n" +
            "    VVVVVVVVVXVVVV  VVVMMMM|MMMM                   _-\n" +
            "    VVVVVVVVVVXVVVVVIVVVMMIIMMMMVV            -----\n" +
            "   VVVVVVVVVVVVVXXVVVIVVVVIIMMMMMVV\n" +
            "   VVVVVVVVVVVVVVVXXVVVVVVVIIVMMMVVVVVVV\n" +
            "   VVVVVVVVVVVVVVVVXVXVVVVVIIVVVVVVVVVVVVVVVV\n" +
            "   VVVVVVVVVVVVVVVVVXVXVVVVIVIVVVVVVVVVVVVXVVVVVV\n" +
            "   VVVVVVVVVVVVVVVVVVXVXVVIVVVIVVVVVVVVVXVVXVVVVVVV\n" +
            "    VVVVVVVVVVVVVVVVVVXVXVIVVVVVVVVVVVVXVVXVVVVVVVVV\n" +
            "    VVVVVVVVVVVVVVVVVVVXVVIVVVVVVVVVVVXVXVVVVVVVVVVVV\n" +
            "     VVVVVVVVVVVVVVVVVVVVVVIVVVVVVVVVXVXVVVVVVVVVVVVV\n" +
            "      VVVVVVVVVVVVVVVVVVVVVIVVVVVVVVXVXVVVVVVVVVVVVVV\n" +
            "        VVVVVVVVVVVVVVVVVV  VVVVVVVXXVVVVVVVVVVVVVVVV\n" +
            "          VVVVVVVVVVVVVV     VVVXXXVVVVVVVVVVVVVVVVVV\n" +
            "             VVVVVVVVV        VXVVVVVVVVVVVVVVVVVVVV\n" +
            "                                VVVVVVVVVVVVVVVVVVV\n" +
            "                                 VVVVVVVVVVVVVVVVV\n" +
            "                                  VVVVVVVVVVVVVV\n" +
            "                                    VVVVVVVVV");
    }
}
