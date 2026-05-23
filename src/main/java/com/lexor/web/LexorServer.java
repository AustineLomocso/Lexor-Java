package com.lexor.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lexor.error.LexorException;
import com.lexor.error.LexorRuntimeException;
import com.lexor.error.ParseException;
import com.lexor.error.SemanticException;
import com.lexor.interpreter.Interpreter;
import com.lexor.lexer.Lexer;
import com.lexor.lexer.Token;
import com.lexor.parser.Parser;
import com.lexor.parser.ast.ProgramNode;
import com.lexor.semantic.SemanticAnalyzer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class LexorServer {

    private static final Gson GSON = new Gson();

    public static void start(int port) throws IOException, InterruptedException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/run", new RunHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("LEXOR HTTP server running at http://localhost:" + port);
        System.out.println("Press Ctrl+C to stop.");
        Thread.currentThread().join(); // block until process is killed
    }

    static class RunHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(
                exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            JsonObject req = GSON.fromJson(body, JsonObject.class);
            String code  = req.has("code")  ? req.get("code").getAsString()  : "";
            String stdin = req.has("stdin") ? req.get("stdin").getAsString() : "";

            RunResult result = runInterpreter(code, stdin);

            String json = GSON.toJson(result);
            byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.getResponseBody().close();
        }

        private void addCorsHeaders(HttpExchange exchange) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        }

        private RunResult runInterpreter(String code, String stdin) {
            ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
            PrintStream out = new PrintStream(outBuffer, true, StandardCharsets.UTF_8);
            Scanner in = new Scanner(stdin != null ? stdin : "");

            try {
                Lexer lexer        = new Lexer(code);
                List<Token> tokens = lexer.tokenize();
                Parser parser      = new Parser(tokens);
                ProgramNode ast    = parser.parse();
                new SemanticAnalyzer().analyze(ast);
                new Interpreter(out, in).interpret(ast);
                return RunResult.success(outBuffer.toString(StandardCharsets.UTF_8));

            } catch (ParseException e) {
                return RunResult.failure("Parse Error", e.getMessage(), e.getLine(), e.getColumn());
            } catch (SemanticException e) {
                return RunResult.failure("Semantic Error", e.getMessage(), e.getLine(), e.getCol());
            } catch (LexorRuntimeException e) {
                return RunResult.failure("Runtime Error", e.getMessage(), e.getLine(), 0);
            } catch (LexorException e) {
                return RunResult.failure("Lex Error", e.getMessage(), e.getLine(), e.getColumn());
            } catch (Exception e) {
                return RunResult.failure("Internal Error", e.getMessage(), 0, 0);
            }
        }
    }
}
