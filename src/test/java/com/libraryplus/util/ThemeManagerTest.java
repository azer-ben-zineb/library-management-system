package com.libraryplus.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ThemeManagerTest {
    private Path originalHome;
    private Path tempHome;

    @BeforeEach
    public void setUp() throws Exception {
        originalHome = Path.of(System.getProperty("user.home"));
        tempHome = Files.createTempDirectory("libraryplus-test-home");
        System.setProperty("user.home", tempHome.toString());
    }

    @AfterEach
    public void tearDown() throws Exception {
        
        System.setProperty("user.home", originalHome.toString());
        
        try {
            var pref = tempHome.resolve(".libraryplus");
            if (Files.exists(pref)) {
                Files.walk(pref)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception ignore) {}
                        });
            }
            Files.deleteIfExists(tempHome);
        } catch (Exception ignored) {}
    }

    @Test
    public void testSaveAndLoadPreference() {
        String name = "Catppuccin";
        ThemeManager.saveThemePreference(name);
        String loaded = ThemeManager.loadThemePreference();
        assertEquals(name, loaded);

        
        ThemeManager.saveThemePreference("Tokyo Night");
        assertEquals("Tokyo Night", ThemeManager.loadThemePreference());
    }
}

