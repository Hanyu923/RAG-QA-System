package com.example.rag_qa_system;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.*;

/**
 * 文档检索服务 — 将知识库文本转为向量，通过余弦相似度做语义检索
 */
@Service
public class DocumentSearchService {

    private static final Logger log = LoggerFactory.getLogger(DocumentSearchService.class);

    /** 知识库文本列表 */
    private volatile List<String> sentences = Collections.emptyList();
    /** 每条文本对应的向量 */
    private volatile List<List<Double>> sentenceVectors = Collections.emptyList();

    private final EmbeddingService embeddingService;
    private final KnowledgeService knowledgeService;

    public DocumentSearchService(EmbeddingService embeddingService,
                                  KnowledgeService knowledgeService) {
        this.embeddingService = embeddingService;
        this.knowledgeService = knowledgeService;
    }

    /** 应用启动后异步生成向量，不阻塞启动流程 */
    @PostConstruct
    @Async
    public void initIndex() {
        rebuildIndex();
    }

    /** 从知识库加载所有文本 */
    private void loadDocument() {
        sentences = knowledgeService.getAllContents();
        log.info("文档加载完成，共 {} 条知识", sentences.size());
    }

    /** 为每条知识生成 Embedding 向量 */
    private void loadEmbeddings() {
        if (sentences.isEmpty()) return;
        List<String> currentSentences = sentences;
        List<List<Double>> vectors = new ArrayList<>();
        log.info("正在生成向量，共 {} 条", currentSentences.size());
        for (int i = 0; i < currentSentences.size(); i++) {
            List<Double> vector = embeddingService.getEmbedding(currentSentences.get(i));
            if (vector != null) {
                vectors.add(vector);
            } else {
                log.warn("第 {} 条向量生成失败", i + 1);
            }
        }
        sentenceVectors = vectors;
        log.info("向量生成完成");
    }

    /** 重建索引（知识变更后调用） */
    public synchronized void rebuildIndex() {
        loadDocument();
        loadEmbeddings();
    }

    /** 监听知识库变更事件，自动重建向量索引 */
    @EventListener
    @Async
    public void onKnowledgeChange(KnowledgeChangeEvent event) {
        log.info("知识库已变更，开始重建向量索引...");
        rebuildIndex();
    }

    /** 检索与问题最相关的 top-3 知识文本 */
    public String findRelevantContent(String question) {
        List<List<Double>> currentVectors = sentenceVectors;
        if (currentVectors.isEmpty()) {
            return "未找到相关信息，请根据通用知识回答。";
        }

        List<Double> questionVector = embeddingService.getEmbedding(question);
        if (questionVector == null) {
            return "未找到相关信息，请根据通用知识回答。";
        }

        // 计算每一条知识与问题的余弦相似度
        List<String> currentSentences = sentences;
        List<ScoredResult> scoredResults = new ArrayList<>();
        for (int i = 0; i < currentVectors.size(); i++) {
            double score = EmbeddingService.cosineSimilarity(questionVector, currentVectors.get(i));
            String text = i < currentSentences.size() ? currentSentences.get(i) : "";
            scoredResults.add(new ScoredResult(text, score));
        }

        // 按相似度降序排列，取前 3 条
        scoredResults.sort((a, b) -> Double.compare(b.score, a.score));
        return scoredResults.stream()
                .limit(3)
                .map(r -> r.text)
                .collect(Collectors.joining("\n"));
    }

    /** 带分数的检索结果 */
    private static class ScoredResult {
        String text;
        double score;
        ScoredResult(String text, double score) {
            this.text = text;
            this.score = score;
        }
    }
}
