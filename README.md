# 🎬 Lebanon Cinema Booking System

A desktop cinema management and booking system developed with **Java**, **JavaFX**, and **MySQL**. The system is designed to manage cinema branches across Lebanon and provides tools for employees to manage movies, halls, seats, shows, customers, bookings, payments, snacks, employees, and reports.

## ✨ Features

- 🔐 Employee login and role-based access
- 🎬 Movie management
- 🏢 Cinema branch management
- 🏛️ Hall and seat management
- 🕒 Show scheduling
- 👤 Customer management
- 🎟️ Seat booking and booking management
- 💳 Payment management
- 🍿 Snack management
- 📊 Reports and cinema performance statistics
- 🤖 AI/movie recommendation features
- 📧 Email notifications
- 🎞️ TMDB integration for movie information and posters

## 🛠️ Technologies

- Java
- JavaFX
- MySQL
- JDBC
- NetBeans
- TMDB API
- JavaMail

## 📁 Project Structure

```text
CinemaSystem/
├── src/                 # Java source code, controllers, models, FXML and images
├── lib/                 # Required third-party JAR files
├── nbproject/           # NetBeans project configuration
├── database/            # Database scripts (add your exported SQL schema here)
├── .env.example         # Example environment variables
├── .gitignore
├── build.xml
├── manifest.mf
└── README.md
```

## ⚙️ Requirements

- JDK 17+
- JavaFX SDK compatible with the project
- MySQL Server
- NetBeans (or another Java IDE that supports the project)

## 🚀 Setup

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/CinemaSystem.git
cd CinemaSystem
```

### 2. Create the MySQL database

Create a database named:

```sql
CREATE DATABASE cinema_booking_system;
```

Then import the SQL script from the `database/` folder.

> The repository currently does not contain an exported database dump. Export your working `cinema_booking_system` database from MySQL Workbench/phpMyAdmin and save it as `database/cinema_booking_system.sql` before publishing the final repository.

### 3. Configure environment variables

Copy `.env.example` to your local environment configuration and set your own values. Do **not** commit `.env` or real credentials.

Required variables:

```text
CINEMA_DB_URL
CINEMA_DB_USER
CINEMA_DB_PASSWORD
CINEMA_SENDER_EMAIL
CINEMA_EMAIL_APP_PASSWORD
TMDB_READ_ACCESS_TOKEN
```

### 4. Open the project

Open the project folder in NetBeans and make sure the JavaFX libraries and MySQL connector are available.

### 5. Run

Run the main application class:

```text
cinemasystem.CinemaSystem
```

## 🔒 Security

API tokens, email app passwords, and database passwords are intentionally loaded from environment variables and should never be committed to GitHub.

If a credential was previously committed to a public repository, **rotate/revoke it** even after removing it from the latest files because Git history may still contain it.

## 📌 Notes

This project was developed as a university/training project to demonstrate desktop GUI development, database connectivity, CRUD operations, booking workflows, reporting, and API integration.

## 👩‍💻 Authors

**Hiba Nasrallah**

Lebanon,Tyre-IUL — Computer Science Project
