package com.libraryplus.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class FxmlLoadSmokeTest {

    @Test
    public void loadDashboardFxml() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] ex = new Exception[1];
        Runnable runner = () -> {
            try {
                FXMLLoader loader = new FXMLLoader(FxmlLoadSmokeTest.class.getResource("/fxml/dashboard.fxml"));
                Parent root = loader.load();
                assertNotNull(root);
            } catch (Exception e) {
                ex[0] = e;
            } finally {
                latch.countDown();
            }
        };
        try {
            Platform.startup(runner);
        } catch (IllegalStateException already) {
            
            Platform.runLater(runner);
        }
        boolean ok = latch.await(10, TimeUnit.SECONDS);
        if (!ok) fail("FX startup timed out");
        if (ex[0] != null) throw ex[0];
    }
}
