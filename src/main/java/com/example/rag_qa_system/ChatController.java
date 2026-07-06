package com.example.rag_qa_system;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
public class ChatController {

    @Resource
    private ChatClient.Builder chatClientBuilder;

    @Resource
    private AgentTools agentTools;

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    private final DocumentSearchService docService;

    public ChatController(DocumentSearchService docService) {
        this.docService = docService;
    }

    @PostMapping("/chat")
    public String chat(@RequestBody Map<String, String> request) {
        String question = request.get("question");

        String cached = redisTemplate.opsForValue().get("chat:" + question);
        if (cached != null) {
            return cached;
        }

        String relevantDocs = docService.findRelevantContent(question);

        String systemPrompt = "你是一个知识库助手。请根据以下资料回答问题。"
                + "如果资料中没有答案，就说我暂时没有找到相关信息，不要瞎编。"
                + "\n\n【资料】\n" + relevantDocs;

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(question)
        ));
        String answer = chatClientBuilder
                .defaultTools(agentTools)
                .build()
                .prompt(prompt)
                .call()
                .content();

        if (answer == null) answer = "抱歉，暂时无法回答。";

        redisTemplate.opsForValue().set("chat:" + question, answer, 1, TimeUnit.HOURS);

        return answer;
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String relevantDocs = docService.findRelevantContent(question);

        String systemPrompt = "你是一个知识库助手。请根据以下资料回答问题。"
                + "如果资料中没有答案，就说我暂时没有找到相关信息，不要瞎编。"
                + "\n\n【资料】\n" + relevantDocs;

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(question)
        ));

        SseEmitter emitter = new SseEmitter(0L);

        chatClientBuilder.build()
                .prompt(prompt)
                .stream()
                .content()
                .subscribe(
                        content -> {
                            try {
                                emitter.send(SseEmitter.event().data(content));
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        },
                        error -> emitter.completeWithError(error),
                        () -> emitter.complete()
                );

        return emitter;
    }
}
