package com.example.rag_qa_system;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Component
public class AgentTools {

    @Tool(description = "计算数学表达式，比如加法、减法、乘法、除法")
    public String calculate(String expression) {
        try {
            // 简单表达式求值（仅支持 + - * / 和括号）
            String expr = expression.replaceAll("\\s+", "");
            double result = evalSimple(expr);
            return expr + " = " + result;
        } catch (Exception e) {
            return "计算失败，请检查表达式格式";
        }
    }

    @Tool(description = "获取当前日期")
    public String getCurrentDate() {
        return "当前日期：" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    @Tool(description = "获取当前时间")
    public String getCurrentTime() {
        return "当前时间：" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private double evalSimple(String expr) {
        return new Object() {
            int pos = -1, ch;

            void next() {
                ch = (++pos < expr.length()) ? expr.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') next();
                if (ch == charToEat) {
                    next();
                    return true;
                }
                return false;
            }

            double parse() {
                next();
                double x = parseExpression();
                if (pos < expr.length()) throw new RuntimeException("意外的字符");
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (; ; ) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (; ; ) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') next();
                    x = Double.parseDouble(expr.substring(startPos, pos));
                } else {
                    throw new RuntimeException("意外的字符");
                }
                return x;
            }
        }.parse();
    }
}
