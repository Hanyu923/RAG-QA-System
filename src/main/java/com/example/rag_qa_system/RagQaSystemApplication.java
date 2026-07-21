package com.example.rag_qa_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * RAG 智能问答系统 — 主启动类
 */
@EnableAsync
@SpringBootApplication
public class RagQaSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(RagQaSystemApplication.class, args);
	}

}
