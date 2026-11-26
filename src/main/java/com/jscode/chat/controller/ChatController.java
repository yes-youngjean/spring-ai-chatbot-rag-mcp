package com.jscode.chat.controller;

import jakarta.annotation.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import java.awt.*;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/ai")
    public String generation(String userPrompt){
        return this.chatClient.prompt()
                .user(userPrompt)   //-> 전달하고
                .call()             //-> 부름
                .content();         //-> 단순한 TEXT 형태
    }


    @GetMapping(value = "/cr", produces = MediaType.APPLICATION_JSON_VALUE)
    public @Nullable ChatResponse chatResponse(String userPrompt){
        return this.chatClient.prompt()
                .user(userPrompt)
                .call()
                .chatResponse();    //-> MetaData 형식으로 전달
    }


    @GetMapping(value = "/st", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(String userPrompt){
        return this.chatClient.prompt()
                .user(userPrompt)
                .stream()           //-> Stream: 생성되는 대로 조각(토큰) 단위로 받는 방식을 의미
                .content();
    }
}
