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
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;
import java.io.IOException;
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
                    seedActiveWorkout(scope);
                    scope.pressHome();
                    startTrainIqMainActivity(scope);
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
                    seedActiveWorkout(scope);
                    scope.pressHome();
                    startTrainIqMainActivity(scope);
                    waitForAppReady(scope.getDevice());
                    return Unit.INSTANCE;
                },
                this::navigateAndScrollSettings
        );
    }

    @Test
    public void activeWorkoutLoggingFrames() {
        benchmarkRule.measureRepeated(
                PACKAGE_NAME,
                Collections.singletonList(new FrameTimingMetric()),
                new CompilationMode.Partial(BaselineProfileMode.Require),
                StartupMode.WARM,
                5,
                scope -> {
                    seedActiveWorkout(scope);
                    scope.pressHome();
                    startTrainIqMainActivity(scope);
                    tapBottomTrain(scope.getDevice());
                    tapSeededActiveRoutineStart(scope.getDevice());
                    requireAnyText(scope.getDevice(), "Actieve training");
                    return Unit.INSTANCE;
                },
                this::logActiveWorkoutSet
        );
    }

    @Test
    public void activeWorkoutScrollFrames() {
        benchmarkRule.measureRepeated(
                PACKAGE_NAME,
                Collections.singletonList(new FrameTimingMetric()),
                new CompilationMode.Partial(BaselineProfileMode.Require),
                StartupMode.WARM,
                5,
                scope -> {
                    seedActiveWorkout(scope);
                    scope.pressHome();
                    startTrainIqMainActivity(scope);
                    tapBottomTrain(scope.getDevice());
                    tapSeededActiveRoutineStart(scope.getDevice());
                    requireAnyText(scope.getDevice(), "Actieve training");
                    return Unit.INSTANCE;
                },
                this::scrollActiveWorkoutUpAndDown
        );
    }

    private Unit navigateAndScrollSettings(MacrobenchmarkScope scope) {
        UiDevice device = scope.getDevice();
        tapVisibleTextCenter(device, "Meer");
        requireAnyText(device, "Instellingen", "Health Connect, AI en voorkeuren");
        device.waitForIdle();
        swipeAcrossSettings(device, true, 6);
        swipeAcrossSettings(device, false, 6);
        return Unit.INSTANCE;
    }

    private Unit logActiveWorkoutSet(MacrobenchmarkScope scope) {
        UiDevice device = scope.getDevice();
        swipeToActiveWorkoutLogControls(device);
        device.click(
                (int) (device.getDisplayWidth() * 0.76f),
                (int) (device.getDisplayHeight() * 0.56f)
        );
        device.waitForIdle();
        return Unit.INSTANCE;
    }

    private Unit scrollActiveWorkoutUpAndDown(MacrobenchmarkScope scope) {
        UiDevice device = scope.getDevice();
        int width = device.getDisplayWidth();
        int height = device.getDisplayHeight();
        for (int i = 0; i < 4; i++) {
            device.swipe(width / 2, (int) (height * 0.78f), width / 2, (int) (height * 0.20f), 36);
            device.waitForIdle();
        }
        for (int i = 0; i < 4; i++) {
            device.swipe(width / 2, (int) (height * 0.22f), width / 2, (int) (height * 0.80f), 36);
            device.waitForIdle();
        }
        return Unit.INSTANCE;
    }

    private static void seedActiveWorkout(MacrobenchmarkScope scope) {
        try {
            String output = scope.getDevice().executeShellCommand(
                    "am start -W -n " + PACKAGE_NAME + "/com.trainiq.benchmark.BenchmarkSeedActivity"
            );
            if (output.contains("Error") || output.contains("Exception")) {
                throw new AssertionError("Failed to seed active workout benchmark state: " + output);
            }
        } catch (IOException exception) {
            throw new AssertionError("Failed to seed active workout benchmark state", exception);
        }
        scope.getDevice().waitForIdle();
    }

    private static void startTrainIqMainActivity(MacrobenchmarkScope scope) {
        try {
            String output = scope.getDevice().executeShellCommand(
                    "am start -W -n " + PACKAGE_NAME + "/.MainActivity"
            );
            if (output.contains("Error") || output.contains("Exception")) {
                throw new AssertionError("Failed to start TrainIQ main activity: " + output);
            }
        } catch (IOException exception) {
            throw new AssertionError("Failed to start TrainIQ main activity", exception);
        }
        waitForAppReady(scope.getDevice());
    }

    private static void waitForAppReady(UiDevice device) {
        if (!device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), STARTUP_TIMEOUT_MILLIS)) {
            throw new AssertionError("Required benchmark package did not render: " + PACKAGE_NAME);
        }
        device.waitForIdle();
    }

    private static void tapBottomTrain(UiDevice device) {
        tapBottomDestination(device, 1);
    }

    private static void tapBottomDestination(UiDevice device, int index) {
        int destinationCount = 5;
        device.click(
                (int) (device.getDisplayWidth() * ((index + 0.5f) / destinationCount)),
                (int) (device.getDisplayHeight() * 0.94f)
        );
        device.waitForIdle();
    }

    private static void tapSeededActiveRoutineStart(UiDevice device) {
        device.wait(Until.hasObject(By.text("Benchmark routine")), TIMEOUT_MILLIS);
        device.click(
                (int) (device.getDisplayWidth() * 0.28f),
                (int) (device.getDisplayHeight() * 0.39f)
        );
        device.waitForIdle();
    }

    private static void requireAnyText(UiDevice device, String... labels) {
        for (String label : labels) {
            if (device.wait(Until.hasObject(By.text(label)), 5_000L)) {
                return;
            }
        }
        throw new AssertionError("Required benchmark screen not found: " + String.join(" or ", labels));
    }

    private static void tapVisibleTextCenter(UiDevice device, String label) {
        if (!device.wait(Until.hasObject(By.text(label)), TIMEOUT_MILLIS)) {
            throw new AssertionError("Required benchmark target not found: " + label);
        }
        android.graphics.Point center = device.findObject(By.text(label)).getVisibleCenter();
        device.click(center.x, center.y);
        device.waitForIdle();
    }

    private static void swipeAcrossSettings(UiDevice device, boolean downward, int count) {
        int width = device.getDisplayWidth();
        int height = device.getDisplayHeight();
        for (int index = 0; index < count; index++) {
            int startY = downward ? (int) (height * 0.78f) : (int) (height * 0.22f);
            int endY = downward ? (int) (height * 0.22f) : (int) (height * 0.78f);
            device.swipe(width / 2, startY, width / 2, endY, 30);
        }
        device.waitForIdle();
    }

    private static void swipeToActiveWorkoutLogControls(UiDevice device) {
        int width = device.getDisplayWidth();
        int height = device.getDisplayHeight();
        for (int i = 0; i < 2; i++) {
            device.swipe(width / 2, (int) (height * 0.76f), width / 2, (int) (height * 0.22f), 30);
            device.waitForIdle();
        }
    }

    private static final String PACKAGE_NAME = "com.trainiq";
    private static final long TIMEOUT_MILLIS = 5_000L;
    private static final long STARTUP_TIMEOUT_MILLIS = 30_000L;
}
