package com.example.rag_qa_system;

import org.springframework.context.ApplicationEvent;

/**
 * 知识库变更事件 — 数据增删后通知向量索引重建
 */
public class KnowledgeChangeEvent extends ApplicationEvent {

    public KnowledgeChangeEvent(Object source) {
        super(source);
    }
}
