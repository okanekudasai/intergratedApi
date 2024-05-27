package com.okane.intergratedapi.testing.controller;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    @Value("${github.webhook.secret}")
    String GITHUB_SECRET;

    @GetMapping("/hello")
    String hello() {
        return "hello webhook!";
    }
    @PostMapping("/test")
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Hub-Signature-256") String signature) {
        if (!isValidSignature(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }
        // GitHub Webhook에서 보내는 페이로드 처리
        System.out.println("Received payload: " + payload);
        // 원하는 비즈니스 로직을 여기에 추가합니다.
        return ResponseEntity.ok("Success");
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
