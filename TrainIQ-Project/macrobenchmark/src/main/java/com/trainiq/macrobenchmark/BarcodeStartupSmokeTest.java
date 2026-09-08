package com.trainiq.macrobenchmark;

import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Functional R8 regression, not a performance measurement. Run on an isolated camera-equipped AVD. */
@RunWith(AndroidJUnit4.class)
public class BarcodeStartupSmokeTest {
    @Test public void minifiedScannerOpensWithCameraPermissionAlreadyGranted() throws Exception {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.executeShellCommand("am force-stop com.trainiq");
        device.executeShellCommand("pm grant com.trainiq android.permission.CAMERA");
        device.executeShellCommand("am start -W -n com.trainiq/.MainActivity");
        assertTrue(device.wait(Until.hasObject(By.pkg("com.trainiq")), 15000));
        for (int step = 0; step < 10; step++) {
            device.waitForIdle();
            UiObject2 later = device.wait(Until.findObject(By.text("Later afronden")), 500);
            if (later != null) {
                later.click();
            } else if (device.hasObject(By.desc("Voeding"))) {
                break;
            } else {
                device.swipe(device.getDisplayWidth() / 2, device.getDisplayHeight() * 3 / 4,
                        device.getDisplayWidth() / 2, device.getDisplayHeight() / 3, 25);
            }
        }
        click(device, By.desc("Voeding"));
        click(device, By.desc("Toevoegen aan Ochtend"));
        click(device, By.text("Barcode scannen"));
        assertTrue("Scanner must render", device.wait(Until.hasObject(By.text("Barcodescanner")), 10000));
        // The original minified app throws during ML Kit registrar discovery before preview.
        // Allow asynchronous initialization to finish, then require a live camera and retained UI.
        long deadline = SystemClock.uptimeMillis() + 15000;
        boolean cameraActive = false;
        do {
            String camera = device.executeShellCommand("dumpsys media.camera");
            cameraActive = camera.contains("Client package: com.trainiq");
            if (!cameraActive) SystemClock.sleep(250);
        } while (!cameraActive && SystemClock.uptimeMillis() < deadline);
        assertTrue("Minified scanner must own an active camera", cameraActive);
        SystemClock.sleep(2000);
        assertTrue(device.hasObject(By.text("Barcodescanner")));
        assertFalse(device.hasObject(By.textContains("Camera kan nu niet starten")));
        assertFalse(device.hasObject(By.textContains("Barcodeherkenning kon niet starten")));
        device.pressBack();
        assertTrue(device.wait(Until.hasObject(By.desc("Toevoegen aan Ochtend")), 5000));
    }

    private static void click(UiDevice device, BySelector selector) {
        UiObject2 target = device.wait(Until.findObject(selector), 10000);
        assertNotNull("Missing action: " + selector, target);
        target.click();
        device.waitForIdle();
    }
}
