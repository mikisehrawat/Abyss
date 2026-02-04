# Abyss

A Java web application built with **Spring MVC**, **Spring Data JPA**, and **Lombok**, using **Jakarta** (`jakarta.*`) APIs.  
This repository is Maven-based and includes a Docker Compose setup for local development.

---

## Tech Stack

- **Java + Maven Wrapper** (`mvnw`, `mvnw.cmd`)
- **Spring MVC** (web layer)
- **Spring Data JPA** (persistence)
- **Jakarta EE imports** (`jakarta.*`)
- **Lombok** (boilerplate reduction)
- **Docker Compose** (`compose.yaml`) for local dependencies

---

## Project Structure (high level)

- `src/` — application source code
- `pom.xml` — Maven configuration
- `compose.yaml` — local dev services (e.g., database)
- `target/` — build output (generated)
- `Readme.md` — this file

---

## Prerequisites

- **JDK** (use the version configured for your project; commonly 17+)
- **Docker Desktop** (optional, for `compose.yaml`)
- No need to install Maven globally (wrapper is included)

---

## Getting Started

### 1) Clone the repository