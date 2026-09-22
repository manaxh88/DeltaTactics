# Live glass navigation preview

The page is recorded with Compose GraphicsLayer; the dock samples that layer in root coordinates. Navigation labels, the dock itself, snackbars and dialogs are excluded from the source to prevent feedback. No per-frame Bitmap or screen capture is used.

Android 13+: RuntimeShader rounded lens distortion composed with RenderEffect blur.
Android 12: RenderEffect blur without lens distortion.
Android 8-11: translucent page copy and surface decoration without GPU blur.

Architecture reference: https://github.com/Kyant0/AndroidLiquidGlass (Backdrop, Apache-2.0). Its current KMP branch requires a much newer toolchain. No upstream source was copied; the small shader and integration here are independently written for Compose 1.7 / Kotlin 2.0.

Validation: installed and launched on Pixel_10 emulator, API 37; home and tasks navigation checked, no AndroidRuntime crash. Existing 4 unit tests passed. Screenshots in docs/previews show the actual app. Older API fallbacks have not been run on devices. Visual parity with Apple and physical-device frame rate are not established.

Version 2.8.7 removes fixed blue tints. The screenshots in docs/previews are from the earlier blue-tinted prototype, not the final neutral palette. Lint was blocked by uncached dependency downloads; build and unit tests were run separately.
