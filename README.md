# RAG 智能问答系统

基于 Spring Boot + 通义千问 的 RAG 问答系统。用户提问后从本地知识库检索相关内容，让 AI 基于真实资料回答。

## 技术栈

- Java 17 + Spring Boot
- 通义千问 API
- Maven

## 快速启动

1. 在 ChatController.java 中填入你的阿里云 API Key
2. 运行 RagQaSystemApplication.java
3. 浏览器打开 http://localhost:8080/index.html

## 项目结构

- ChatController.java - 接收请求，调用 API
- DocumentSearchService.java - 知识库检索
- knowledge.txt - 本地知识库
- index.html - 对话页面
