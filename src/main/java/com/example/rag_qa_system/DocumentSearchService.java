package com.example.rag_qa_system;

  import org.springframework.stereotype.Service;
  import java.io.*;
  import java.util.*;

  @Service
  public class DocumentSearchService {

      private List<String> sentences = new ArrayList<>();

      public DocumentSearchService() {
          loadDocument();
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
              for (String s : sentences) {
                  System.out.println("  - " + s);
              }
          } catch (Exception e) {
              System.err.println("加载文档失败: " + e.getMessage());
          }
      }

      public String findRelevantContent(String question) {
          String bestMatch = "";
          int maxScore = 0;

          for (String sentence : sentences) {
              int score = calculateScore(question, sentence);
              if (score > maxScore) {
                  maxScore = score;
                  bestMatch = sentence;
              }
          }

          if (maxScore == 0) {
              return "未找到相关信息，请根据通用知识回答。";
          }
          return bestMatch;
      }

      private int calculateScore(String question, String sentence) {
          int score = 0;
          String[] words = question.split("");
          for (String word : words) {
              if (sentence.contains(word)) {
                  score++;
              }
          }
          if (sentence.contains(question)) {
              score += 10;
          }
          return score;
      }
  }