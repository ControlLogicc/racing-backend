# 🐎 HorseRacing — Backend

Backend service for **HorseRacingMVP**, a full-stack web application for managing horse racing operations, including race scheduling, event management, and role-based access control.

---

## 📋 Overview

This service exposes a RESTful API that powers race scheduling, event management, and secure role-based access for the HorseRacingMVP platform. It handles authentication, authorization, and all core business logic for the racing ecosystem.

## ✨ Key Features

- **Race & Schedule Management** — REST endpoints for creating, updating, and tracking races and event calendars
- **Role-Based Access Control (RBAC)** — 6 distinct user roles with tailored permissions:
  - **Admin** — full system control, user and race management
  - **Staff** — operational management of races and events
  - **Owner** — manages horses and views race entries
  - **Jockey** — views assigned races and schedules
  - **User** — general registered access
  - **Spectator** — public/read-only access to race information
- **JWT Authentication** — secure, stateless login and session management
- **RESTful API Design** — clean, resource-based endpoints following REST conventions

## 🛠️ Tech Stack

| Component | Technology |
|---|---|
| Language | Java |
| Framework | Spring Boot |
| Database | Microsoft SQL Server |
| Authentication | JWT (JSON Web Token) |
| Build Tool | Maven |

## 👥 My Role — Team Coordinator & Backend Contributor

As team coordinator for this 4-member project, I was responsible for:
- Breaking down project requirements into tasks and delegating them across the team
- Managing timelines and tracking deliverables to keep the project on schedule
- Contributing directly to backend development, including the JWT authentication and role-based access control system
- Coordinating API integration with the [frontend repository](../horseracing-frontend)

## 🚀 Getting Started

### Prerequisites
- Java JDK 17+
- Maven
- SQL Server

### Setup
```bash
git clone <this-repo-url>
cd horseracing-backend
mvn clean install
mvn spring-boot:run
```

Configure your database connection in `src/main/resources/application.properties` before running.

## 📌 Project Status

Developed as an academic MVP (Minimum Viable Product) for the Software Engineering / Software Testing coursework at FPT University, demonstrating REST API design, authentication/authorization architecture, and team collaboration.

---

*Developed by a 4-member team at FPT University, Ho Chi Minh City Campus.*
