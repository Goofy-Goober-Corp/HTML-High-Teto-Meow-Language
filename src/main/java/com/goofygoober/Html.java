package com.goofygoober;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Html {
    private static Map<String, String> vars = new HashMap<>();
    private static Scanner inputScanner = new Scanner(System.in);

    private static List<String> tokenize(String code) {
        Pattern pattern = Pattern.compile(
            "(<\\/?[a-z]+>|" +
            "<if\\s+(?:\\{[a-zA-Z\\d]+\\}|\\d+)\\s?(?:==|!=|>|<)\\s?(?:\\{[a-zA-Z\\d]+\\}|\\d+)\\s?>|" +
            "</if>|" +
            "<math\\s+[^>]+>|" +
            "</math>|" +
            "<set\\s+(?:[a-zA-Z\\d]+)\\s?=\\s?(?:[a-zA-Z\\d]+|(?:<in\\s+(?:[a-zA-Z\\d]+)>|<in>))\\s?>|" +
            "<loop>|" +
            "</loop>|" +
            "<break>|" +
            "[^<]+)"
        );
        Matcher matcher = pattern.matcher(code);

        List<String> tokens = new ArrayList<>();
        while(matcher.find()) {
            String token = matcher.group().trim();
            if(!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static String replaceVar(String text) {
        for(Map.Entry<String, String> entry : vars.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return text;
    }

    private static boolean evaluateCondition(String condition) {
        // condition = condition.replaceAll("[<>]", "").trim();
        condition = replaceVar(condition);
        // System.out.println(condition);

        if(condition.contains("==")) {
            String[] parts = condition.split("==");
            return parts[0].trim().equals(parts[1].trim());
        } else if(condition.contains("!=")) {
            String[] parts = condition.split("!=");
            return !parts[0].trim().equals(parts[1].trim());
        } else if(condition.contains(">")) {
            String[] parts = condition.split(">");
            return Float.parseFloat(parts[0].replaceAll("\\s+", "")) > Float.parseFloat(parts[1].replaceAll("\\s+", ""));
        } else if(condition.contains("<")) {
            String[] parts = condition.split("<");
            return Float.parseFloat(parts[0].replaceAll("\\s+", "")) < Float.parseFloat(parts[1].replaceAll("\\s+", ""));
        }
        return false;
    }

    private static void processSetTag(String setTag) {
        String content = setTag.substring(5, setTag.length() - 1).trim();
        String[] parts = content.split("=");
        
        if(parts.length == 2) {
            String varName = parts[0].trim();
            if (parts[1].replaceAll("\\s+", "").startsWith("<in")) {
                String varValue = input();
                vars.put(varName, varValue);
            } else {
                String varValue = parts[1].trim().replace("\"", "");
                varValue = replaceVar(varValue);
                vars.put(varName, varValue);
            }
        }
    }

    private static String evaluateMath(String mathExpression) {
        try {
            mathExpression = replaceVar(mathExpression);
            
            String[] tokens = mathExpression.split("\\s+");
            if (tokens.length < 3) {
                return "ERROR: Invalid expression";
            }
            
            double result = 0;
            double left = Double.parseDouble(tokens[0]);
            String operator = tokens[1];
            double right = Double.parseDouble(tokens[2]);
            
            switch (operator) {
                case "+":
                    result = left + right;
                    break;
                case "-":
                    result = left - right;
                    break;
                case "*":
                    result = left * right;
                    break;
                case "/":
                    if (right == 0) return "ERROR: Division by zero";
                    result = left / right;
                    break;
                case "%":
                    result = left % right;
                    break;
                case "^":
                    result = Math.pow(left, right);
                    break;
                default:
                    return "ERROR: Unknown operator: " + operator;
            }
            
            if (result == (int) result) {
                return String.valueOf((int) result);
            } else {
                return String.valueOf(result);
            }
            
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private static void processMathTag(String mathTag, Stack<String> data, boolean saveToVar) {
        String expression = mathTag.substring(6, mathTag.length() - 1).trim();
        
        if (saveToVar && expression.contains("=")) {
            String[] parts = expression.split("=", 2);
            if (parts.length == 2) {
                String varName = parts[0].trim();
                String mathExpr = parts[1].trim();
                String result = evaluateMath(mathExpr);
                vars.put(varName, result);
                return;
            }
        }
        
        String result = evaluateMath(expression);
        data.push(result);
    }

    public static int execute(List<String> tokens) {
        Stack<String> data = new Stack<>();
        Stack<Boolean> condition = new Stack<>();
        Stack<Integer> loopStartIndices = new Stack<>();
        Stack<List<String>> loopTokens = new Stack<>();
        Stack<Integer> loopCounters = new Stack<>();

        boolean inParagraph = false;
        boolean execBlock = true;
        boolean breakLoop = false;

        for(int i = 0; i < tokens.size(); i++) {
            if (breakLoop) {
                if (tokens.get(i).equals("</loop>") && !loopStartIndices.isEmpty()) {
                    breakLoop = false;
                    loopStartIndices.pop();
                    loopTokens.pop();
                    loopCounters.pop();
                }
                continue;
            }

            String token = tokens.get(i);

            if(token.startsWith("<set")) {
                if(execBlock) {
                    processSetTag(token);
                }
                continue;
            }

            if(token.startsWith("<if")) {
                // System.out.println(token);
                String cond = token.substring(4, token.length() - 1).trim();
                boolean condResult = evaluateCondition(cond);
                condition.push(condResult);
                execBlock = condResult;
                continue;
            }

            if(token.equals("</if>")) {
                if(!condition.isEmpty()) {
                    condition.pop();
                    execBlock = condition.isEmpty() || condition.peek();
                }
                continue;
            }

            if(token.startsWith("<math")) {
                if(execBlock) {
                    processMathTag(token, data, !inParagraph);
                }
                continue;
            }

            if(token.equals("<loop>")) {
                if(execBlock) {
                    loopStartIndices.push(i);
                    loopTokens.push(new ArrayList<>());
                    loopCounters.push(0);
                }
                continue;
            }

            if(token.equals("</loop>")) {
                if(execBlock && !loopStartIndices.isEmpty()) {
                    List<String> currentLoopTokens = loopTokens.peek();
                    int counter = loopCounters.pop() + 1;
                    loopCounters.push(counter);
                    
                    if (counter > 1) {
                        currentLoopTokens.add(token);
                    }
                    
                    i = loopStartIndices.peek();
                    
                    if (counter == 1) {
                        continue;
                    }
                }
                continue;
            }

            if(token.equals("<break>")) {
                if(execBlock && !loopStartIndices.isEmpty()) {
                    breakLoop = true;
                }
                continue;
            }

            if(execBlock) {
                if(!loopStartIndices.isEmpty() && loopCounters.peek() == 0) {
                    loopTokens.peek().add(token);
                }

                switch(token) {
                    case "<p>":
                        inParagraph = true;
                        data.clear();
                        break;

                    case "</p>":
                        inParagraph = false;
                        if(!data.isEmpty()) {
                            String content = String.join(" ", data);
                            content = replaceVar(content);
                            System.out.println(content);
                            data.clear();
                        }
                        break;

                    default:
                        if(inParagraph) {
                            data.push(token);
                        }
                        break;
                }
            }
        }

        if(!data.isEmpty()) {
            String content = String.join(" ", data);
            content = replaceVar(content);
            System.out.println(content);
        }

        return 0;
    }

    public static String input() {
        return inputScanner.nextLine();
    }

    public static void readFile(File file) {
        try {
            Scanner scanner = new Scanner(file);
            StringBuilder sources = new StringBuilder();

            while (scanner.hasNextLine()) {
                sources.append(scanner.nextLine()).append("\n");
            }
            scanner.close();

            execute(tokenize(sources.toString()));
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java Html <filename>");
            return;
        }
        File file = new File(args[0]);
        readFile(file);

        inputScanner.close();
    }
}