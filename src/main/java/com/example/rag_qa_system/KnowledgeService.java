package com.example.rag_qa_system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 知识库服务 — 管理知识条的增删改查，数据持久化到文件
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final List<Knowledge> knowledgeList = new CopyOnWriteArrayList<>();
    private final String dataFile;
    private final AtomicInteger idSeq = new AtomicInteger(1);

    @Resource
    private ApplicationEventPublisher eventPublisher;

    public KnowledgeService() {
        dataFile = System.getProperty("user.dir") + "/knowledge_data.txt";
        loadFromFile();
    }

    /** 从文件加载知识库，文件不存在时从 classpath 初始化 */
    private void loadFromFile() {
        File file = new File(dataFile);
        try {
            if (!file.exists()) {
                loadFromClasspath();
            } else {
                loadFromDataFile(file);
            }
        } catch (Exception e) {
            log.error("加载知识库失败", e);
        }
    }

    private void loadFromClasspath() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("knowledge.txt");
        if (is == null) {
            log.warn("classpath 中未找到 knowledge.txt，知识库为空");
            return;
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    knowledgeList.add(new Knowledge(idSeq.getAndIncrement(), trimmed));
                }
            }
        }
        saveToFile();
        log.info("知识库初始化完成，共 {} 条", knowledgeList.size());
    }

    private void loadFromDataFile(File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    knowledgeList.add(new Knowledge(idSeq.getAndIncrement(), trimmed));
                }
            }
        }
        log.info("知识库加载完成，共 {} 条", knowledgeList.size());
    }

    /** 持久化知识库到文件（线程安全） */
    private synchronized void saveToFile() {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(dataFile), "UTF-8"))) {
            for (Knowledge k : knowledgeList) {
                bw.write(k.content);
                bw.newLine();
            }
        } catch (Exception e) {
            log.error("保存知识库失败", e);
        }
    }

    /** 获取全部知识 */
    public List<Knowledge> getAll() {
        return new ArrayList<>(knowledgeList);
    }

    /** 新增一条知识 */
    public Knowledge add(String content) {
        Knowledge k = new Knowledge(idSeq.getAndIncrement(), content);
        knowledgeList.add(k);
        saveToFile();
        eventPublisher.publishEvent(new KnowledgeChangeEvent(this));
        log.info("新增知识: id={}", k.getId());
        return k;
    }

    /** 按 ID 删除知识 */
    public boolean delete(int id) {
        boolean removed = knowledgeList.removeIf(k -> k.getId() == id);
        if (removed) {
            saveToFile();
            eventPublisher.publishEvent(new KnowledgeChangeEvent(this));
            log.info("删除知识: id={}", id);
        }
        return removed;
    }

    /** 获取全部知识内容（仅文本） */
    public List<String> getAllContents() {
        return knowledgeList.stream().map(Knowledge::getContent).toList();
    }

    /** 知识实体 */
    public static class Knowledge {
        private int id;
        private String content;

        public Knowledge() {}

        public Knowledge(int id, String content) {
            this.id = id;
            this.content = content;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
