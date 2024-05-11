package com.okane.intergratedapi.testing.controller;

import com.okane.intergratedapi.testing.UserRepository;
import com.okane.intergratedapi.testing.dto.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    final UserRepository userRepository;

    @PostConstruct
    void init() {
        userRepository.save(User.builder().name("eeeee").email("gg@gg.cc").build());
        userRepository.save(User.builder().name("aefsaef").email("gg@geeg.cc").build());
        userRepository.save(User.builder().name("easfasae").email("gg@gggg.cc").build());
        userRepository.save(User.builder().name("eesefseafee").email("ggww@gg.cc").build());
    }

    @GetMapping("/hello")
    String getTest() {
        return "hello intergrated!!!!";
    }

    @PostMapping("/saveDummy")
    String saveDummy(@RequestParam("name") String name, @RequestParam("email") String email) {

        userRepository.save(User.builder().name(name).email(email).build());
        return "success";
    }

    @GetMapping("/user")
    String getUser() {
        List<User> list = userRepository.findAll();
        StringBuilder sb = new StringBuilder();
        for (User u : list) {
            sb.append(u.getName() + " " + u.getEmail() + "\n");
        }
        return sb.toString();
    }

    @GetMapping("/true")
    boolean getTrue() {
        return true;
    }

    @GetMapping("/false")
    boolean getFalse() {
        return false;
    }

}
