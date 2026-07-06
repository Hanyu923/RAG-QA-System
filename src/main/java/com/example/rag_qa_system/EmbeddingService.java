package com.example.rag_qa_system;

  import com.fasterxml.jackson.databind.JsonNode;
  import com.fasterxml.jackson.databind.ObjectMapper;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.http.*;
  import org.springframework.stereotype.Service;
  import org.springframework.web.client.RestTemplate;
  import java.util.*;

  @Service
  public class EmbeddingService {

      @Value("${aliyun.api.key}")
      private String apiKey;

      @Value("${aliyun.embedding.model}")
      private String model;

      private static final String EMBEDDING_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";

      public List<Double> getEmbedding(String text) {
          RestTemplate restTemplate = new RestTemplate();

          HttpHeaders headers = new HttpHeaders();
          headers.setContentType(MediaType.APPLICATION_JSON);
          headers.set("Authorization", "Bearer " + apiKey);

          Map<String, Object> requestBody = new HashMap<>();
          requestBody.put("model", model);
          requestBody.put("input", text);

          HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

          try {
              ResponseEntity<String> response = restTemplate.postForEntity(EMBEDDING_URL, entity, String.class);
              ObjectMapper mapper = new ObjectMapper();
              JsonNode root = mapper.readTree(response.getBody());
              JsonNode embeddingNode = root.path("data").get(0).path("embedding");

              List<Double> vector = new ArrayList<>();
              for (JsonNode node : embeddingNode) {
                  vector.add(node.asDouble());
              }
              return vector;
          } catch (Exception e) {
              System.err.println("Embedding API 调用失败: " + e.getMessage());
              return null;
          }
      }

      public static double cosineSimilarity(List<Double> vecA, List<Double> vecB) {
          double dotProduct = 0.0, normA = 0.0, normB = 0.0;
          for (int i = 0; i < vecA.size(); i++) {
              dotProduct += vecA.get(i) * vecB.get(i);
              normA += Math.pow(vecA.get(i), 2);
              normB += Math.pow(vecB.get(i), 2);
          }
          if (normA == 0 || normB == 0) return 0;
          return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
      }
  }