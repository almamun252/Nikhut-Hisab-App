# Nikhut Hisab - Personal Finance Tracker

**Nikhut Hisab** (Perfect Calculation) is a simple, offline-first, and completely free income and expense tracking Android application. Designed specially with students and the general public of Bangladesh in mind, this app makes it effortless to track pocket money, hostel/mess expenses, tuition fees, or just everyday personal finances.

This project does not use any complex AI or paid APIs. All user data is securely stored locally on the device (Local Database), meaning the app requires absolutely no internet connection or server costs to run.

## Key Features

### Interactive Dashboard
* **Master Date Range Filter:** Filter your financial summary by the current month, last month, or a custom date range with a single click.
* **Visual Summary:** Beautiful, animated **Pie Charts** visually represent your expenses relative to your income.
* **Top 5 Expenses:** A **Bar Chart** on the dashboard helps you track which specific categories are draining the most money.

### Income & Expense Tracking
* **Super Fast Entry:** Quickly add data using fields for name, amount, date, and notes.
* **Suggestion Chips:** Frequently used expense names will appear as clickable suggestions during data entry, reducing the need to type.
* **Smooth Edit/Delete:** Click on any entry to open a **Bottom Sheet**, allowing easy modification or deletion of the data.

### Dynamic Categories
* Users can create unlimited custom income and expense categories according to their needs (e.g., Mess Rent, Semester Fee, Books, Transport, etc.).
* Option to save data without assigning a category (Uncategorized).

## Tech Stack

This project follows Modern Android Development (MAD) standards to ensure high performance, scalability, and clean code:

* **Language:** [Kotlin](https://kotlinlang.org/) (100%)
* **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (For modern, declarative UI and smooth animations)
* **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture Principles
* **Local Database:** [Room Database](https://developer.android.com/training/data-storage/room) (Super-fast local storage built on top of SQLite)
* **Asynchronous Programming:** Kotlin Coroutines & Flow (For efficient, real-time data updates)
* **Charts & Graphs:** Vico / YCharts (Compose-supported charting libraries)

## Database Schema

The app uses **Room Database** and primarily consists of two Entities/Tables:

1. **Transaction Table:** Stores the core income and expense data (Amount, Type, Title, CategoryId, Timestamp, Note).
2. **Category Table:** Stores user-created dynamic categories (Name, Type).

## Getting Started

Since this is an open-source project, you can easily run it on your local machine:

1. Ensure you have the latest version of [Android Studio](https://developer.android.com/studio) installed on your computer.
2. Clone this repository:
   ```bash
   git clone [https://github.com/almamun252/Nikhut-Hisab-App.git](https://github.com/almamun252/Nikhut-Hisab-App.git)

## Future Roadmap

[ ] Basic UI design and navigation setup (Jetpack Compose).

[ ] Room Database implementation and CRUD operations.

[ ] Add Pie Chart and Bar Chart to the dashboard.

[ ] Implement Suggestion Chips and filtering logic.

[ ] Future Update: Option to backup the database as a local JSON file (Export/Import).

[ ] Future Update: Light and Dark Theme support.

## Contributing
This is an open-source project. If you find any bugs or want to add new features, feel free to open an Issue or submit a Pull Request (PR).
