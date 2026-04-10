# LEXOR Interpreter — Design & Architecture Documentation

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [IntelliJ IDEA Project Setup](#intellij-idea-project-setup)
4. [Libraries & Dependencies](#libraries--dependencies)
5. [How It Works](#how-it-works)
6. [Concepts Used & Applied](#concepts-used--applied)
7. [Project Structure](#project-structure)
8. [Package Diagram](#package-diagram)
9. [Class Diagram](#class-diagram)
10. [Activity Diagram](#activity-diagram)

---

## Overview

LEXOR is a strongly-typed, interpreted programming language designed for Senior High School students. The interpreter is built in **Java** and follows the classic pipeline of a language processor: **Lexical Analysis → Parsing → Semantic Analysis → Execution (Tree-Walk Interpretation)**. No bytecode or virtual machine is involved — the interpreter directly walks the Abstract Syntax Tree (AST) to execute statements.

---

## Prerequisites

Before opening IntelliJ, ensure the following are installed on your machine:

| Tool | Minimum Version | Download |
|---|---|---|
| **JDK (Java Development Kit)** | JDK 17 (LTS) | https://adoptium.net |
| **IntelliJ IDEA** | 2023.1 Community or Ultimate | https://www.jetbrains.com/idea/download |
| **Apache Maven** | 3.9+ (bundled in IntelliJ, or install separately) | https://maven.apache.org/download.cgi |
| **Git** *(optional but recommended)* | Any recent version | https://git-scm.com |

To verify your JDK and Maven installations, run in a terminal:

```bash
java -version
mvn -version
```

Both commands should return version numbers without errors before proceeding.

---

## IntelliJ IDEA Project Setup

### Step 1 — Create a new Maven project

1. Open IntelliJ IDEA. On the **Welcome screen**, click **New Project**.
2. In the left panel, select **Maven Archetype**.
3. Fill in the fields:
   - **Name:** `lexor-interpreter`
   - **Location:** choose your preferred directory
   - **Language:** Java
   - **Build system:** Maven
   - **JDK:** select JDK 17 (if not listed, click **Add JDK** and point to your JDK 17 install directory)
   - **Archetype:** `org.apache.maven.archetypes:maven-archetype-quickstart`
4. Expand **Advanced Settings** and set:
   - **GroupId:** `com.lexor`
   - **ArtifactId:** `lexor-interpreter`
   - **Version:** `1.0-SNAPSHOT`
5. Click **Create**. IntelliJ generates the skeleton project and opens it.

> If the Welcome screen is not shown (a project is already open), go to **File → New → Project** and follow the same steps.

---

### Step 2 — Configure the `pom.xml`

IntelliJ will open `pom.xml` automatically. Replace its entire contents with the full POM below. IntelliJ will show a **"Maven build scripts found"** notification — click **Load Maven Changes** (or the reload icon in the Maven tool window) to download all dependencies.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.lexor</groupId>
    <artifactId>lexor-interpreter</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>

        <!-- Unit testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>

        <!-- Mocking in tests -->
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>5.11.0</version>
            <scope>test</scope>
        </dependency>

        <!-- Logging API -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.13</version>
        </dependency>

        <!-- Logging implementation -->
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.5.6</version>
        </dependency>

        <!-- String and character utilities -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>3.14.0</version>
        </dependency>

        <!-- REPL terminal line editing and history -->
        <dependency>
            <groupId>org.jline</groupId>
            <artifactId>jline</artifactId>
            <version>3.26.1</version>
        </dependency>

        <!-- CLI argument parsing -->
        <dependency>
            <groupId>info.picocli</groupId>
            <artifactId>picocli</artifactId>
            <version>4.7.6</version>
        </dependency>

    </dependencies>

    <build>
        <plugins>

            <!-- Run JUnit 5 tests with Maven -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>

            <!-- Package all dependencies into a single executable fat-JAR -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.3</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <transformers>
                                <transformer implementation=
                                    "org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.lexor.Main</mainClass>
                                </transformer>
                            </transformers>
                            <createDependencyReducedPom>false</createDependencyReducedPom>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

        </plugins>
    </build>

</project>
```

After IntelliJ finishes downloading dependencies, the **External Libraries** section in the Project panel will list all jars. If downloads fail, go to **File → Settings → Build, Execution, Deployment → Build Tools → Maven** and verify the Maven home path and repository settings.

---

### Step 3 — Set the project SDK

1. Go to **File → Project Structure** (shortcut: `Ctrl+Alt+Shift+S` on Windows/Linux, `⌘;` on macOS).
2. Under **Project**, confirm:
   - **SDK:** `17` (your JDK 17 install)
   - **Language level:** `17 - Sealed classes, always-strict floating-point`
3. Click **Apply → OK**.

---

### Step 4 — Create the package structure

In the **Project** tool window (left side), expand `src/main/java`. Right-click on the `java` folder and select **New → Package**. Create each of the following packages one at a time:

```
com.lexor
com.lexor.lexer
com.lexor.parser
com.lexor.parser.ast
com.lexor.semantic
com.lexor.interpreter
com.lexor.visitor
com.lexor.error
com.lexor.repl
```

Then repeat under `src/test/java`:

```
com.lexor.lexer
com.lexor.parser
com.lexor.semantic
com.lexor.interpreter
```

> **Tip:** In IntelliJ you can type the full dotted package name (e.g., `com.lexor.parser.ast`) in the New Package dialog and IntelliJ will create all intermediate directories automatically.

---

### Step 5 — Create the `Main.java` entry point

Right-click `com.lexor` under `src/main/java` → **New → Java Class** → name it `Main`. This is the Picocli CLI entry point that will be specified as the main class in the fat-JAR manifest. A skeleton:

```java
package com.lexor;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "lexor", mixinStandardHelpOptions = true, version = "1.0",
         description = "Runs a LEXOR source file or starts an interactive REPL.")
public class Main implements Callable<Integer> {

    @Parameters(index = "0", description = "LEXOR source file (.lxr)", arity = "0..1")
    private File sourceFile;

    @Option(names = {"--repl", "-r"}, description = "Start interactive REPL mode")
    private boolean replMode;

    @Override
    public Integer call() throws Exception {
        if (replMode) {
            // new ReplRunner().start();
        } else if (sourceFile != null) {
            // String source = Files.readString(sourceFile.toPath());
            // run pipeline: Lexer -> Parser -> SemanticAnalyzer -> Interpreter
        } else {
            System.err.println("Provide a .lxr file or use --repl");
            return 1;
        }
        return 0;
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
```

---

### Step 6 — Add the Logback configuration file

Right-click `src/main/resources` (create the folder if it does not exist: right-click `src/main` → **New → Directory** → name it `resources`, then mark it as a resources root via **File → Project Structure → Modules → Sources**). Create a file named `logback.xml`:

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss} [%level] %logger{20} - %msg%n</pattern>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

Change `level="INFO"` to `level="DEBUG"` during development to see AST-dump logs.

---

### Step 7 — Create the `samples` directory

Right-click the project root (not `src`) → **New → Directory** → name it `samples`. Add a test file `samples/hello.lxr`:

```
%% hello world sample
SCRIPT AREA
START SCRIPT
DECLARE INT x=4
DECLARE BOOL t="TRUE"
DECLARE CHAR a_1='c'
PRINT: x & t & [#] & $ & a_1
END SCRIPT
```

---

### Step 8 — Configure a Run/Debug configuration

1. Click **Run → Edit Configurations** (or the dropdown in the top toolbar → **Edit Configurations**).
2. Click the **+** icon → **Application**.
3. Set:
   - **Name:** `Run File`
   - **Module:** `lexor-interpreter`
   - **Main class:** `com.lexor.Main`
   - **Program arguments:** `samples/hello.lxr`
4. Add a second configuration:
   - **Name:** `REPL`
   - **Program arguments:** `--repl`
5. Click **OK**.

You can now press the green **Run** button to execute either configuration, and the **Debug** button to set breakpoints inside any interpreter stage.

---

### Step 9 — Build the fat-JAR

Open the **Terminal** tab at the bottom of IntelliJ (or any system terminal in the project root) and run:

```bash
mvn clean package
```

Maven compiles all sources, runs all tests, then the Shade plugin bundles everything into:

```
target/lexor-interpreter-1.0-SNAPSHOT.jar
```

Run it from the terminal:

```bash
# File mode
java -jar target/lexor-interpreter-1.0-SNAPSHOT.jar samples/hello.lxr

# REPL mode
java -jar target/lexor-interpreter-1.0-SNAPSHOT.jar --repl
```

---

### Step 10 — Recommended IntelliJ plugins and settings

These are optional but significantly improve the development experience:

| Plugin / Setting | Why it helps |
|---|---|
| **SonarLint** (plugin) | Flags common bugs and code smells inline as you type |
| **Rainbow Brackets** (plugin) | Color-codes nested parentheses — very useful when writing the recursive descent parser |
| **Indent Rainbow** (plugin) | Visualizes indentation depth, helpful for nested AST visitor methods |
| **Save Actions** (plugin) | Auto-formats and organizes imports on every save |
| **Editor → Code Style → Java → Tabs and Indents** | Set indent to 4 spaces, ensure "Use tab character" is unchecked |
| **Editor → General → Auto Import** | Enable "Add unambiguous imports on the fly" and "Optimize imports on the fly" |
| **Version Control → Git** | Initialize a local git repo: **VCS → Create Git Repository** |

---

## Libraries & Dependencies

All dependencies are managed with **Maven** (`pom.xml`).

| Library / Tool | Purpose | Maven Artifact |
|---|---|---|
| **JUnit 5** | Unit testing for lexer, parser, and interpreter stages | `org.junit.jupiter:junit-jupiter:5.10.2` |
| **Mockito** | Mocking dependencies in unit tests | `org.mockito:mockito-core:5.11.0` |
| **SLF4J + Logback** | Structured logging across all interpreter phases | `org.slf4j:slf4j-api:2.0.13` + `ch.qos.logback:logback-classic:1.5.6` |
| **Apache Commons Lang** | String utilities, character classification helpers | `org.apache.commons:commons-lang3:3.14.0` |
| **JLine 3** | Interactive REPL (Read-Eval-Print Loop) with line editing and history | `org.jline:jline:3.26.1` |
| **Picocli** | CLI argument parsing for the interpreter entry point (file mode vs REPL mode) | `info.picocli:picocli:4.7.6` |
| **Maven Shade Plugin** | Package the entire project into a single executable fat-JAR | `org.apache.maven.plugins:maven-shade-plugin:3.5.3` |

> **Note:** Python and JavaScript are explicitly excluded per the language specification.

---

## How It Works

The LEXOR interpreter processes source code through five sequential, clearly defined stages:

```
Source Code (.lxr file or REPL input)
        |
        v
+-----------------+
|  1. LEXER        |  Tokenizes raw text into a flat list of Tokens
+--------+--------+
         |
         v
+-----------------+
|  2. PARSER       |  Transforms Token list into an Abstract Syntax Tree (AST)
+--------+--------+
         |
         v
+-----------------+
|  3. SEMANTIC     |  Type-checks declarations, assignments, and expressions
|     ANALYZER     |
+--------+--------+
         |
         v
+-----------------+
|  4. INTERPRETER  |  Tree-walks the AST; executes statements and evaluates
|  (Tree-Walker)   |  expressions against the Symbol Table (Environment)
+--------+--------+
         |
         v
+-----------------+
|  5. OUTPUT /     |  Writes to stdout (PRINT) or reads from stdin (SCAN)
|     I/O          |
+-----------------+
```

### Stage-by-Stage Explanation

**Stage 1 — Lexical Analysis (Lexer)**
The `Lexer` reads the source character-by-character and groups characters into meaningful units called **Tokens**. Each token has a `TokenType` (e.g., `KEYWORD_DECLARE`, `IDENTIFIER`, `INT_LITERAL`, `OP_PLUS`) and its raw lexeme. Comments (`%%`) are stripped. Whitespace between tokens is ignored; newlines are emitted as `NEWLINE` tokens (since LEXOR is line-delimited). All reserved words are matched case-sensitively in uppercase.

**Stage 2 — Parsing (Parser)**
The `Parser` applies a **Recursive Descent** strategy, consuming the flat token list and constructing a tree of `ASTNode` objects. Each grammar rule (program, declaration, statement, expression) maps to a recursive method. Operator precedence and associativity are encoded structurally in the method call chain (unary → multiplicative → additive → relational → equality → logical). The result is an `AST` root wrapping the entire program.

**Stage 3 — Semantic Analysis**
The `SemanticAnalyzer` performs a single pass over the AST before execution. It validates: all variables are declared before use, no duplicate declarations exist, assignment types match (strong typing), and that control-flow predicates evaluate to BOOL. It populates a `SymbolTable` with data-type metadata.

**Stage 4 — Interpretation (Tree-Walking Evaluator)**
The `Interpreter` walks the validated AST recursively. It maintains an `Environment` (runtime symbol table) mapping variable names to their current values. For each node: `DeclarationNode` initializes storage; `AssignmentNode` updates it; `PrintNode` concatenates its operands using `&` and writes to output; `IfNode` / `ForNode` / `RepeatNode` control program flow by conditionally recursing.

**Stage 5 — I/O**
`PRINT` output is directed to `System.out`. `SCAN` reads from `System.in`, splits on commas, coerces each token to the declared type of the target variable, and stores the value in the `Environment`.

---

## Concepts Used & Applied

| Concept | Where Applied |
|---|---|
| **Lexical Analysis / Tokenization** | `Lexer` class — transforms raw characters into typed Tokens using a finite-state automaton approach |
| **Recursive Descent Parsing** | `Parser` class — each grammar production rule is a Java method that may call itself or other rule-methods recursively |
| **Abstract Syntax Tree (AST)** | All `ASTNode` subclasses form a composite tree representing the program's structure without syntax noise |
| **Visitor Design Pattern** | `SemanticAnalyzer` and `Interpreter` both implement `ASTVisitor<T>`, decoupling traversal logic from node data |
| **Composite Design Pattern** | `ASTNode` hierarchy — compound nodes (e.g., `BinaryExprNode`) contain child nodes, enabling uniform traversal |
| **Symbol Table / Environment** | `Environment` class — a scoped hash map storing variable names to `LexorValue` wrappers; supports nested scopes for control-flow blocks |
| **Strong Static Typing** | `SemanticAnalyzer` enforces type compatibility at analysis time before any execution occurs |
| **Operator Precedence via Call Stack** | Parser method ordering encodes LEXOR's arithmetic precedence: `()` → `unary` → `*/%` → `+-` → `><` → `== <>` → `AND OR NOT` |
| **Escape Code Processing** | `PrintNode` evaluation recognizes `[` + char + `]` patterns (e.g., `[#]` to `#`, `[[]` to `[`) |
| **Control Flow with Structured Blocks** | `START/END` keyword pairs delimit block scope, mirrored in AST block nodes and `Environment` scope push/pop |
| **Single Responsibility Principle** | Each pipeline stage is a separate class with no crossover concerns |
| **Error Reporting** | `LexorException`, `ParseException`, `SemanticException`, `RuntimeException` carry line/column info for student-friendly messages |
| **REPL Architecture** | `ReplRunner` wraps the full pipeline in a loop, re-initializing lexer and parser per input while persisting environment state |

---

## Project Structure

```
lexor-interpreter/
├── pom.xml
├── README.md
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── lexor/
│   │   │           │
│   │   │           ├── Main.java                        <- Entry point (Picocli CLI)
│   │   │           │
│   │   │           ├── lexer/
│   │   │           │   ├── Lexer.java                   <- Tokenizer
│   │   │           │   ├── Token.java                   <- Token data class
│   │   │           │   └── TokenType.java               <- Enum of all token kinds
│   │   │           │
│   │   │           ├── parser/
│   │   │           │   ├── Parser.java                  <- Recursive descent parser
│   │   │           │   └── ast/
│   │   │           │       ├── ASTNode.java             <- Abstract base node
│   │   │           │       ├── ProgramNode.java
│   │   │           │       ├── DeclarationNode.java
│   │   │           │       ├── AssignmentNode.java
│   │   │           │       ├── PrintNode.java
│   │   │           │       ├── ScanNode.java
│   │   │           │       ├── BinaryExprNode.java
│   │   │           │       ├── UnaryExprNode.java
│   │   │           │       ├── LiteralNode.java
│   │   │           │       ├── VariableNode.java
│   │   │           │       ├── IfNode.java
│   │   │           │       ├── ForNode.java
│   │   │           │       └── RepeatNode.java
│   │   │           │
│   │   │           ├── semantic/
│   │   │           │   ├── SemanticAnalyzer.java        <- Type checker & scope validator
│   │   │           │   └── SymbolTable.java             <- Compile-time variable registry
│   │   │           │
│   │   │           ├── interpreter/
│   │   │           │   ├── Interpreter.java             <- AST tree-walker
│   │   │           │   ├── Environment.java             <- Runtime variable store (scoped)
│   │   │           │   └── LexorValue.java              <- Runtime value wrapper (INT/CHAR/BOOL/FLOAT)
│   │   │           │
│   │   │           ├── visitor/
│   │   │           │   └── ASTVisitor.java              <- Visitor interface
│   │   │           │
│   │   │           ├── error/
│   │   │           │   ├── LexorException.java
│   │   │           │   ├── ParseException.java
│   │   │           │   ├── SemanticException.java
│   │   │           │   └── LexorRuntimeException.java
│   │   │           │
│   │   │           └── repl/
│   │   │               └── ReplRunner.java              <- Interactive REPL loop (JLine)
│   │   │
│   │   └── resources/
│   │       └── logback.xml                             <- Logging configuration
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── lexor/
│                   ├── lexer/
│                   │   └── LexerTest.java
│                   ├── parser/
│                   │   └── ParserTest.java
│                   ├── semantic/
│                   │   └── SemanticAnalyzerTest.java
│                   └── interpreter/
│                       └── InterpreterTest.java
│
└── samples/
    ├── hello.lxr
    ├── arithmetic.lxr
    ├── logical.lxr
    ├── if_else.lxr
    ├── for_loop.lxr
    └── repeat_when.lxr
```

---

## Package Diagram

```
+------------------------------------------------------------------------------+
|                          com.lexor (root)                                     |
|  +----------+                                                                 |
|  |  Main    | --uses--> all packages below                                   |
|  +----------+                                                                 |
+------------------------------------------------------------------------------+

+---------------------+    produces     +---------------------------------+
|   com.lexor.lexer   | --------------> |       com.lexor.parser          |
|                     |   List<Token>   |                                 |
|  Lexer              |                 |  Parser                         |
|  Token              |                 |  +-- com.lexor.parser.ast       |
|  TokenType          |                 |       ASTNode & all subclasses  |
+---------------------+                 +----------------+----------------+
                                                         |
                                                         | produces AST
                                                         v
                                        +---------------------------------+
                                        |    com.lexor.semantic            |
                                        |                                 |
                                        |  SemanticAnalyzer               |
                                        |  SymbolTable                    |
                                        +----------------+----------------+
                                                         |
                                                         | validated AST
                                                         v
                                        +---------------------------------+
                                        |    com.lexor.interpreter         |
                                        |                                 |
                                        |  Interpreter                    |
                                        |  Environment                    |
                                        |  LexorValue                     |
                                        +---------------------------------+

  All stages use:
  +-----------------------+     +-------------------------+
  |   com.lexor.visitor   |     |    com.lexor.error       |
  |  ASTVisitor<T>        |     |  LexorException          |
  +-----------------------+     |  ParseException          |
                                |  SemanticException       |
                                |  LexorRuntimeException   |
                                +-------------------------+

  Interactive mode:
  +-------------------------+
  |     com.lexor.repl      |
  |  ReplRunner             |  <-- uses JLine3 for line editing
  +-------------------------+
```

---

## Class Diagram

```
+----------------------------------------------------------------------+
|                         LEXER PACKAGE                                |
|                                                                      |
|  <<enum>>                 <<class>>            <<class>>             |
|  TokenType                Token                Lexer                 |
|  -------------            -------------        -----------------     |
|  KEYWORD_SCRIPT           -type:TokenType      -source:String        |
|  KEYWORD_START            -lexeme:String       -pos:int              |
|  KEYWORD_END              -line:int            -line:int             |
|  KEYWORD_DECLARE          -column:int          -----------------     |
|  KEYWORD_PRINT            -------------        +tokenize():          |
|  KEYWORD_SCAN             +getType()             List<Token>         |
|  KEYWORD_IF               +getLexeme()         -scanToken()          |
|  KEYWORD_ELSE             +getLine()           -isKeyword()          |
|  KEYWORD_FOR              +getColumn()         -readString()         |
|  KEYWORD_REPEAT                                -readNumber()         |
|  TYPE_INT                                                            |
|  TYPE_CHAR                                                           |
|  TYPE_BOOL                                                           |
|  TYPE_FLOAT                                                          |
|  INT_LITERAL                                                         |
|  FLOAT_LITERAL                                                       |
|  CHAR_LITERAL                                                        |
|  BOOL_LITERAL                                                        |
|  STRING_LITERAL                                                      |
|  IDENTIFIER                                                          |
|  OP_PLUS, OP_MINUS                                                   |
|  OP_MUL, OP_DIV, OP_MOD                                              |
|  OP_GT, OP_LT, OP_GTE                                                |
|  OP_LTE, OP_EQ, OP_NEQ                                               |
|  OP_AND, OP_OR, OP_NOT                                               |
|  AMPERSAND, DOLLAR                                                   |
|  LBRACKET, RBRACKET                                                  |
|  LPAREN, RPAREN                                                      |
|  COMMA, COLON, ASSIGN                                                |
|  NEWLINE, EOF                                                        |
+----------------------------------------------------------------------+

+----------------------------------------------------------------------+
|                        PARSER / AST PACKAGE                          |
|                                                                      |
|  <<abstract>>                                                        |
|  ASTNode                                                             |
|  -----------------                                                   |
|  #line:int                                                           |
|  +accept(v:ASTVisitor<T>):T                                          |
|         |                                                            |
|         +---> ProgramNode         -declarations: List<DeclarationNode>
|         |                         -statements:   List<ASTNode>       |
|         |                                                            |
|         +---> DeclarationNode     -type:String                       |
|         |                         -name:String                       |
|         |                         -initializer:ASTNode (nullable)    |
|         |                                                            |
|         +---> AssignmentNode      -targets:List<String>              |
|         |                         -value:ASTNode                     |
|         |                                                            |
|         +---> PrintNode           -segments:List<ASTNode>            |
|         |                                                            |
|         +---> ScanNode            -variables:List<String>            |
|         |                                                            |
|         +---> BinaryExprNode      -left:ASTNode                      |
|         |                         -operator:String                   |
|         |                         -right:ASTNode                     |
|         |                                                            |
|         +---> UnaryExprNode       -operator:String                   |
|         |                         -operand:ASTNode                   |
|         |                                                            |
|         +---> LiteralNode         -value:Object                      |
|         |                         -type:String                       |
|         |                                                            |
|         +---> VariableNode        -name:String                       |
|         |                                                            |
|         +---> IfNode              -condition:ASTNode                 |
|         |                         -thenBlock:List<ASTNode>           |
|         |                         -elseIfClauses:List<ElseIfClause>  |
|         |                         -elseBlock:List<ASTNode> (nullable)|
|         |                                                            |
|         +---> ForNode             -init:AssignmentNode               |
|         |                         -condition:ASTNode                 |
|         |                         -update:AssignmentNode             |
|         |                         -body:List<ASTNode>                |
|         |                                                            |
|         +---> RepeatNode          -condition:ASTNode                 |
|                                   -body:List<ASTNode>                |
|                                                                      |
|  <<class>>                                                           |
|  Parser                                                              |
|  -------------------------                                           |
|  -tokens:List<Token>                                                 |
|  -current:int                                                        |
|  -------------------------                                           |
|  +parse():ProgramNode                                                |
|  -parseDeclaration():DeclarationNode                                 |
|  -parseStatement():ASTNode                                           |
|  -parsePrint():PrintNode                                             |
|  -parseScan():ScanNode                                               |
|  -parseIf():IfNode                                                   |
|  -parseFor():ForNode                                                 |
|  -parseRepeat():RepeatNode                                           |
|  -parseAssignment():AssignmentNode                                   |
|  -parseExpression():ASTNode   <- entry for expr chain               |
|  -parseLogical():ASTNode                                             |
|  -parseRelational():ASTNode                                          |
|  -parseAdditive():ASTNode                                            |
|  -parseMultiplicative():ASTNode                                      |
|  -parseUnary():ASTNode                                               |
|  -parsePrimary():ASTNode                                             |
+----------------------------------------------------------------------+

+----------------------------------------------------------------------+
|                      VISITOR PACKAGE                                 |
|                                                                      |
|  <<interface>>                                                       |
|  ASTVisitor<T>                                                       |
|  ---------------------------------------------------                 |
|  +visitProgram(n:ProgramNode):T                                      |
|  +visitDeclaration(n:DeclarationNode):T                              |
|  +visitAssignment(n:AssignmentNode):T                                |
|  +visitPrint(n:PrintNode):T                                          |
|  +visitScan(n:ScanNode):T                                            |
|  +visitBinaryExpr(n:BinaryExprNode):T                                |
|  +visitUnaryExpr(n:UnaryExprNode):T                                  |
|  +visitLiteral(n:LiteralNode):T                                      |
|  +visitVariable(n:VariableNode):T                                    |
|  +visitIf(n:IfNode):T                                                |
|  +visitFor(n:ForNode):T                                              |
|  +visitRepeat(n:RepeatNode):T                                        |
|                                                                      |
|  Implementations:                                                    |
|     SemanticAnalyzer  implements  ASTVisitor<Void>                   |
|     Interpreter       implements  ASTVisitor<LexorValue>             |
+----------------------------------------------------------------------+

+----------------------------------------------------------------------+
|                      SEMANTIC PACKAGE                                |
|                                                                      |
|  <<class>>                      <<class>>                            |
|  SemanticAnalyzer               SymbolTable                          |
|  -----------------              ------------------                   |
|  -symbolTable:SymbolTable       -table:HashMap<String,TypeInfo>      |
|  -----------------              ------------------                   |
|  +analyze(p:ProgramNode)        +declare(name,type,line)             |
|  +visitDeclaration(...)         +lookup(name):TypeInfo               |
|  +visitAssignment(...)          +isDeclared(name):boolean            |
|  +visitBinaryExpr(...)                                               |
|  +visitIf(...)                                                       |
|  -checkTypeCompatibility(...)                                        |
+----------------------------------------------------------------------+

+----------------------------------------------------------------------+
|                     INTERPRETER PACKAGE                              |
|                                                                      |
|  <<class>>                                                           |
|  LexorValue                                                          |
|  -------------------------                                           |
|  -type:String  (INT|CHAR|BOOL|FLOAT)                                 |
|  -value:Object                                                       |
|  -------------------------                                           |
|  +asInt():int                                                        |
|  +asFloat():float                                                    |
|  +asBool():boolean                                                   |
|  +asChar():char                                                      |
|  +toString():String                                                  |
|                                                                      |
|  <<class>>                                                           |
|  Environment                                                         |
|  -------------------------                                           |
|  -store:HashMap<String,LexorValue>                                   |
|  -parent:Environment (nullable, for nested scopes)                   |
|  -------------------------                                           |
|  +define(name:String, val:LexorValue)                                |
|  +assign(name:String, val:LexorValue)                                |
|  +get(name:String):LexorValue                                        |
|  +createChildScope():Environment                                     |
|                                                                      |
|  <<class>>                                                           |
|  Interpreter  implements ASTVisitor<LexorValue>                      |
|  -------------------------                                           |
|  -environment:Environment                                            |
|  -output:PrintStream                                                 |
|  -input:Scanner                                                      |
|  -------------------------                                           |
|  +interpret(p:ProgramNode)                                           |
|  +visitDeclaration(...)                                              |
|  +visitAssignment(...)                                               |
|  +visitPrint(...)    <- handles &, $, escape codes [x]              |
|  +visitScan(...)                                                     |
|  +visitBinaryExpr(...) <- arithmetic, relational, logical            |
|  +visitUnaryExpr(...)                                                |
|  +visitIf(...)                                                       |
|  +visitFor(...)                                                      |
|  +visitRepeat(...)                                                   |
|  -evaluate(node:ASTNode):LexorValue                                  |
|  -resolveEscapeCode(raw:String):String                               |
+----------------------------------------------------------------------+

+----------------------------------------------------------------------+
|                       ERROR PACKAGE                                  |
|                                                                      |
|  RuntimeException                                                    |
|       +---> LexorException(message, line, column)                    |
|               +---> ParseException                                   |
|               +---> SemanticException                                |
|               +---> LexorRuntimeException                            |
+----------------------------------------------------------------------+
```

---

## Activity Diagram

```
===================================================================
              LEXOR INTERPRETER - FULL EXECUTION FLOW
===================================================================

  +---------------------------------------------+
  |               Program Start                  |
  |   (java -jar lexor.jar [file | --repl])      |
  +---------------------+-----------------------+
                        |
              +---------v------------------+
              |  File mode or REPL?        |
              +--+-------------------------+
          File <-+                 +-> REPL
           |                          |
           |                    +-----v----------------------+
           |                    |  Display prompt via        |
           |                    |  JLine3 terminal           |
           |                    |  Read one line/block       |
           |                    +----------------------------+
           |                          |
           +----------+---------------+
                      |  Source text available
                      v
          +-------------------------+
          |     LEXER PHASE          |
          |  Scan characters        |
          |  Match token patterns   |
          |  Strip %% comments      |
          |  Emit Token list        |
          +------------+------------+
                       |
              +--------v--------+
              |  Lexer error?   |
              +-+---------------+
           Yes |         No |
               |             v
    +----------v-----+  +------------------------+
    |  Report error  |  |      PARSER PHASE       |
    |  with line/col |  |  Recursive descent     |
    +----------------+  |  Build AST nodes       |
                        |  Check structure       |
                        +------------+-----------+
                                     |
                            +--------v--------+
                            |  Parse error?   |
                            +-+---------------+
                         Yes |         No |
                             |             v
                  +----------v-----+  +------------------------+
                  |  Report error  |  |   SEMANTIC PHASE        |
                  +----------------+  |  Walk AST              |
                                      |  Check declarations    |
                                      |  Validate types        |
                                      |  Verify bool predicates|
                                      +------------+-----------+
                                                   |
                                          +--------v--------+
                                          |  Semantic error?|
                                          +-+---------------+
                                       Yes |         No |
                                           |             v
                                +----------v-----+  +-----------------------+
                                |  Report error  |  |  INTERPRETER PHASE    |
                                +----------------+  |  Walk validated AST   |
                                                    |  Maintain Environment |
                                                    +----------+------------+
                                                               |
                                                    +----------v------------+
                                                    |  For each statement:  |
                                                    |  +----------------+   |
                                                    |  | DECLARE?       |   |
                                                    |  | -> init in Env |   |
                                                    |  +----------------+   |
                                                    |  | ASSIGN?        |   |
                                                    |  | -> eval+store  |   |
                                                    |  +----------------+   |
                                                    |  | PRINT?         |   |
                                                    |  | -> eval concat |   |
                                                    |  | -> write stdout|   |
                                                    |  +----------------+   |
                                                    |  | SCAN?          |   |
                                                    |  | -> read stdin  |   |
                                                    |  | -> coerce+store|   |
                                                    |  +----------------+   |
                                                    |  | IF?            |   |
                                                    |  | -> eval cond   |   |
                                                    |  | -> exec branch |   |
                                                    |  +----------------+   |
                                                    |  | FOR?           |   |
                                                    |  | -> init; loop  |   |
                                                    |  | -> run body    |   |
                                                    |  | -> update; rep |   |
                                                    |  +----------------+   |
                                                    |  | REPEAT WHEN?   |   |
                                                    |  | -> eval cond   |   |
                                                    |  | -> run body    |   |
                                                    |  | -> re-eval cond|   |
                                                    |  +----------------+   |
                                                    +----------+------------+
                                                               |
                                                    +----------v------------+
                                                    |  Runtime error?       |
                                                    +-+---------------------+
                                                   Yes |             No |
                                                       |                v
                                              +--------v-------+  +-----v----------+
                                              | Report runtime |  | Program ends   |
                                              | error + line   |  | cleanly        |
                                              +----------------+  +-----+----------+
                                                                        |
                                                             +----------v------+
                                                             |  REPL mode?     |
                                                             +-+---------------+
                                                          Yes |          No -> Exit
                                                              |
                                                    +---------v----------+
                                                    |  Loop back to      |
                                                    |  prompt (JLine3)   |
                                                    +--------------------+
```

---

*End of LEXOR Interpreter Design Document*
