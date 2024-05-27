package com.okane.intergratedapi.util;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CommonUtil {
    public WebClient webClient = WebClient.builder().build();
}
