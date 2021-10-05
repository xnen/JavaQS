package com.github.xnen.decode;
import com.github.xnen.App;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSCHandler {
    private final String mainClass;
    private final JSCClass mainJSCObj;

    List<JSCClass> classList = new ArrayList<>();

    public JSCHandler(String mainClass) {
        this.mainClass = mainClass;
        mainJSCObj = handle(this.mainClass);
    }

    JSCClass handle(String className) {
        List<String> lines = JSCFormatter.format(new File(className));
        JSCClass clazz = JSCParser.parse(this, className, lines);
        this.classList.add(clazz);
        return clazz;
    }

    public void writeClasses(Path tmpDir) {
        File srcDir = new File(tmpDir + File.separator + "src");
        File binDir = new File(tmpDir + File.separator + "bin");

        if (!srcDir.exists()) {
            if (!srcDir.mkdir()) {
                System.out.println("Could not create src directory.");
                System.exit(-1);
            }
        }
        if (!binDir.exists()) {
            if (!binDir.mkdir()) {
                System.out.println("Could not create bin directory.");
                System.exit(-1);
            }
        }

        for (JSCClass jscClass : this.classList) {
            File targetDir = srcDir;
            if (jscClass.getPackage() != null) {
                StringBuilder path = new StringBuilder(srcDir.getAbsolutePath() + File.separator);
                for (String s : jscClass.getPackage().split("\\.")) {
                    path.append(s).append(File.separator);
                }
                targetDir = new File(path.toString());
            }

            if (!targetDir.exists()) {
                if (!targetDir.mkdirs()) {
                    System.out.println("Could not create package dirs for jsc " + jscClass.getClassName());
                    System.exit(-1);
                }
            }

            try (FileWriter writer = new FileWriter(targetDir.getAbsolutePath() + File.separator + jscClass.getClassName() + ".java")) {
                List<String> cLines = jscClass.toJavaClass(this);
                for (String line : cLines) {
                    writer.write(applyMacro(jscClass, line) + System.lineSeparator());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String applyMacro(JSCClass jscClass, String line) {
        for (String key : jscClass.getMacros().keySet()) {
            key = key.replace("\\\"", "\"");
            key = key.replace("\\\\", "\\");
            String val = jscClass.getMacros().get(key);
            val = val.replace("\\\\", "\\");
            line = line.replace(key, val);
        }
        return line;
    }

    public boolean compileAll(Path tmpDir) {
        Path JAVAC_LOCATION = Paths.get(App.getInstance().getSettings().JDK_LOCATION + File.separator + "bin" + File.separator);

        List<String> javac_args = new ArrayList<>();

        javac_args.add(JAVAC_LOCATION + File.separator + "javac");
        javac_args.add("-nowarn");

        List<String> libs = gatherLibs();
        if (libs.size() > 0) {
            javac_args.add("-classpath");
            StringBuilder sb = new StringBuilder();
            sb.append(".");
            for (String s : libs) {
                sb.append(App.getInstance().getOs().toLowerCase(Locale.ROOT).contains("windows") ? ";" : ":").append(s);
            }
            javac_args.add(sb.toString());
        }

        javac_args.add("-d");
        javac_args.add(tmpDir + File.separator + "bin");

        Path srcDir = Paths.get(tmpDir + File.separator + "src");

        for (JSCClass jscClass : this.classList) {
            StringBuilder jscClassPath = new StringBuilder();
            jscClassPath.append(srcDir).append(File.separator);
            if (jscClass.getPackage() != null) {
                jscClassPath.append(jscClass.getPackage().replace(".", File.separator)).append(File.separator);
            }
            jscClassPath.append(jscClass.getClassName()).append(".java");
            javac_args.add(jscClassPath.toString());
        }


        String[] args = javac_args.toArray(new String[0]);

        try {
            Process compiler = Runtime.getRuntime().exec(args);
            compiler.waitFor(2000L, TimeUnit.MILLISECONDS);

            InputStream err = compiler.getErrorStream();
            Scanner sc = new Scanner(err);

            Pattern pattern = Pattern.compile("(.+)(/)(.+)(:)(\\d+)(:)(.+)(error)");
            while (sc.hasNextLine()) {
                String s = sc.nextLine();
                Matcher m = pattern.matcher(s);
                if (m.find()) {
                    if (m.groupCount() == 8) {
                        boolean result = this.correctImportByLineNumber(m.group(1) + File.separator + m.group(3), m.group(5));
                        if (result) {
                            // Recur with new JavaImport in place.
                            this.writeClasses(tmpDir);
                            return this.compileAll(tmpDir);
                        } else {
                            if (!this.looseCorrectImport(s)) {
                                System.out.println("Errors in JSC:");
                                System.out.println("===============");
                                System.out.println(s);
                                while (sc.hasNextLine()) {
                                    System.out.println(sc.nextLine());
                                }
                                System.exit(-1);
                            } else {
                                // Also check that the loose correction didn't fix it.
                                this.writeClasses(tmpDir);
                                return this.compileAll(tmpDir);
                            }
                        }
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }

    private boolean looseCorrectImport(String rawError) {
        for (JSCClass jscClass : this.classList) {
            for (JavaImport javaImport : jscClass.getImports()) {
                if (rawError.contains(" " + javaImport.getRaw() + " ")) {
                    return javaImport.increment();
                }
            }
        }
        return false;
    }

    private boolean correctImportByLineNumber(String className, String lineNumber) throws IOException {
        Optional<String> lines = Files.lines(Paths.get(className)).skip(Long.parseLong(lineNumber) - 1).findFirst();
        if (lines.isPresent()) {
            String line = lines.get();
            for (JSCClass jscClass : this.classList) {
                for (JavaImport javaImport : jscClass.getImports()) {
                    if (line.endsWith(javaImport.getCurrentImport() + ";")) {
                        return javaImport.increment();
                    }
                }
            }
        }

        return false;
    }

    public List<String> gatherLibs() {
        List<String> libs = new ArrayList<>();
        for (JSCClass jscClass : this.classList) {
            for (JavaImport javaImport : jscClass.getImports()) {
                if (javaImport.getCurrentLibrary() != null) {
                    if (!libs.contains(javaImport.getCurrentLibrary())) {
                        libs.add(javaImport.getCurrentLibrary());
                    }
                }
            }
        }
        return libs;
    }

    public boolean pack(Path tempDir) {
        Path JDK_BIN_LOC = Paths.get(App.getInstance().getSettings().JDK_LOCATION + File.separator + "bin" + File.separator);
        File binFiles = new File(tempDir + File.separator + "bin");

        List<String> jarArgs = new ArrayList<>();
        jarArgs.add(JDK_BIN_LOC + File.separator + "jar");

        jarArgs.add("cfm");
        jarArgs.add("JSCRuntime.jar");

        try {
            String mcName;

            if (this.mainJSCObj.getPackage() != null) {
                mcName = this.mainJSCObj.getPackage() + "." + this.mainJSCObj.getClassName();
            } else {
                mcName = this.mainJSCObj.getClassName();
            }

            FileWriter writer = new FileWriter(tempDir + File.separator + "temp.mf");
            writer.write("Main-Class: " + mcName + System.lineSeparator());
            StringBuilder cp = new StringBuilder();
            boolean b = false;
            for (String s : gatherLibs()) {
                if (b) cp.append(" ");
                cp.append(s.replace(" ", "%20"));
                b = true;
            }
            writer.write("Class-Path: " + cp + System.lineSeparator());
            writer.close();

            jarArgs.add(tempDir + File.separator + "temp.mf");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        for (JSCClass jscClass : this.classList) {
            jarArgs.add("-C");
            jarArgs.add(binFiles.getAbsolutePath());
            String targetPath = "";

            if (jscClass.getPackage() != null) {
                jarArgs.add(jscClass.getPackage().replace(".", File.separator) + File.separator + jscClass.getClassName() + ".class");
                targetPath += jscClass.getPackage().replace(".", File.separator) + File.separator;
            } else {
                jarArgs.add(jscClass.getClassName() + ".class");
            }

            int i = 0;
            File file = new File (binFiles.getAbsolutePath() + File.separator + targetPath + jscClass.getClassName() + "$" + i + ".class");
            while (file.exists() || i < 1) {
                if (file.exists()) {
                    jarArgs.add("-C");
                    jarArgs.add(binFiles.getAbsolutePath());
                    jarArgs.add(targetPath + jscClass.getClassName() + "$" + i + ".class");
                }
                i++;
                file = new File (binFiles.getAbsolutePath() + File.separator + targetPath + jscClass.getClassName() + "$" + i + ".class");
                Thread.yield();
            }
        }



        try {
            Process jar = Runtime.getRuntime().exec(jarArgs.toArray(new String[0]));
            jar.waitFor();

            InputStream err = jar.getErrorStream();
            Scanner sc = new Scanner(err);
            if (sc.hasNextLine()) {
                System.out.println("ERRORS WHILE CREATING ARCHIVE:");
            }

            boolean errored = false;
            while (sc.hasNextLine()) {
                System.out.println(sc.nextLine());
                errored = true;
            }

            return !errored;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }
}
