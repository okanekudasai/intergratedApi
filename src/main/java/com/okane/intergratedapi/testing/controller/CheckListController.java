package com.okane.intergratedapi.testing.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

@RestController
@RequestMapping("/checklist")
@RequiredArgsConstructor
@EnableScheduling
public class CheckListController {
    @Value("${checklist.api_key}")
    String apiKey;
    @Value("${checklist.database_id}")
    String checklistDatabaseId;

    String Notion_Version = "2022-06-28";
    String reqURL = "https://api.notion.com/v1/pages";

    @GetMapping("/addDate")
    @Scheduled(cron = "0 0 0 * * *")
    void addDate() {
        LocalDate td = LocalDate.now();
        String formattedDate = td.toString();

        String body = "{\"parent\":{\"database_id\": \"" + checklistDatabaseId + "\"},\"properties\":{\"날짜\":{\"date\":{\"start\": \"" + formattedDate + "\",\"end\":null}},\"군것질 안함\":{\"checkbox\":true}}}";

        System.out.println(body);

        WebClient webClient = WebClient.builder()
                .baseUrl(reqURL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Notion-Version", Notion_Version)
                .build();

        String response = webClient.post()
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        JsonElement element = JsonParser.parseString(response);
        String page_id = element.getAsJsonObject().get("id").getAsString();

        System.out.println(page_id);//
    }
    @GetMapping("all")
    String getAllData() {
        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.notion.com/v1/databases/" + checklistDatabaseId + "/query")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Notion-Version", Notion_Version)
                .build();
        String response = webClient.post()
                .bodyValue("{\"sorts\":[{\"property\":\"날짜\",\"direction\":\"descending\"}]}")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        JsonElement element = JsonParser.parseString(response);
        JsonArray data = element.getAsJsonObject().get("results").getAsJsonArray();
        return data.toString();
    }
}
