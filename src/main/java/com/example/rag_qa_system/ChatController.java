  package com.example.rag_qa_system;

  import jakarta.annotation.Resource;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  import org.springframework.ai.chat.client.ChatClient;
  import org.springframework.ai.chat.messages.SystemMessage;
  import org.springframework.ai.chat.messages.UserMessage;
  import org.springframework.ai.chat.prompt.Prompt;
  import org.springframework.data.redis.core.StringRedisTemplate;
  import org.springframework.web.bind.annotation.*;

  import java.util.List;
  import java.util.Map;
  import java.util.concurrent.TimeUnit;

  @RestController
  public class ChatController {

      private static final Logger log = LoggerFactory.getLogger(ChatController.class);

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
      public CommonResult<String> chat(@RequestBody Map<String, String> request) {
          String question = request.get("question");
          if (question == null || question.trim().isEmpty()) {
              return CommonResult.error(40001, "问题不能为空");
          }
          question = question.trim();

          // 查缓存
          String cached = redisTemplate.opsForValue().get("chat:" + question);
          if (cached != null) {
              log.info("缓存命中: {}", question);
              return CommonResult.success(cached);
          }

          log.info("用户提问: {}", question);

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

          if (answer == null) {
              return CommonResult.error(50001, "AI 回复失败");
          }

          redisTemplate.opsForValue().set("chat:" + question, answer, 1, TimeUnit.HOURS);
          log.info("AI回复成功，已缓存");
          return CommonResult.success(answer);
      }
  }