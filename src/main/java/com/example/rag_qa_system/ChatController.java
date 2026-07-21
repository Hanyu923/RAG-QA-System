package com.example.rag_qa_system;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI 对话接口 — 处理用户提问、RAG 检索、LLM 调用及缓存
 */
@RestController
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Resource
    private ChatClient.Builder chatClientBuilder;

    @Resource
    private AgentTools agentTools;

    /** 本地缓存（最大 1000 条，写入后 1 小时过期） */
    private final Cache<String, String> cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats()
            .build();

    private final DocumentSearchService docService;

    public ChatController(DocumentSearchService docService) {
        this.docService = docService;
    }

    /** 处理用户提问：缓存 → 向量检索 → LLM 回答 → 回写缓存 */
    @PostMapping("/chat")
    public CommonResult<String> chat(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        if (question == null || question.trim().isEmpty()) {
            return CommonResult.error(40001, "问题不能为空");
        }
        question = question.trim();

        // 1. 缓存命中则直接返回
        String cacheKey = "chat:" + question;
        String cached = cache.getIfPresent(cacheKey);
        if (cached != null) {
            log.info("缓存命中: {}", question);
            return CommonResult.success(cached);
        }

        log.info("用户提问: {}", question);

        // 2. 向量检索相关文档
        String relevantDocs = docService.findRelevantContent(question);

        // 3. 构建 Prompt，让 AI 基于检索结果回答
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

        if (answer == null) {
            return CommonResult.error(50001, "AI 回复失败");
        }

        // 4. 写入缓存（自动过期，无需手动清理）
        cache.put(cacheKey, answer);
        log.info("AI回复成功，已缓存");
        return CommonResult.success(answer);
    }
}