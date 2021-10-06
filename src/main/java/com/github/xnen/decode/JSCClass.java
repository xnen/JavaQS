package com.github.xnen.decode;

import com.github.xnen.App;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSCClass {
    public void setGenericTypes(String[] genericTypes) {
        this.genericTypes = genericTypes;
    }

    public void setExtendingClass(String extendClass) {
        this.extendClass = extendClass;
    }

    public void setImplementingClasses(String[] implClasses) {
        this.implClasses = implClasses;
    }

    public void addImport(JavaImport javaImport) {
        this.imports.add(javaImport);
    }

    public void addStaticImport(String staticImport) {
        this.staticImports.add(staticImport);
    }

    public void addMacro(String[] strings) {
        this.macros.put(strings[0], strings[1]);
    }

    public String getClassName() {
        return this.className.substring(0, this.className.lastIndexOf("."));
    }

    public void addMethodExtension(JSCClass fileName) {
        this.methodExtensions.add(fileName);
    }

    public void addClassScope(String line) {
        this.classScope.add(line);
    }

    public void addMainScope(String line) {
        this.mainScope.add(line);
    }

    public List<JavaImport> getImports() {
        return this.imports;
    }

    public String[] getGenericTypes() {
        return this.genericTypes;
    }

    public String getExtendingClass() {
        return this.extendClass;
    }

    public void setPackage(String classPkg) {
        this.classPkg = classPkg;
    }

    public String getPackage() {
        return this.classPkg;
    }

    public enum C_MODIFIER {
        INTERFACE("interface"),
        ABSTRACT("abstract class"),
        CLASS("class");

        String name;

        C_MODIFIER(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final String className;

    public JSCClass(String className) {
        this.className = className;
    }

    public void setModifier(C_MODIFIER modifier) {
        this.modifier = modifier;
    }

    public List<String> getStaticImports() {
        return staticImports;
    }

    public Map<String, String> getMacros() {
        return macros;
    }

    public List<String> getClassScope() {
        return classScope;
    }

    public List<String> getMainScope() {
        return mainScope;
    }

    public C_MODIFIER getModifier() {
        return modifier;
    }

    private String[] genericTypes;
    private String[] implClasses;
    private String extendClass;
    private String classPkg;

    private List<JavaImport> imports = new ArrayList<>();

    private C_MODIFIER modifier = C_MODIFIER.CLASS;

    private List<JSCClass> methodExtensions = new ArrayList<>();

    private List<String> mainScope = new ArrayList<>();
    private List<String> classScope = new ArrayList<>();
    private List<String> staticImports = new ArrayList<>();

    private Map<String, String> macros = new HashMap<>(App.getInstance().getSettings().globalMacros);

    public List<String> toJavaClass(JSCHandler handler) {
        List<String> javaLines = new ArrayList<>();

        // Method extensions first, then class scope
        if (this.classPkg != null) {
            javaLines.add("package " + this.classPkg + ";");
        }

        for (JSCClass ext : this.methodExtensions) {
            for (JavaImport javaImport : ext.getImports()) {
                javaLines.add("import " + javaImport.getCurrentImport() + ";");
            }
        }

        for (JavaImport javaImport : this.getImports()) {
            javaLines.add("import " + javaImport.getCurrentImport() + ";");
        }

        for (String staticImport : this.getStaticImports()) {
            javaLines.add("import static " + staticImport + ";");
        }

        StringBuilder classLine = new StringBuilder("public " + this.getModifier().getName() + " " + this.getClassName());
        if (this.getGenericTypes() != null && this.getGenericTypes().length > 0) {
            classLine.append("<");
            boolean commaFlag = false;
            for (String type : this.getGenericTypes()) {
                if (commaFlag) classLine.append(",");
                classLine.append(type);
                commaFlag = true;
            }
            classLine.append(">");
        }

        if (this.getExtendingClass() != null && this.getExtendingClass().length() > 0) {
            classLine.append(" extends ").append(this.getExtendingClass());
        }

        if (this.implClasses != null && this.implClasses.length > 0) {
            classLine.append(" implements ");
            boolean commaFlag = false;
            for (String impl : this.implClasses) {
                if (commaFlag) classLine.append(",");
                classLine.append(impl);
                commaFlag = true;
            }
        }

        classLine.append(" {");
        javaLines.add(classLine.toString());

        if (this.getModifier() != C_MODIFIER.INTERFACE) {
            javaLines.add("public static void main(String[] args) {");
            javaLines.addAll(this.mainScope);
            javaLines.add("}");
        }

        for (JSCClass ext : this.methodExtensions) {
            javaLines.addAll(ext.classScope);
        }

        javaLines.addAll(this.classScope);
        javaLines.add("}");

        return javaLines;
    }
}
