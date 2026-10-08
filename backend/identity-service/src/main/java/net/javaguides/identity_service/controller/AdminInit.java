package net.javaguides.identity_service.controller;

import lombok.NoArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AdminInit {
 //   private  final PasswordEncoder passwordEncoder;

//    public AdminInit() {
//    }


//    public AdminInit(PasswordEncoder passwordEncoder) {
//        this.passwordEncoder = passwordEncoder;
//    }


    public static void main(String []args) {
        AdminInit adminTest = new AdminInit();
        PasswordEncoder passwordEncode = new BCryptPasswordEncoder();
        System.out.print("password: " +   passwordEncode.encode("123456") );
    }



}
