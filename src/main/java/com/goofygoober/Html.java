package com.goofygoober;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Html {
    private static Map<String, String> vars = new HashMap<>();

    private static List<String> tokenize(String code) {
        Pattern pattern = Pattern.compile(
            "(<\\/?[a-z]+>|" +
            "<if\\s+[^>]+>|" +
            "</if>|" +
            "<math\\s+[^>]+>|" +
            "</math>|" +
            "<set\\s+(?:[a-zA-Z\\d]+)\\s?=\\s?(?:[a-zA-Z\\d]+|(?:<in\\s+(?:[a-zA-Z\\d]+)>|<in>))\\s?>|" +
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
        condition = condition.replaceAll("[<>]", "").trim();
        condition = replaceVar(condition);

        if(condition.contains("==")) {
            String[] parts = condition.split("==");
            return parts[0].trim().equals(parts[1].trim());
        } else if(condition.contains("!=")) {
            String[] parts = condition.split("!=");
            return !parts[0].trim().equals(parts[1].trim());
        }
        return false;
    }

    private static void processSetTag(String setTag) {
        String content = setTag.substring(5, setTag.length() - 1).trim();
        String[] parts = content.split("=");
        // System.out.println(parts[1]);

        if(parts.length == 2) {
            String varName = parts[0].trim();
            if (parts[1].startsWith("<in") || parts[1].startsWith(" <in")) {
                String varValue = input();
                vars.put(varName, varValue);
            } else {
                String varValue = parts[1].trim().replace("\"", "");
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

        boolean inParagraph = false;
        boolean execBlock = true;

        for(String token : tokens) {
            if(token.startsWith("<set")) {
                processSetTag(token);
                continue;
            }

            if(token.startsWith("<if")) {
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

            // if(token.startsWith("<in")) {
            //     if(execBlock) {
            //         try {
            //             String content = token.substring(4, token.length() - 1).trim();
            //         } catch(StringIndexOutOfBoundsException e) {
            //             e.printStackTrace();
            //         }
            //         input();
            //     }
            // }

            if(execBlock) {
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
        Scanner inputScanner = new Scanner(System.in);
        String fuck = inputScanner.nextLine();
        // System.out.println(fuck);
        inputScanner.close();
        return fuck;
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
        File file = new File(args[0]);
        readFile(file);
    }
}
