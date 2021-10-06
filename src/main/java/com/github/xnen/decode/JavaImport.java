package com.github.xnen.decode;

import com.github.xnen.App;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JavaImport {
    private final String className;
    private String library;

    private List<String> possibleMatches = new ArrayList<>();
    private List<String> possibleLibraries = new ArrayList<>();
    private int index;


    public JavaImport(String className) {
        this.className = className;
        this.lookup();
    }

    public JavaImport(String className, String library) {
        this.className = className;
        this.library = library;
        this.lookup();
    }

    /**
     * Search along rt.jar and libs in the library directory in Settings
     * for the class name.
     */
    private void lookup() {
        if (!new File(App.getInstance().getSettings().JDK_LOCATION).isDirectory()) {
            throw new RuntimeException("JDK Location MUST BE SET in --settings!");
        }

        String rtJar = App.getInstance().getSettings().JDK_LOCATION + File.separator + "jre" + File.separator + "lib" + File.separator + "rt.jar";

        if (this.library != null) {
            if (rtJar.contains(this.library)) {
                findMatches(rtJar);
            }
            for (String s : App.getInstance().getSettings().getLibraries()) {
                if (s.contains(this.library)) {
                    findMatches(App.getInstance().getSettings().getFormattedLibDirectory() + File.separator + s);
                }
            }
        } else {
            findMatches(rtJar);

            for (String s : App.getInstance().getSettings().getLibraries()) {
                findMatches(App.getInstance().getSettings().getFormattedLibDirectory() + File.separator + s);
            }

            if (this.possibleLibraries.size() == 0) {
                System.out.println("No matches found for " + this.className);
            }
        }
    }

    private void findMatches(String zipFile) {
        try {
            ZipFile testZip = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> entries = testZip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();

                String name = this.className.replace(".", File.separator);
                boolean wc = false;

                if (name.endsWith("*")) {
                    name = name.substring(0, name.length() - 1);
                    wc = true;
                }

                if (!name.startsWith(File.separator)) {
                    name = File.separator + name;
                }


                if (wc) {
                    if (entryName.contains(name)) {
                        String formatted = entryName.substring(0, entryName.indexOf(name) + name.length()).replace(File.separator, ".") + "*";

                        if (!this.possibleMatches.contains(formatted)) {
                            this.possibleMatches.add(formatted);
                            this.possibleLibraries.add(zipFile);
                        }
                    }
                } else {
                    if (entryName.endsWith(name + ".class")) {
                        entryName = entryName.replace(File.separator, ".");
                        entryName = entryName.substring(0, entryName.length() - 6);
                        if (!this.possibleMatches.contains(entryName)) {
                            this.possibleMatches.add(entryName);
                            this.possibleLibraries.add(zipFile);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getPossibleImports() {
        return this.possibleMatches;
    }

    public String getRaw() {
        return this.className;
    }

    public String getCurrentImport() {
        if (this.possibleMatches.size() == 0)
            return getRaw();

        return this.possibleMatches.get(this.index);
    }

    public boolean increment() {
        this.index++;

        if (this.index >= this.possibleMatches.size()) {
            this.index = this.possibleMatches.size() - 1;
            return false;
        }

        return true;
    }

    public String getCurrentLibrary() {
        if (this.possibleLibraries.size() == 0) {
            return null;
        }

        return this.possibleLibraries.get(this.index);
    }
}
