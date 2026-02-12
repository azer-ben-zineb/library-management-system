package com.libraryplus.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class DashboardControllerSearchTest {

    @Test
    public void invokeSearchHandlerProgrammatically() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] ex = new Exception[1];
        Runnable runner = () -> {
            try {
                FXMLLoader loader = new FXMLLoader(FxmlLoadSmokeTest.class.getResource("/fxml/dashboard.fxml"));
                Parent root = loader.load();
                assertNotNull(root);
                DashboardController ctl = loader.getController();
                assertNotNull(ctl);
                
                Object sfObj = loader.getNamespace().get("searchField");
                Platform.runLater(() -> {
                    try {
                        if (sfObj instanceof javafx.scene.control.TextField) {
                            ((javafx.scene.control.TextField) sfObj).setText("the");
                        }
                        ctl.onSearch(null);
                        
                        Platform.runLater(() -> latch.countDown());
                    } catch (Exception e) {
                        ex[0] = e;
                        latch.countDown();
                    }
                });
            } catch (Exception e) {
                ex[0] = e;
                latch.countDown();
            }
        };
        try {
            Platform.startup(runner);
        } catch (IllegalStateException already) {
            Platform.runLater(runner);
        }
        boolean ok = latch.await(6, TimeUnit.SECONDS);
        if (!ok) fail("FX search test timed out");
        if (ex[0] != null) throw ex[0];
    }
}
