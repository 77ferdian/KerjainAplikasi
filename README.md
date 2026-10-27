# KerjainAplikasi

KerjainAplikasi is an Android-based platform designed to connect students, mentors, and industry partners in a single integrated ecosystem. The application supports learning processes, assignment management, skill development, and internship program coordination in a more structured and transparent way.

<p align="left">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase" alt="Firebase" />
  <img src="https://img.shields.io/badge/Firestore-FFCA28?style=for-the-badge&logo=google-cloud" alt="Firestore" />
  <img src="https://img.shields.io/badge/Material%20Design-757575?style=for-the-badge&logo=material-design" alt="Material Design" />
  <img src="https://img.shields.io/badge/Architecture-MVVM-4ECDC4?style=for-the-badge" alt="MVVM" />
  <img src="https://img.shields.io/badge/Build-Gradle-02303A?style=for-the-badge&logo=gradle" alt="Gradle" />
</p>

## Overview

KerjainAplikasi was developed to bridge the gap between education and industry by providing a digital ecosystem for:

- Students to explore programs, submit assignments, track learning progress, and receive digital certificates.
- Mentors to assess student work, monitor active learners, and provide structured feedback.
- Industry partners to manage programs, evaluate candidates, and monitor the effectiveness of their initiatives.

## Roles and Features

### Student
- Explore internship and training programs
- Track learning progress and task completion
- Submit assignments and portfolio work
- Receive digital certificates after completing a program

### Mentor
- Review and assess student submissions
- Monitor participant development in real time
- Provide structured and constructive feedback

### Partner
- Manage and publish internship or training programs
- Access a talent pool filtered by student performance and skills
- Analyze program performance and participation data

## Tech Stack

- Android
- Kotlin
- Jetpack Components
- Material Design 3
- Firebase Authentication
- Cloud Firestore
- Firebase Storage
- MVVM Architecture
- Repository Pattern
- Gradle

## Technology and Architecture

The application is built using a modern Android development approach to ensure scalability, maintainability, and strong performance:

- Programming language: Kotlin
- Architecture: MVVM + Repository Pattern
- Database: Cloud Firestore
- Authentication: Firebase Authentication
- File storage: Firebase Storage
- User interface: Material Design 3
- Supporting libraries: Lifecycle, Coroutines, Flow, Navigation

## Project Structure

```text
KerjainAplikasi/
├── app/
│   ├── src/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
├── gradle.properties
├── .gitignore
├── README.md
└── .idea/
```

## Requirements

- Latest Android Studio
- JDK 11 or higher
- Android emulator or physical device
- Active Firebase account

## Getting Started

1. Clone the repository

```bash
git clone https://github.com/77ferdian/KerjainAplikasi.git
```

2. Open the project in Android Studio

3. Add your Firebase configuration file

Place the `google-services.json` file inside the `app/` directory.

4. Enable required Firebase services

Make sure Firebase Authentication, Firestore, and Storage are enabled in the Firebase Console.

5. Sync Gradle and run the application

```bash
./gradlew build
```

## Contributing

Contributions are welcome for improvements such as bug fixes, UI enhancement, performance optimization, and new feature development. Please fork the repository and submit a pull request with a clear description of the changes.

## Developer Team

| Name | NPM | Role |
| --- | --- | --- |
| Agil Adiwidya Nurfadillah | 24082010199 | Developer |
| Asti Sofiana Anastasia | 24082010207 | Developer |
| Ferdian | 24082010213 | Developer |

## Project Status

This project is developed as part of an initiative to strengthen the collaboration between education and industry through a more relevant, practical, and technology-driven learning ecosystem.
