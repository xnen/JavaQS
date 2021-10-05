package com.github.xnen;


import com.github.xnen.decode.JSCHandler;
import com.github.xnen.exception.ParameterException;
import com.github.xnen.impl.IHandler;
import com.github.xnen.param.ParamBuilder;
import com.github.xnen.param.Parameter;
import com.github.xnen.settings.Settings;
import com.google.gson.Gson;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class App {
    public static final File SETTINGS_FILE = new File(System.getProperty("user.home") + File.separator + "javart.json");

    private Parameters parameters;
    private Settings settings;

    public static App stInstance;

    public static App getInstance() {
        if (stInstance == null) {
            throw new RuntimeException("App instance not yet set-up! Should be called with the main() function with it's args!");
        }

        return stInstance;
    }

    public App() {
    }

    private void init(String[] args) {
        this.loadSettings();

        this.parameters = new Parameters(getJARName(), "Run Java in quick scripting or in a real-time prompt.");

        this.parameters.handleInvalidOptionsWith(strings ->
                System.out.println("Unhandled options '" + Arrays.toString(strings) + "'."));

        this.parameters.setDefaultParameter(FILE_HANDLER_PARAM);
        this.parameters.register(CLEAR_SETTINGS_PARAM);
        this.parameters.register(ParamBuilder.with().identifier("--settings").description("Open settings file").handler(OPEN_SETTINGS_HANDLER).priority((short) 10).build());

        try {
            this.parameters.process(args);
        } catch (ParameterException e) {
            System.out.println(e.getMessage());
        }
    }

    private void loadSettings() {
        if (SETTINGS_FILE.exists()) {
            try (FileReader reader = new FileReader(SETTINGS_FILE)) {
                this.settings = new Gson().fromJson(reader, Settings.class);
            } catch (IOException e) {
                System.out.println("[WARN] Unable to load settings file 'javart.json'! Loading defaults");
                this.settings = new Settings();
                e.printStackTrace();
            }
        } else {
            this.settings = new Settings();
            this.settings.save();
        }
    }

    private String getJARName() {
        return new File(Parameters.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
    }

    public static void main(String[] args) {
        stInstance = new App();
        stInstance.init(args);
    }

    private void pipe(InputStream is, OutputStream os) throws IOException {
        boolean flag = false;

        while (is.available() > 0) {
            byte[] byteBuf = new byte[is.available()];
            int i = is.read(byteBuf);
            if (i > -1) {
                os.write(byteBuf);
                flag = true;
            }
        }

        if (flag) {
            os.flush();
        }
    }

    private final Parameter FILE_HANDLER_PARAM = ParamBuilder.with().identifier("*.jsc").description("JSC File").handler(strings ->
    {
        try {
            Path tempDir = Files.createTempDirectory("tmpjsccompile");

            JSCHandler handler = new JSCHandler(strings[0]);
            handler.writeClasses(tempDir);
            handler.compileAll(tempDir);
            handler.pack(tempDir);
            String[] args = new String[strings.length + 2];

            args[0] = "java";
            args[1] = "-jar";
            args[2] = "JSCRuntime.jar";

            System.arraycopy(strings, 1, args, 3, strings.length - 1);

            Process running = Runtime.getRuntime().exec(args);
            InputStream ris = running.getInputStream();
            while (running.isAlive()) {
                while (ris.available() > 0) {
                    byte[] byteBuf = new byte[ris.available()];
                    int i = ris.read(byteBuf);
                    if (i > -1) {
                        System.out.print(new String(byteBuf));
                    }
                }

                pipe(System.in, running.getOutputStream());
                Thread.sleep(1);
            }

            System.exit(running.exitValue());

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }).acceptsInfiniteArgs("[Class] [Args...]").build();


    private final IHandler CLEAR_SETTINGS_HANDLER = s -> {
        Scanner sc = new Scanner(System.in);
        System.out.println("Are you sure? (y/n): ");
        String v = sc.nextLine();
        if (v.equalsIgnoreCase("y")) {
            new Settings().save();
        }
        System.exit(0);
    };

    private final Parameter CLEAR_SETTINGS_PARAM = ParamBuilder.with().identifier("--clear-settings").description("Clear all settings and restore defaults")
            .handler(CLEAR_SETTINGS_HANDLER)
            .priority((short) 10)
            .build();

    private final IHandler OPEN_SETTINGS_HANDLER = s -> {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        try {
            if (os.startsWith("linux")) {
                Runtime.getRuntime().exec("xdg-open " + SETTINGS_FILE.getAbsolutePath());
            } else if (os.startsWith("windows")) {
                Desktop.getDesktop().edit(SETTINGS_FILE);
            }
        } catch (IOException e) {
            System.out.println("Could not open Settings file @ " + SETTINGS_FILE.getAbsolutePath() + ".");
            e.printStackTrace();
        }
        System.exit(0);
    };

    public Settings getSettings() {
        return this.settings;
    }

    public String getOs() {
        return System.getProperty("os.name");
    }
}
