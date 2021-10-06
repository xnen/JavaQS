package com.github.xnen.decode;

import com.github.xnen.App;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xnen
 * JSC Class Container, containing all necessary elements to reconstruct the JSC class to a compile-able Java file.
 */
public class JSCClass {

    private final Map<String, String> macros = new HashMap<>(App.getInstance().getSettings().globalMacros);
    private final List<JSCClass> methodExtensions = new ArrayList<>();
    private final List<String> staticImports = new ArrayList<>();
    private final List<JavaImport> imports = new ArrayList<>();
    private final List<String> classScope = new ArrayList<>();
    private final List<String> mainScope = new ArrayList<>();
    private final String className;
    private C_MODIFIER modifier = C_MODIFIER.CLASS;
    private String[] genericTypes;
    private String[] implClasses;
    private String extendClass;
    private String classPkg;

    public JSCClass(String className) {
        this.className = className;
    }

    public void setImplementingClasses(String[] implClasses) {
        this.implClasses = implClasses;
    }
    public void setExtendingClass(String extendClass) {
        this.extendClass = extendClass;
    }
    public void setGenericTypes(String[] genericTypes) {
        this.genericTypes = genericTypes;
    }
    public void setModifier(C_MODIFIER modifier) {
        this.modifier = modifier;
    }
    public void setPackage(String classPkg) {
        this.classPkg = classPkg;
    }

    public String getClassNameNoExtension() {
        return this.className.substring(0, this.className.lastIndexOf("."));
    }
    public List<String> getStaticImports() {
        return staticImports;
    }
    public String[] getGenericTypes() {
        return this.genericTypes;
    }
    public String getExtendingClass() {
        return this.extendClass;
    }
    public List<JavaImport> getImports() {
        return this.imports;
    }
    public List<String> getClassScope() {
        return classScope;
    }
    public Map<String, String> getMacros() {
        return macros;
    }
    public List<String> getMainScope() {
        return mainScope;
    }
    public C_MODIFIER getModifier() {
        return modifier;
    }
    public String getPackage() {
        return this.classPkg;
    }
    public String getClassName() {
        return className;
    }

    public void addStaticImport(String staticImport) {
        this.staticImports.add(staticImport);
    }
    public void addMethodExtension(JSCClass fileName) {
        this.methodExtensions.add(fileName);
    }
    public void addMacro(String[] strings) {
        this.macros.put(strings[0], strings[1]);
    }
    public void addImport(JavaImport javaImport) {
        this.imports.add(javaImport);
    }
    public void addClassScope(String line) {
        this.classScope.add(line);
    }
    public void addMainScope(String line) {
        this.mainScope.add(line);
    }

    public List<String> toJavaClass() {
        List<String> javaLines = new ArrayList<>();

        if (this.classPkg != null) {
            javaLines.add("package " + this.classPkg + ";");
        }

        for (JSCClass ext : this.methodExtensions) {
            // Must add macros here to translate properly in new class
            this.macros.putAll(ext.macros);

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

        StringBuilder classLine = new StringBuilder("public " + this.getModifier().getName() + " " + this.getClassNameNoExtension());
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
}
