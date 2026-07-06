package com.example.rag_qa_system;

import org.springframework.stereotype.Service;
import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class KnowledgeService {

    private final List<Knowledge> knowledgeList = new CopyOnWriteArrayList<>();
    private final String dataFile;

    public KnowledgeService() {
        // 数据文件存在项目运行目录下
        dataFile = System.getProperty("user.dir") + "/knowledge_data.txt";
        loadFromFile();
    }

    private void loadFromFile() {
        File file = new File(dataFile);
        if (!file.exists()) {
            // 首次运行，从 classpath 的 knowledge.txt 复制
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
                System.out.println("知识库初始化完成，共 " + knowledgeList.size() + " 条");
            } catch (Exception e) {
                System.err.println("初始化知识库失败: " + e.getMessage());
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
                System.out.println("知识库加载完成，共 " + knowledgeList.size() + " 条");
            } catch (Exception e) {
                System.err.println("加载知识库失败: " + e.getMessage());
            }
        }
    }

    private void saveToFile() {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(dataFile), "UTF-8"))) {
            for (Knowledge k : knowledgeList) {
                bw.write(k.content);
                bw.newLine();
            }
        } catch (Exception e) {
            System.err.println("保存知识库失败: " + e.getMessage());
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
