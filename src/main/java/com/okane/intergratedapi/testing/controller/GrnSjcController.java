package com.okane.intergratedapi.testing.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.Local;
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
import java.util.Date;
import java.util.Locale;

@RestController
@RequestMapping("/GrnSjc")
@RequiredArgsConstructor
@EnableScheduling
public class GrnSjcController {
    @Value("${grnsjc.daily_scrum_id}")
    String daily_scrum_id;

    @Value("${grnsjc.api_key}")
    String api_key;

    String Notion_Version = "2022-06-28";

    @GetMapping("/add")
    @Scheduled(cron = "0 30 9 * * 1-5")
    void add_line() {
        LocalDate td = LocalDate.now();
        LocalDate overDate = LocalDate.of(2024, 12, 1);
        if (td.isAfter(overDate)) return;

        String reqURL = "https://api.notion.com/v1/pages";

        Date today = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd(E)", Locale.KOREAN);
        String formattedDate = dateFormat.format(today);
        LocalDate referenceDate = LocalDate.of(2024, 6, 3);
        long weeksBetween = ChronoUnit.WEEKS.between(referenceDate, td) + 1;
        String week_num = weeksBetween + "주차";
        DayOfWeek dayOfWeek = td.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.FRIDAY) {
            for (int i = 0; i < 2; i++) {
                String body = "";
                if (i == 0) body = "{\"parent\":{\"database_id\":\"" + daily_scrum_id + "\"},\"properties\":{\"이름\":{\"title\":[{\"text\":{\"content\":\"" + formattedDate + " 오전\"}}]},\"태그\":{\"multi_select\":[{\"name\":\"" + week_num + "\"}]}}}";
                else body = "{\"parent\":{\"database_id\":\"" + daily_scrum_id + "\"},\"properties\":{\"이름\":{\"title\":[{\"text\":{\"content\":\"" + formattedDate + " 오후\"}}]},\"태그\":{\"multi_select\":[{\"name\":\"" + week_num + "\"}]}}}";
                WebClient webClient = WebClient.builder()
                        .baseUrl(reqURL)
                        .defaultHeader("Authorization", "Bearer " + api_key)
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

                System.out.println(page_id);
            }
        } else {

            String body = "{\"parent\":{\"database_id\":\"" + daily_scrum_id + "\"},\"properties\":{\"이름\":{\"title\":[{\"text\":{\"content\":\"" + formattedDate + "\"}}]},\"태그\":{\"multi_select\":[{\"name\":\"" + week_num + "\"}]}}}";

            WebClient webClient = WebClient.builder()
                    .baseUrl(reqURL)
                    .defaultHeader("Authorization", "Bearer " + api_key)
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

            System.out.println(page_id);
        }
    }
}
