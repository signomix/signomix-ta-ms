package com.signomix.messaging.webhook;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.jboss.logging.Logger;

import com.signomix.messaging.domain.Message;

public class WebhookStandardClient {
    private static final Logger LOG = Logger.getLogger(WebhookStandardClient.class);

    public HttpResponse<String> sendMessage(String url, Message message) {
        String requestBody = buildRequestBody(message);
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .setHeader("Content-Type", getContetnType(message))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
                    return response;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            LOG.error(e.getMessage());
            return null;
        }
    }

    public HttpResponse<String> sendMesaage(String url, String header, String token, Message message) {
        String requestBody = buildRequestBody(message);
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .setHeader(header, token)
                    .setHeader("Content-Type", getContetnType(message))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
                    return response;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            LOG.error(e.getMessage());
            return null;
        }
    }

    private String buildRequestBody(Message message){
        return message.content;
    }

    private String getContetnType(Message message){
        if(message.content.startsWith("{")){
            return "application/json";
        }else{
            return "application/x-www-form-urlencoded";
        }
    }
}
