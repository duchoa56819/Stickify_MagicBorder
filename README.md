# Stickify: MagicBorder 🪄

Stickify is an Android application built with Kotlin that leverages **Google ML Kit's Subject Segmentation API** to automatically extract people from photos and wrap them in a dynamic white stroke (sticker effect).

## 🚀 Key Features

*   **👥 Multiple Subject Detection:** Automatically detects and isolates multiple people within a single photo.
*   **✂️ Auto-Cropping & Sticker Mode:** Tightly crops the detected subjects and resizes them perfectly for Telegram and Zalo sticker standards (Max 512x512).
*   **🖍️ Magic Stroke Border:** Uses alpha-channel dilation algorithms to generate a smooth, solid white outline around the subject without complicated path tracing.
*   **💾 One-Tap Save:** Save all extracted stickers directly to your device's Gallery with a transparent PNG background in a single tap.

## 🛠️ Technology Stack

*   **Language:** Kotlin
*   **Minimum SDK:** API 24 (Android 7.0)
*   **Core Logic:** Google Play Services - ML Kit Subject Segmentation
*   **Graphics:** Native Android Canvas and Paint APIs for image processing and dilation

## 💡 How It Works

1. Select a photo from your gallery.
2. The ML Kit AI analyzes the image and returns a list of individual subject masks.
3. The app extracts each subject's alpha channel and draws it iteratively in 36 angles to simulate a perfect morphological dilation (stroke).
4. The final processed image is overlaid on top and resized for sticker usage.
5. Tap **Lưu Ảnh** (Save) to export them as transparent PNGs!

## 📸 To-do / Enhancements

- [ ] Add Color Picker for custom border colors
- [ ] Add SeekBar to adjust border thickness
- [ ] Support custom background replacement

## 📄 License
This project is open-sourced under the MIT License.
