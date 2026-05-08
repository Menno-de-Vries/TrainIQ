package com.trainiq.macrobenchmark;

import androidx.benchmark.macro.BaselineProfileMode;
import androidx.benchmark.macro.CompilationMode;
import androidx.benchmark.macro.FrameTimingMetric;
import androidx.benchmark.macro.MacrobenchmarkScope;
import androidx.benchmark.macro.StartupMode;
import androidx.benchmark.macro.StartupTimingMetric;
import androidx.benchmark.macro.junit4.BaselineProfileRule;
import androidx.benchmark.macro.junit4.MacrobenchmarkRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;
import java.util.Collections;
import kotlin.Unit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TrainIqStartupBenchmark {
    @Rule
    public final MacrobenchmarkRule benchmarkRule = new MacrobenchmarkRule();

    @Rule
    public final BaselineProfileRule baselineProfileRule = new BaselineProfileRule();

    @Test
    public void generateBaselineProfileForCriticalJourneys() {
        baselineProfileRule.collect(
                PACKAGE_NAME,
                15,
                3,
                "trainiq-critical-journeys",
                true,
                true,
                rule -> true,
                scope -> {
                    scope.pressHome();
                    scope.startActivityAndWait();
                    waitForAppReady(scope.getDevice());
                    navigateAndScrollSettings(scope);
                    return Unit.INSTANCE;
                }
        );
    }

    @Test
    public void coldStartupWithRequiredBaselineProfile() {
        benchmarkRule.measureRepeated(
                PACKAGE_NAME,
                Collections.singletonList(new StartupTimingMetric()),
                new CompilationMode.Partial(BaselineProfileMode.Require),
                StartupMode.COLD,
                5,
                scope -> {
                    scope.pressHome();
                    scope.startActivityAndWait();
                    waitForAppReady(scope.getDevice());
                    return Unit.INSTANCE;
                }
        );
    }

    @Test
    public void topLevelNavigationAndSettingsScrollFrames() {
        benchmarkRule.measureRepeated(
                PACKAGE_NAME,
                Collections.singletonList(new FrameTimingMetric()),
                new CompilationMode.Partial(BaselineProfileMode.Require),
                StartupMode.WARM,
                5,
                scope -> {
                    scope.pressHome();
                    scope.startActivityAndWait();
                    waitForAppReady(scope.getDevice());
                    return Unit.INSTANCE;
                },
                this::navigateAndScrollSettings
        );
    }

    private Unit navigateAndScrollSettings(MacrobenchmarkScope scope) {
        UiDevice device = scope.getDevice();
        tapAnyText(device, "Training", "Train");
        tapAnyText(device, "Voeding");
        tapAnyText(device, "Coach");
        tapAnyText(device, "Meer", "Instellingen");
        tapAnyText(device, "Instellingen");
        device.waitForIdle();
        if (device.findObject(By.scrollable(true)) != null) {
            device.findObject(By.scrollable(true)).scroll(Direction.DOWN, 0.7f);
        }
        device.waitForIdle();
        return Unit.INSTANCE;
    }

    private static void waitForAppReady(UiDevice device) {
        if (!device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), STARTUP_TIMEOUT_MILLIS)) {
            throw new AssertionError("Required benchmark package did not render: " + PACKAGE_NAME);
        }
        device.waitForIdle();
    }

    private static void tapAnyText(UiDevice device, String... labels) {
        for (String label : labels) {
            if (device.wait(Until.hasObject(By.text(label)), 1_000L)) {
                device.findObject(By.text(label)).click();
                device.waitForIdle();
                return;
            }
        }
        throw new AssertionError("Required benchmark target not found: " + String.join(" or ", labels));
    }

    private static final String PACKAGE_NAME = "com.trainiq";
    private static final long TIMEOUT_MILLIS = 5_000L;
    private static final long STARTUP_TIMEOUT_MILLIS = 30_000L;
}
