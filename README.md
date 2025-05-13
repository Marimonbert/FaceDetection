
# FaceDetection

Android application developed in **Kotlin** that performs real-time **face detection and recognition**, using **ML Kit** and **TensorFlow Lite**. The app captures and stores user faces, recognizes registered individuals, and manages a local user database with **SQLite**.

## 🎯 What This App Does

This app enables facial access control and user identification with three core features:

1. **Image Capture & Registration:**  
   Detects a user's face, captures an image, and stores it locally in SQLite along with a provided name.

2. **Face Recognition & Validation:**  
   Detects and recognizes a user's face in real-time. If the face matches a registered user in SQLite, the app greets the user and navigates to the next screen.

3. **User List Management:**  
   Displays a list of all registered users with their names and stored images.

---

## 🚀 Features

- 📸 Real-time face detection (ML Kit)
- 🧠 Face recognition with TensorFlow Lite
- 🗂️ SQLite integration for local face storage
- 🧾 User registration with face + name
- ✅ Identity validation & welcome screen
- 👤 View all registered users

---

## 🛠️ Tech Stack

- **Kotlin**
- **ML Kit** – Face detection API
- **TensorFlow Lite** – Face recognition engine
- **CameraX** – Live camera feed
- **SQLite** – Local face data storage
- **Jetpack Compose** – Declarative UI
- **MediaPipe Vision Tasks**
- **Gson**, **Coil** – Image and data handling

---

## 🧱 Project Structure

```
FaceDetection/
├── app/
│   ├── ui/            -> Jetpack Compose UI
│   ├── camera/        -> CameraX integration
│   ├── detection/     -> ML Kit logic
│   ├── recognition/   -> TFLite recognition
│   ├── database/      -> SQLite local storage
│   └── screens/       -> Navigation and views
```

---

## 🔧 How to Run

1. Clone this repository:

```bash
git clone https://github.com/Marimonbert/FaceDetection.git
```

2. Open with Android Studio (Arctic Fox or newer)

3. Connect a physical device or emulator with camera

4. Run the app using ▶️ or `Shift + F10`

---

## 📷 Required Permissions

- **Camera**: To capture face images
- **Storage (optional)**: To save face data if exported

---

## 👤 Author

**Maria Monteiro**  
🔗 [LinkedIn](https://www.linkedin.com/in/marimonob)  
💻 [GitHub](https://github.com/Marimonbert)

---

![Platform](https://img.shields.io/badge/platform-Android-blue)
![Language](https://img.shields.io/badge/language-Kotlin-orange)
![MLKit](https://img.shields.io/badge/ML_Kit-Face_Detection-green)
![TensorFlow](https://img.shields.io/badge/TensorFlow_Lite-Enabled-yellow)
