package com.okane.intergratedapi.testing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.okane.intergratedapi.util.CommonUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    ObjectMapper mapper = new ObjectMapper();

    final CommonUtil util;

    @Value("${github.webhook.secret}")
    String GITHUB_SECRET;

    @Value("${notion.database_id}")
    String database_id;

    @Value("${notion.apiKey}")
    String apiKey;

    @GetMapping("/hello")
    String hello() {
        return "hello webhook!";
    }

    /**
     * 오후 4시가 되면 자동으로 작동하는 코드
     */
    @GetMapping("/confirmDaily")
//    @Scheduled(cron = "0 0 16 * * *")
    void confirmDaily() {

        String reqURL = "https://leetcode.com/graphql";
        String body = "{\"query\":\"query questionOfToday {\\n\\tactiveDailyCodingChallengeQuestion {\\n\\t\\tdate\\n\\t\\tuserStatus\\n\\t\\tlink\\n\\t\\tquestion {\\n\\t\\t\\tacRate\\n\\t\\t\\tdifficulty\\n\\t\\t\\tfreqBar\\n\\t\\t\\tfrontendQuestionId: questionFrontendId\\n\\t\\t\\tisFavor\\n\\t\\t\\tpaidOnly: isPaidOnly\\n\\t\\t\\tstatus\\n\\t\\t\\ttitle\\n\\t\\t\\ttitleSlug\\n\\t\\t\\thasVideoSolution\\n\\t\\t\\thasSolution\\n\\t\\t\\ttopicTags {\\n\\t\\t\\t\\tname\\n\\t\\t\\t\\tid\\n\\t\\t\\t\\tslug\\n\\t\\t\\t}\\n\\t\\t}\\n\\t}\\n}\\n\",\"operationName\":\"questionOfToday\"}";

        WebClient webClient = WebClient.builder()
                .baseUrl(reqURL)
                .defaultHeader("Content-Type", "application/json")
                .build();
        String response = webClient.post()
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonElement element = JsonParser.parseString(response);
        JsonElement problem_data = element.getAsJsonObject().get("data").getAsJsonObject().get("activeDailyCodingChallengeQuestion").getAsJsonObject();
        String date = problem_data.getAsJsonObject().get("date").getAsString();
        String link = problem_data.getAsJsonObject().get("link").getAsString();
        String number = problem_data.getAsJsonObject().get("question").getAsJsonObject().get("frontendQuestionId").getAsString();
        String title = problem_data.getAsJsonObject().get("question").getAsJsonObject().get("titleSlug").getAsString();
//        StringBuilder sb = new StringBuilder();
//        sb.append("날짜 : ").append(date).append("\n");
//        sb.append("링크 : ").append("https://leetcode.com" + link).append("\n");
//        sb.append("숫자 : ").append(number + "-" + title).append("\n");
//        System.out.println(sb);
        makeNewLine(number + "-" + title, date, "https://leetcode.com" + link);
    }

    /**
     * 깃허브에 푸시가 되면 자동으로 실행되는 메서드,, 깃허브에 변화가 생기면 노셔닝함수를 호출하여 노션데이터베이스의 정보를 수정한다.
     * @param payload
     * @param signature
     * @return
     */
    @PostMapping("/test")
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Hub-Signature-256") String signature) {
        if (!isValidSignature(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }
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
        String edited = null;
        if (added.isEmpty()) {
            JsonArray modified = commit.getAsJsonObject().get("modified").getAsJsonArray();
            if (modified.isEmpty()) {
                System.out.println("no edit");
                return ResponseEntity.ok("noEdit");
            } else {
                edited = modified.get(0).getAsString();
            }
        } else {
            edited = added.get(0).getAsString();
        }
        String problem_name = edited.split("/")[0];
//        StringBuilder sb = new StringBuilder();
//        sb.append("URL : ").append(url).append("\n");
//        sb.append("저자이름 : ").append(authorName).append("\n");
//        sb.append("닉네임 : ").append(authorUserName).append("\n");
//        sb.append("문제경로 : ").append(edited).append("\n");
//        sb.append("문제이름 : ").append(problem_name).append("\n");
//        sb.append("최종url : ").append(url + "/blob/master/" + edited);
        //        System.out.println(payload);
//        System.out.println(sb);

//        System.out.println(authorName);
        if (edited.endsWith(".md")) {
            System.out.println("md파일 스킵");
            return ResponseEntity.ok("Skip md");
        }
        updateUrl(authorName, url + "/blob/master/" + edited, problem_name);

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

    @GetMapping("/notionGetAll")
    private void notionGetAll() {
        String reqURL = "https://api.notion.com/v1/databases/c95a44c239514d7f92adc66605f07e4d";
        WebClient webClient = WebClient.builder()
                .baseUrl(reqURL)
                .defaultHeader("Authorization", "Bearer secret_ivKjOTQbvbLVmZkqGhA2dokCANUwpuyVPxmuqDcmqYI")
                .defaultHeader("Notion-Version", "2022-06-28")
                .build();
        Mono<String> responseMono = webClient.get()
                .retrieve()
                .bodyToMono(String.class);
        responseMono.subscribe(
                responseBody -> {
                    System.out.println("Response: " + responseBody);
                    // 여기서 responseBody을 원하는 방식으로 처리합니다.
                },
                error -> {
                    System.err.println("Error: " + error.getMessage());
                    // 에러 발생 시 처리합니다.
                }
        );
    }

    @GetMapping("/makeNewLine")
    private String makeNewLine(String problem, String date, String link) {

//        LocalDate today = LocalDate.now();
//        String formattedDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
//        System.out.println("오늘 날짜: " + formattedDate);
        String formattedDate = date;

        String reqURL = "https://api.notion.com/v1/pages";
        String body = "{\"parent\":{\"type\":\"database_id\",\"database_id\":\"" + database_id + "\"},\"properties\":{\"링크\":{\"url\":\"" + link + "\"},\"날짜\":{\"date\":{\"start\":\"" + formattedDate + "\"}},\"문제\":{\"title\":[{\"text\":{\"content\":\"" + problem + "\",\"link\":null}}]},\"이동하\":{\"url\":\" \"},\"박아멘\":{\"url\":\" \"}}}";

        WebClient webClient = WebClient.builder()
                .baseUrl(reqURL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Notion-Version", "2022-06-28")
                .build();
        String response = webClient.post()
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
        JsonElement element = JsonParser.parseString(response);
        String page_id = element.getAsJsonObject().get("id").getAsString();

        return page_id;
    }

    @GetMapping("/query")
    private String notionQuery(String problem) {
        String reqURL = "https://api.notion.com/v1/databases/" + database_id + "/query";

        //모든 열을 다 가져옴
        WebClient webClient = WebClient.builder()
                .baseUrl(reqURL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Notion-Version", "2022-06-28")
                .build();
        String response = webClient.post()
                .retrieve()
                .bodyToMono(String.class)
                .block();

//        System.out.println(response);

        JsonElement element = JsonParser.parseString(response);
        JsonArray pageList = element.getAsJsonObject().get("results").getAsJsonArray();

        int pageListSize = pageList.size();
        for (int i = 0; i < pageListSize; i++) {

            String title = pageList.get(i).getAsJsonObject().get("properties").getAsJsonObject().get("문제").getAsJsonObject().get("title").getAsJsonArray().get(0).getAsJsonObject()
                    .get("text").getAsJsonObject().get("content").toString();
            String id = pageList.get(i).getAsJsonObject().get("id").toString();

            if (title.equals("\"" + problem + "\"")) {
                return id;
            }
        }
        System.out.println("못찾음");
        return null;

        //가져온 열에서 problem과 같은 문제가 있는지 확인
        //있으면 그 열의 id 반환
        //없으면 열을 만들어 id 반환
    }

    @GetMapping("/updateUrl")
    private void updateUrl(String target_name, String target_url, String problem_name) {
//        String target_name = "박아멘";
//        String target_url = "https://github.com/okanekudasai/leethub_notion_connector/blob/master/0001-two-sum/0001-two-sum.java";

        String target_id = notionQuery(problem_name);
        if (target_id == null) {
            System.out.println("해당 문제는 데일리 문제가 아님");
            return;
        }
        if (target_id.charAt(0) == '"') target_id = target_id.substring(1, target_id.length() - 1);
//        System.out.println(target_id);
        String reqURL = "https://api.notion.com/v1/pages/" + target_id;
        String body = "{\"properties\":{\"" + target_name + "\":{\"url\":\"" + target_url + "\"}}}";

        WebClient webClient = WebClient.builder()
                .baseUrl(reqURL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Notion-Version", "2022-06-28")
                .build();
        String response = webClient.patch()
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

    }
}
