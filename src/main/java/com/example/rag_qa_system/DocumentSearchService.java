package com.example.rag_qa_system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.*;

@Service
public class DocumentSearchService {

    private static final Logger log = LoggerFactory.getLogger(DocumentSearchService.class);

    private List<String> sentences = new ArrayList<>();
    private List<List<Double>> sentenceVectors = new ArrayList<>();
    private final EmbeddingService embeddingService;
    private final KnowledgeService knowledgeService;

    public DocumentSearchService(EmbeddingService embeddingService,
                                  KnowledgeService knowledgeService) {
        this.embeddingService = embeddingService;
        this.knowledgeService = knowledgeService;
        loadDocument();
        loadEmbeddings();
    }

    private void loadDocument() {
        sentences = knowledgeService.getAllContents();
        log.info("文档加载完成，共 {} 条知识", sentences.size());
    }

    private void loadEmbeddings() {
        if (sentences.isEmpty()) return;
        log.info("正在生成向量，共 {} 条", sentences.size());
        for (int i = 0; i < sentences.size(); i++) {
            List<Double> vector = embeddingService.getEmbedding(sentences.get(i));
            if (vector != null) {
                sentenceVectors.add(vector);
                log.info("第 {} 条向量生成完成", i + 1);
            } else {
                log.warn("第 {} 条向量生成失败", i + 1);
            }
        }
        log.info("向量生成完成");
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
