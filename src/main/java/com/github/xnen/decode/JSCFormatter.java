package com.github.xnen.decode;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class JSCFormatter {
    public static List<String> format(File jscFile) {
        if (!jscFile.exists()) {
            System.out.println("File not found: " + jscFile.getName() + ".");
            System.exit(-1);
        }

        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(jscFile))) {
            boolean inMultiLineComment = false;
            int bracketScopeIndex = 0;

            StringBuilder lineBuffer = new StringBuilder();

            while (reader.ready()) {
                String cLine = reader.readLine();
                char pChar = 0;

                boolean inQuoteScope = false;
                boolean inComment = inMultiLineComment;

                if (!inComment && bracketScopeIndex == 0 && lineBuffer.length() > 0) {
                    lines.add(lineBuffer.toString());
                    lineBuffer = new StringBuilder();
                }

                for (char c : cLine.toCharArray()) {
                    if (pChar == '/' && c == '/') {
                        inComment = true;
                        // Remove last character from lineBuffer as we aren't writing comments.
                        lineBuffer = new StringBuilder(lineBuffer.substring(0, lineBuffer.length() - 1));
                    }

                    if (pChar == '/' && c == '*') {
                        inMultiLineComment = inComment = true;
                    }

                    if (pChar == '*' && c == '/' && inMultiLineComment) {
                        inMultiLineComment = inComment = false;
                        // Remove last character from lineBuffer since we aren't writing comments.
                        lineBuffer = new StringBuilder(lineBuffer.substring(0, lineBuffer.length() - 1));
                        // Continue, since we aren't in a comment anymore, and we don't want to write the current char to the lineBuffer.
                        continue;
                    }

                    if (pChar == '\\' && !inComment) {
                        if (c == '"') {
                            inQuoteScope = !inQuoteScope;
                        }
                    }

                    if (!inComment && !inQuoteScope) {
                        if (c == '{') {
                            bracketScopeIndex++;
                        } else if (c == '}') {
                            bracketScopeIndex--;
                        }
                    }

                    // Not in comment, so we can write our current character to the buffer.
                    if (!inComment) {
                        lineBuffer.append(c);
                    }

                    // Every semi-colon needs formatted as 'its own line' in Java.
                    if (c == ';' && bracketScopeIndex == 0 && !inComment && !inQuoteScope) {
                        lines.add(lineBuffer.toString());
                        lineBuffer = new StringBuilder();
                    }

                    pChar = c;
                }
            }

            // Final add
            lines.add(lineBuffer.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lines;
    }
}
