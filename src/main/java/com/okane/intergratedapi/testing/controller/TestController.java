package com.okane.intergratedapi.testing.controller;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

//    final UserRepository userRepository;

    @GetMapping("/hello")
    String getTest() {
        return "hello intergrated!!!!";
    }

//    @PostMapping("/saveDummy")
//    String saveDummy(@RequestParam("name") String name, @RequestParam("email") String email) {
//
//        userRepository.save(User.builder().name(name).email(email).build());
//        return "success";
//    }

//    @GetMapping("/user")
//    String getUser() {
//        List<User> list = userRepository.findAll();
//        StringBuilder sb = new StringBuilder();
//        for (User u : list) {
//            sb.append(u.getName() + " " + u.getEmail() + "\n");
//        }
//        return sb.toString();
//    }

    @GetMapping("/true")
    boolean getTrue() throws InterruptedException {
        Thread.sleep(2000);
        return true;
    }

    @GetMapping("/false")
    boolean getFalse() throws InterruptedException {
        Thread.sleep(2000);
        return false;
    }

}
