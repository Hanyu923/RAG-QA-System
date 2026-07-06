package com.example.rag_qa_system;

import com.example.rag_qa_system.KnowledgeService.Knowledge;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    public List<Knowledge> list() {
        return knowledgeService.getAll();
    }

    @PostMapping
    public Knowledge add(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("内容不能为空");
        }
        return knowledgeService.add(content.trim());
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable int id) {
        boolean ok = knowledgeService.delete(id);
        return ok ? "删除成功" : "删除失败，未找到该知识";
    }
}
