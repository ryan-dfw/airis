# AIris 2.0

A rebuilt Android client for Image Reconstruction via Captioning. It uses the same pipeline as before: Sony camera, then gpt-4o-mini, then an image model (originally dall-e-3, now gpt-image-1). The app is now Kotlin + Jetpack Compose with a glass-style UI.

## Build

Open this folder in Android Studio (Ladybug or newer) and run on your phone. The build uses AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21, compileSdk 34 and minSdk 26.

On first launch, the app asks for your OpenAI API key. The key is encrypted with an Android Keystore key and stored in private SharedPreferences, and backups exclude it. You can change or remove it later under **Setup**.

## Logo

`art/airis-logo.svg` (plus a transparent version and a 1024px PNG) shows ten curved blades spiralling into an iris around a glowing pupil, in violet, rose and amber. The same geometry drives the adaptive launcher icon (with a monochrome layer for themed icons) and `ui/components/AirisMark.kt`, which floats in the hollow on the home screen.

## Screens

- **Camera**: a dim, breathing hollow with a faint ring of light circling it and the AIris mark turning slowly inside. The mark dims when no camera is connected. When a shot arrives, the print spins up out of the hollow, growing from a point and unwinding half a turn over about 2 seconds. "developing..." shows while it works, then the image fades up out of black over about 9.5 seconds and thumbs down/up buttons appear either side. Dismissing a print, or the next shot, draws it back into the hollow.
- **Prints**: a plain square grid of every generated image, stored in `filesDir/prints`, with a small badge on rated ones. Tap one to view it large, rate it or delete it. Ratings live in the private `airis_ratings` prefs. There is deliberately no share or save-to-phone option.
- **Setup**: in order, How AIris works, Connect your camera, Mobile data, then API key status. The Setup screen, errors and Prints never name the AI provider or models; only the key dialog mentions OpenAI.

## Pipeline (unchanged)

`pipeline/Pipeline.kt` is a straight port of the original `MainActivity.java`. It keeps the same `getEvent` polling of `http://10.0.0.1:10000/sony/camera`, the same `result[5][0].takePictureUrl` parsing and the same caption prompt. It sends the same image payload with the `"In a photorealistic style: "` prefix, and it still binds OpenAI traffic to the cellular network. OpenAI shut down `dall-e-3` on May 12, 2026, so the image step now calls `gpt-image-1` (change `IMAGE_MODEL` in `Pipeline.kt` to swap it). That model returns the PNG as base64 instead of a URL. The plumbing changes are:

- Coroutines replace raw threads.
- The API key comes from on-device storage.
- Errors show up in the UI instead of being swallowed.
- Polling keeps retrying until the camera is reachable, and only runs while the app is visible.
- A URL that was already developed is skipped, so a repeated `getEvent` result can't re-develop the same shot.
- `net/CellularNetwork.kt` is started once from `AirisApplication.onCreate()`. It calls `requestNetwork()` for cellular + internet and never unregisters, so Android keeps mobile data up for the whole process, even in the background and while on the camera's Wi-Fi. Every OpenAI connection opens on that `Network`.
- If the phone has no cellular network at all, requests fall back to the default network instead of throwing a null pointer exception.

## Layout

```
app/src/main/java/com/tollview/airis/
  MainActivity.kt          edge-to-edge host, foreground signal
  AirisViewModel.kt        listening loop, develop stages, gallery state
  pipeline/Pipeline.kt     camera → gpt-4o-mini → gpt-image-1
  data/ApiKeyStore.kt      Keystore-encrypted key
  data/PrintStore.kt       generated images on disk + FileProvider
  ui/glass/Glass.kt        backdrop recording + frosted glass surface + ambient light
  ui/home/Instax.kt        print frame, blank / developing / exposing film
  ui/home/HomeScreen.kt    camera tab
  ui/gallery/…             prints grid + viewer
  ui/guide/…               setup / how-to
  ui/KeyDialog.kt, TabBar.kt, AirisApp.kt
```

The glass effect records the scene into a `GraphicsLayer` and redraws a blurred, position-matched copy behind each panel. Real blur needs Android 12 or newer. On older versions the panels fall back to a denser frosted tint.
