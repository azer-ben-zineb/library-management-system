package com.libraryplus.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

 
public class MainApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);
    private static final String APP_NAME = "LibraryPlus";
    private static final String APP_VERSION = "1.0.0";

    
    private javafx.scene.media.MediaPlayer bgPlayer;

    @Override
    public void start(Stage stage) throws IOException {
        try {
            
            
            try {
                String projectDir = System.getProperty("user.dir");
                String h2Path = projectDir + "/data/libraryplus";
                String h2Url = "jdbc:h2:file:" + h2Path + ";DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";
                
                
                System.setProperty("H2_FILE_URL", h2Url);
                
                
                try {
                    java.util.Map<String, String> env = System.getenv();
                    java.lang.reflect.Field f = env.getClass().getDeclaredField("m");
                    f.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, String> writableEnv = (java.util.Map<String, String>) f.get(env);
                    writableEnv.put("H2_FILE_URL", h2Url);
                } catch (Throwable ignore) {
                    
                }
            } catch (Exception ignore) {
            }
            logger.info("Starting {} v{}", APP_NAME, APP_VERSION);

            
            try {
                new com.libraryplus.service.FineService().calculateAndApplyFines();
            } catch (Exception e) {
                logger.error("Failed to calculate fines on startup", e);
            }

            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            
            Scene scene = new Scene(root, 800, 600);

            
            applyTheme(scene);

            
            try {
                com.libraryplus.util.AudioManager.getInstance().playMusic();
            } catch (Exception e) {
                logger.warn("Unable to start background music", e);
            }

            
            stage.setTitle(APP_NAME);
            stage.setScene(scene);
            stage.setWidth(800);
            stage.setHeight(600);
            stage.setResizable(true);

            
            

            
            stage.show();
            logger.info("Application started successfully");

        } catch (IOException ex) {
            logger.error("Failed to start application", ex);
            throw ex;
        } catch (Exception ex) {
            logger.error("Unexpected error during application startup", ex);
            throw new RuntimeException("Failed to start application: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void stop() throws Exception {
        try {
            com.libraryplus.util.AudioManager.getInstance().stopMusic();
        } catch (Throwable ignore) {
        }
        super.stop();
    }

     
    private void applyTheme(Scene scene) {
        try {
            String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
            if (pref == null)
                pref = "Catppuccin";
            com.libraryplus.util.ThemeManager.applyTheme(scene, pref);
            logger.info("Theme applied: {}", pref);
        } catch (Exception ex) {
            logger.warn("Failed to load CSS theme: {}", ex.getMessage());
        }
    }

    public static void main(String[] args) {
        logger.info("Launching {} application", APP_NAME);
        launch(args);
    }
}
