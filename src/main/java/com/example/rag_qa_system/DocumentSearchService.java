package com.example.rag_qa_system;

  import org.springframework.stereotype.Service;
  import java.io.*;
  import java.util.*;
  import java.util.stream.*;

  @Service
  public class DocumentSearchService {

      private List<String> sentences = new ArrayList<>();
      private List<List<Double>> sentenceVectors = new ArrayList<>();
      private final EmbeddingService embeddingService;

      public DocumentSearchService(EmbeddingService embeddingService) {
          this.embeddingService = embeddingService;
          loadDocument();
          loadEmbeddings();
      }

      private void loadDocument() {
          try {
              InputStream inputStream = getClass().getClassLoader().getResourceAsStream("knowledge.txt");
              BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
              String line;
              while ((line = reader.readLine()) != null) {
                  String trimmed = line.trim();
                  if (!trimmed.isEmpty()) {
                      sentences.add(trimmed);
                  }
              }
              reader.close();
              System.out.println("文档加载完成，共 " + sentences.size() + " 条知识。");
          } catch (Exception e) {
              System.err.println("加载文档失败: " + e.getMessage());
          }
      }

      private void loadEmbeddings() {
          System.out.println("正在生成向量，共 " + sentences.size() + " 条，请稍候...");
          for (int i = 0; i < sentences.size(); i++) {
              List<Double> vector = embeddingService.getEmbedding(sentences.get(i));
              if (vector != null) {
                  sentenceVectors.add(vector);
                  System.out.println("  - 第 " + (i + 1) + " 条向量生成完成");
              } else {
                  System.out.println("  - 第 " + (i + 1) + " 条向量生成失败");
              }
          }
          System.out.println("向量生成完成！");
      }

      public String findRelevantContent(String question) {
          if (sentenceVectors.isEmpty()) {
              return "未找到相关信息，请根据通用知识回答。";
          }

          List<Double> questionVector = embeddingService.getEmbedding(question);
          if (questionVector == null) {
              return "未找到相关信息，请根据通用知识回答。";
          }

          List<ScoredResult> scoredResults = new ArrayList<>();
          for (int i = 0; i < sentenceVectors.size(); i++) {
              double score = EmbeddingService.cosineSimilarity(questionVector, sentenceVectors.get(i));
              scoredResults.add(new ScoredResult(sentences.get(i), score));
          }

          scoredResults.sort((a, b) -> Double.compare(b.score, a.score));

          return scoredResults.stream()
                  .limit(3)
                  .map(r -> r.text)
                  .collect(Collectors.joining("\n"));
      }

      private static class ScoredResult {
          String text;
          double score;
          ScoredResult(String text, double score) {
              this.text = text;
              this.score = score;
          }
      }
  }