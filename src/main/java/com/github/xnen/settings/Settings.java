package com.github.xnen.settings;

import com.github.xnen.App;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Settings {
    public String JDK_LOCATION = "";
    public String LIB_DIRECTORY = "$home/.jrt-libs";

    public Map<String, String> globalMacros = new HashMap<>();

    public Settings() {
        globalMacros.put("#print", "System.out.println");
    }

    public void save() {
        try (FileWriter writer = new FileWriter(App.SETTINGS_FILE)){
            new GsonBuilder().setPrettyPrinting().create().toJson(this, writer);
            System.out.println("Done writing");
        } catch (IOException e) {
            System.out.println("Could not save Settings file.");
            e.printStackTrace();
        }
    }

    public String[] getLibraries() {
        File file = new File(getFormattedLibDirectory());
        if (!file.exists()) {
            if (!file.mkdirs()) {
                System.out.println("Could not create LIB_DIRECTORY specified in '--settings'!");
                System.exit(-1);
            }
        }

        if (!file.isDirectory()) {
            throw new RuntimeException("Invalid LIB directory in --settings! Please specify a proper one.");
        }

        return file.list();
    }

    public String getJDKLocation() {
        return JDK_LOCATION;
    }

    public String getFormattedLibDirectory() {
        return this.LIB_DIRECTORY.replace("$home", System.getProperty("user.home"));
    }
}
