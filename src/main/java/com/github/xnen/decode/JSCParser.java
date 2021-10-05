package com.github.xnen.decode;

import java.util.List;

public class JSCParser {
    public static JSCClass parse(JSCHandler jscHandler, String className, List<String> lines) {
        boolean firstLine = true;

        JSCClass parsed = new JSCClass(className);

        int lineNumber = 0;
        for (String line : lines) {
            if (line.trim().length() > 0) {
                lineNumber++;
            }

            if (lineNumber == 1 && line.startsWith("pkg ") && line.endsWith(";")) {
                parsed.setPackage(line.substring("pkg ".length(), line.length() - 1));
                continue;
            }

            // First line identifiers -- i.e. #!abstract<K> ext Thread
            if (lineNumber <= 2 && line.startsWith("#!")) {
                int offset = "#!".length();

                // Parse CLASS TYPE
                if (line.startsWith("interface", offset)) {
                    parsed.setModifier(JSCClass.C_MODIFIER.INTERFACE);
                    offset += "interface".length();
                } else if (line.startsWith("abstract", offset)) {
                    parsed.setModifier(JSCClass.C_MODIFIER.ABSTRACT);
                    offset += "abstract".length();
                } else if (line.startsWith("class", offset)) {
                    // Default CLASS
                    offset += "class".length();
                }

                // Parse GENERIC TYPE CLASS PROPERTIES
                if (line.charAt(offset) == '<') {
                    String GT = line.substring(1 + offset, line.indexOf('>'));
                    parsed.setGenericTypes(GT.split(","));
                    offset += 2 + GT.length();
                }

                // Parse EXTENDING CLASS
                String extendingTrimCompare = line.substring(offset);
                if (extendingTrimCompare.trim().startsWith("ext ")) {
                    offset += "ext ".length() + (extendingTrimCompare.length() - extendingTrimCompare.trim().length());
                    String extLine = line.substring(offset);

                    String extendingClass;
                    if (extLine.contains(" ")) {
                        extendingClass = extLine.substring(0, extLine.indexOf(' '));
                    } else {
                        extendingClass = extLine;
                    }

                    parsed.setExtendingClass(extendingClass);
                    offset += extendingClass.length();
                }

                // Parse IMPLEMENTING CLASSES
                String implementsTrimCompare = line.substring(offset);
                if (implementsTrimCompare.trim().startsWith("impl ")) {
                    offset += "impl ".length() + (implementsTrimCompare.length() - implementsTrimCompare.trim().length());
                    parsed.setImplementingClasses(line.substring(offset).split(","));
                }

                // Continue, since this isn't valid Java code to be parsed.
                continue;
            }

            // Parse imports
            if (line.startsWith("import ") && line.endsWith(";")) {
                if (line.startsWith("import static ")) {
                    parsed.addStaticImport(line.substring("import static ".length(), line.lastIndexOf(";")));
                } else {
                    parsed.addImport(new JavaImport(line.substring("import ".length(), line.lastIndexOf(';'))));
                }

                continue;
            }

            // Import while specifying a library to use
            if (line.startsWith("from ") && line.contains(" import ") && line.endsWith(";")) {
                if (line.startsWith("from \"")) {
                    String libLine = line.substring("from ".length() + 1);
                    String library = libLine.substring(0, libLine.indexOf('"'));
                    String impLine = libLine.substring(library.length() + 2 + "import ".length());
                    parsed.addImport(new JavaImport(impLine.substring(0, impLine.lastIndexOf(";")), library));
                } else {
                    // TODO: Sloppy fix, clean up (Test if quotes exist and handle them as one regardless of spaces)
                    String libLine = line.substring("from ".length());
                    String library = libLine.substring(0, libLine.indexOf(' '));
                    String impLine = libLine.substring(library.length() + 1 + "import ".length());
                    parsed.addImport(new JavaImport(impLine.substring(0, impLine.lastIndexOf(";")), library));
                }
                continue;
            }

            // Macros
            if (line.startsWith("!macro")) {
                line = line.substring(1 + "!macro".length());
                String[] resultBuffer = { "", "" };

                // Basically, everything after first in-escaped quote gets added as key
                // Then, everything after third in-escaped quote gets added as value.

                int qId = 0; char pChar = 0;
                for (char c : line.toCharArray()) {
                    if (c == '"' && pChar != '\\') qId++;
                    if (qId % 2 == 1) resultBuffer[qId / 3] += c;
                    pChar = c;
                }

                parsed.addMacro(resultBuffer);
                continue;
            }

            // Parse other JSC file imports
            if (line.startsWith("using ") && line.contains("\"") && line.endsWith(";")) {
                String usingLine = line.substring("using ".length() + 1);
                String fileName = usingLine.substring(0, usingLine.lastIndexOf("\""));
                if (!fileName.substring(0, fileName.lastIndexOf(".")).equalsIgnoreCase(parsed.getClassName())) {
                    // Start handling new JSC file
                    jscHandler.handle(fileName);
                }
                continue;
            }

            // Add another JSC file's functions to this class.
            if (line.startsWith("ext ") && line.contains("\"") && line.endsWith(";")) {
                String extLine = line.substring("ext ".length() + 1);
                String fileName = extLine.substring(0, extLine.lastIndexOf("\""));
                if (!fileName.substring(0, fileName.lastIndexOf(".")).equalsIgnoreCase(parsed.getClassName())) {
                    parsed.addMethodExtension(fileName);
                }
                continue;
            }

            if (isClassScopeIdentifier(line)) {
                if (line.startsWith("svoid")) {
                    line = "static " + line.substring(1);
                }
                if (line.startsWith("cvar")) {
                    line = line.substring(4);
                }
                parsed.addClassScope(line);
            } else {
                parsed.addMainScope(line);
            }
        }

        return parsed;
    }

    private static boolean isClassScopeIdentifier(String s) {
        for (String s0 : class_scope_identifiers) {
            if (s.trim().startsWith(s0)) {
                return true;
            }
        }

        return s.trim().startsWith("@");
    }

    static final String[] class_scope_identifiers = {
            "abstract"
            , "class"
            , "default"
            , "enum"
            , "native"
            , "private"
            , "protected"
            , "public"
            , "static"
            , "transient"
            , "void"
            , "volatile"
            , "svoid"
            , "cvar"
    };
}
