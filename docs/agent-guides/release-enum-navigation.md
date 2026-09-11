# Release enum navigation regression

The signed release at `a8ff65a39ed1600f43325d02ec9e0f28be8717bd`
installed successfully, then crashed while constructing the navigation graph:
`Cannot find class with name "com.trainiq.domain.model.MealType"`.
R8 had renamed that class to `gi2`. Debug navigation tests cannot expose this.

`MealDetail` uses this enum as a typed argument. Keep only that enum and its
constants at the R8 boundary; do not disable shrinking or keep the entire domain
package. This preserves the existing serialized route and stored enum values.
Android documents preserving enum names for navigation:
https://developer.android.com/guide/navigation/use-graph/pass-data#proguard

## Local regression proof

1. Build the signed, minified `:app:assembleRelease` from the recorded clean commit.
   Generate any required migration marker in a separate preceding Gradle invocation.
2. Check release mapping retains `com.trainiq.domain.model.MealType` and its
   constants. Verify the APK signature and install that exact APK on an isolated
   agent-owned emulator with compatible signing; never clear existing user data.
3. Start `com.trainiq/.MainActivity`, verify actual visible onboarding/Home UI and
   a surviving process. `am start -W` returning `ok` alone is insufficient.
4. Complete optional onboarding locally, open each Home meal category and return;
   verify the corresponding empty detail title and Back behavior without saving.
5. Background/foreground the app and inspect app-specific fatal logs. Retain the
   APK hash, UI trees/screenshots and results in the ignored delivery ledger.

No domain, persistence, dependency, permission or UI behavior changes are needed.
Reuse unchanged lower-layer evidence; the decisive regression gate is the exact
minified artifact running its typed enum routes.
