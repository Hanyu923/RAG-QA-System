package com.example.rag_qa_system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final List<Knowledge> knowledgeList = new CopyOnWriteArrayList<>();
    private final String dataFile;

    public KnowledgeService() {
        dataFile = System.getProperty("user.dir") + "/knowledge_data.txt";
        loadFromFile();
    }

    // ===== 从文件加载知识库 =====
    private void loadFromFile() {
        File file = new File(dataFile);
        if (!file.exists()) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("knowledge.txt");
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                String line;
                int id = 1;
                while ((line = br.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        knowledgeList.add(new Knowledge(id++, trimmed));
                    }
                }
                saveToFile();
                log.info("知识库初始化完成，共 {} 条", knowledgeList.size());
            } catch (Exception e) {
                log.error("初始化知识库失败", e);
            }
        } else {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
                String line;
                int id = 1;
                while ((line = br.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        knowledgeList.add(new Knowledge(id++, trimmed));
                    }
                }
                log.info("知识库加载完成，共 {} 条", knowledgeList.size());
            } catch (Exception e) {
                log.error("加载知识库失败", e);
            }
        }
    }

    // ===== 保存知识库到文件 =====
    private void saveToFile() {
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

    public List<Knowledge> getAll() {
        return new ArrayList<>(knowledgeList);
    }

    public Knowledge add(String content) {
        int newId = knowledgeList.isEmpty() ? 1 :
                knowledgeList.stream().mapToInt(Knowledge::getId).max().orElse(0) + 1;
        Knowledge k = new Knowledge(newId, content);
        knowledgeList.add(k);
        saveToFile();
        return k;
    }

    public boolean delete(int id) {
        boolean removed = knowledgeList.removeIf(k -> k.getId() == id);
        if (removed) saveToFile();
        return removed;
    }

    public List<String> getAllContents() {
        return knowledgeList.stream().map(Knowledge::getContent).toList();
    }

    // ===== 知识实体 =====
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
