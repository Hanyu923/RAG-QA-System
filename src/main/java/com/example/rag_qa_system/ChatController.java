package com.example.rag_qa_system;

  import com.fasterxml.jackson.databind.JsonNode;
  import com.fasterxml.jackson.databind.ObjectMapper;
  import org.springframework.web.bind.annotation.*;
  import org.springframework.web.client.RestTemplate;
  import org.springframework.http.*;
  import java.util.*;

  @RestController
  public class ChatController {

      private static final String API_KEY = "your apikey";
      private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

      private final DocumentSearchService docService;

      public ChatController(DocumentSearchService docService) {
          this.docService = docService;
      }

      @PostMapping("/chat")
      public String chat(@RequestBody Map<String, String> request) {
          RestTemplate restTemplate = new RestTemplate();

          HttpHeaders headers = new HttpHeaders();
          headers.setContentType(MediaType.APPLICATION_JSON);
          headers.set("Authorization", "Bearer " + API_KEY);

          String question = request.get("question");
          String relevantDocs = docService.findRelevantContent(question);

          Map<String, Object> requestBody = new HashMap<>();
          requestBody.put("model", "qwen-turbo");

          List<Map<String, String>> messages = new ArrayList<>();

          Map<String, String> systemMessage = new HashMap<>();
          systemMessage.put("role", "system");
          systemMessage.put("content","你是一个知识库助手。请根据以下资料回答问题。如果资料中没有答案，就说我暂时没有找到相关信息，不要瞎编。\n\n【资料】\n"+ relevantDocs);
          messages.add(systemMessage);

          Map<String, String> userMessage = new HashMap<>();
          userMessage.put("role", "user");
          userMessage.put("content", question);
          messages.add(userMessage);

          requestBody.put("messages", messages);

          HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
          ResponseEntity<String> response = restTemplate.postForEntity(API_URL, entity, String.class);

          try {
              ObjectMapper mapper = new ObjectMapper();
              JsonNode root = mapper.readTree(response.getBody());
              String answer = root.path("choices").get(0).path("message").path("content").asText();
              return answer;
          } catch (Exception e) {
              return "解析回复出错: " + e.getMessage();
          }
      }
  }