package com.example.rag_qa_system;

import com.example.rag_qa_system.KnowledgeService.Knowledge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识库管理接口 — 知识条的查询、新增、删除
 */
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeController.class);

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    /** 获取全部知识 */
    @GetMapping
    public CommonResult<List<Knowledge>> list() {
        List<Knowledge> list = knowledgeService.getAll();
        return CommonResult.success(list);
    }

    /** 新增知识 */
    @PostMapping
    public CommonResult<Knowledge> add(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) {
            return CommonResult.error(40001, "内容不能为空");
        }
        Knowledge k = knowledgeService.add(content.trim());
        return CommonResult.success(k);
    }

    /** 删除知识 */
    @DeleteMapping("/{id}")
    public CommonResult<String> delete(@PathVariable int id) {
        boolean ok = knowledgeService.delete(id);
        if (ok) {
            log.info("删除知识: id={}", id);
            return CommonResult.success("删除成功");
        }
        return CommonResult.error(40004, "未找到该知识");
    }
}
