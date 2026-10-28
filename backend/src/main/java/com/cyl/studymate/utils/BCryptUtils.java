package com.cyl.studymate.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


public class BCryptUtils {


    private static final BCryptPasswordEncoder encoder 
            = new BCryptPasswordEncoder();


    /**
     * 密码加密
     */
    public static String encrypt(String password){

        return encoder.encode(password);

    }


    /**
     * 密码校验
     */
    public static boolean matches(
            String password,
            String encryptPassword){

        return encoder.matches(password, encryptPassword);

    }

}