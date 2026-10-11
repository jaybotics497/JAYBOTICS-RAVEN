# Jaybotics Raven fresh production reconstruction
Package: `com.ravencore.zero`.

This tree is a fresh implementation derived from the supplied 100% Completion Authority and Hardened Development Authority v2. It deliberately does not import legacy Raven visual/UI/renderer/animation code.

## Current external release blockers
1. `libraven_qwen.so` (or an admitted llama.cpp Android JNI backend) is not contained in the authority package and must be supplied/built before real Qwen inference can PASS.
2. Android SDK/Gradle are not available in the current execution container, so APK compilation/signing/device validation cannot be truthfully claimed here.
3. Samsung A17 runtime validation requires the target device.

No blocker is converted into a fake PASS.
