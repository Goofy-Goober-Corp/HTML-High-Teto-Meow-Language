package com.goofy_goober.html;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class html {

    private static Map<String, String> vars = new HashMap<>();

    private static List<String> tokenize(String code)
    {
        Pattern pattern = Pattern.compile("(<\\/?[a-z]+>|<if\\s+[^>]+>|</if>|<set\\s+[^>]+>|[^<]+)");
        Matcher matcher = pattern.matcher(code);

        List<String> tokens = new ArrayList<>();
        while(matcher.find())
        {
            String token = matcher.group().trim();
            if(!token.isEmpty())
            {
                tokens.add(token);
            }
        }

        return tokens;
    }
    

    private static String replaceVar(String text)
    {
        for(Map.Entry<String, String> entry : vars.entrySet())
        {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return text;
    }

    private static boolean evaluateCondition(String condition)
    {
        condition = condition.replaceAll("[<>]", "").trim();
        condition = replaceVar(condition);

        if(condition.contains("=="))
        {
            String[] parts = condition.split("==");
            return parts[0].trim().equals(parts[1].trim());
        }
        else if(condition.contains("!="))
        {
            String[] parts = condition.split("!=");
            return !parts[0].trim().equals(parts[1].trim());
        }

        return false;
    }

    private static void proccessSetTag(String setTag)
    {
        String content = setTag.substring(5, setTag.length() - 1).trim();
        String[] parts = content.split("=");

        if(parts.length == 2)
        {
            String varName = parts[0].trim();
            String varValue = parts[1].trim().replace("\"", "");
            
            vars.put(varName, varValue);
        }
    }

    public static int execute(List<String> tokens)
    {
        Stack<String> stack = new Stack<>();
        Stack<String> data = new Stack<>();
        Stack<Boolean> condition = new Stack<>();

        boolean inParagraph = false;
        boolean inCondition = false;
        boolean execBlock = true;

        for(String token : tokens) 
        {
            if(token.startsWith("<set"))
            {
                proccessSetTag(token);
                continue;
            }

            if(token.startsWith("<if"))
            {
                inCondition = true;
        
                String cond = token.substring(4, token.length() - 1).trim();
                boolean condResult = evaluateCondition(cond);

                condition.push(condResult);
                execBlock = condResult;

                continue;
            }

            if(token.equals("</if>"))
            {
                inCondition = false;

                if(!condition.isEmpty())
                {
                    condition.pop();
                    execBlock = condition.isEmpty() || condition.peek();
                }

                continue;
            }

            if(execBlock)
            {
                switch(token) 
                {
                    case "<p>":
                        inParagraph = true;
                        data.clear();
                        break;

                    case "</p>":
                        inParagraph = false;

                        if(!data.isEmpty())
                        {
                            String content = String.join(" ", data);
                            content = replaceVar(content);
                            System.out.println(content);

                            data.clear();
                        }
                        break;

                    default:
                        if(inParagraph)
                        {
                            data.push(token);
                        }
                        else
                        {
                            stack.push(token);
                        }

                        break;
                }
            }
        }

        for(String forPrint : data)
        {
            String content = String.join(" ", data);
            content = replaceVar(content);
            System.out.println(content);
        }

        return 0;
    }

    public static void readFile(File file) {
        try {
            Scanner scanner = new Scanner(file);
            String sources = "";

            while (scanner.hasNextLine()) {
                sources += scanner.nextLine() + "\n";
            }
            scanner.close();

            execute(tokenize(sources));
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        
    }

    public static void main(String[] args) {
        File file = new File("example.html");
        readFile(file);
    }
}