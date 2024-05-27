package com.okane.intergratedapi.testing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    ObjectMapper mapper = new ObjectMapper();

    @Value("${github.webhook.secret}")
    String GITHUB_SECRET;

    @GetMapping("/hello")
    String hello() {
        return "hello webhook!";
    }
//    @PostMapping("/test")
//    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload) {
//        // 여기서 특정 메서드를 실행합니다.
//        runSpecificMethod();
//        return ResponseEntity.ok("Webhook received and processed");
//    }
//
//    public void runSpecificMethod() {
//        // 실행하고자 하는 특정 메서드 로직을 여기에 작성합니다.
//        System.out.println("Specific method executed!");
//    }
    @PostMapping("/test")
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Hub-Signature-256") String signature) {
        if (!isValidSignature(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }
        System.out.println("@@@@@@@@@@@@@@@@@@@@@");
        // GitHub Webhook에서 보내는 페이로드 처리
        JsonElement element = JsonParser.parseString(payload);
        JsonElement repository = element.getAsJsonObject().get("repository").getAsJsonObject();
        String url = repository.getAsJsonObject().get("url").getAsString();
        JsonElement commit = element.getAsJsonObject().get("head_commit").getAsJsonObject();
        JsonElement author = commit.getAsJsonObject().get("author").getAsJsonObject();
        String authorName = author.getAsJsonObject().get("name").getAsString();
        String authorUserName = author.getAsJsonObject().get("username").getAsString();
//        JsonArray added = commit.getAsJsonObject().get("added").getAsJsonArray();
//        String problem = added.get(0).getAsString();
        JsonArray added = commit.getAsJsonObject().get("added").getAsJsonArray();
        if (added.isEmpty()) {
            System.out.println(payload);
            return ResponseEntity.ok("edited");
        } else {
            String problem = added.get(0).getAsString();
            StringBuilder sb = new StringBuilder();
            sb.append("URL : ").append(url).append("\n");
            sb.append("저자이름 : ").append(authorName).append("\n");
            sb.append("닉네임 : ").append(authorUserName).append("\n");
            sb.append("문제 : ").append(problem).append("\n");
            //        System.out.println(payload);
            System.out.println(sb);
            // 원하는 비즈니스 로직을 여기에 추가합니다.
            return ResponseEntity.ok("Success");
        }
    }

    private boolean isValidSignature(String payload, String signature) {
        try {
            String hmac = "sha256=" + hmacSha256(GITHUB_SECRET, payload);
            return hmac.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private String hmacSha256(String secret, String data) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes());
        return Hex.encodeHexString(hmacBytes);
    }
}
