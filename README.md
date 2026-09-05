# Training Center Management Application

A web application for managing a training center's full lifecycle: courses,
students, enrollments, scheduling, attendance, payments, grades, and PDF
certificate generation — with three roles (Administrator, Trainer, Student)
and a bonus QR-code attendance feature.

See [`Training Center Management Application.md`](Training%20Center%20Management%20Application.md)
for the original feature brief.

## Tech Stack

| Layer      | Choice |
|------------|--------|
| Backend    | Java 21, Spring Boot 3.x, Spring Web, Spring Security, Spring Data JPA |
| Auth       | JWT (stateless), role-based access control |
| Database   | PostgreSQL 16, Liquibase migrations |
| PDF        | OpenPDF for certificate generation |
| QR Codes   | ZXing |
| Frontend   | React (JavaScript) + Vite, shadcn/ui, React Router, TanStack Query |
| Containerization | Docker + Docker Compose |

Full architecture, conventions, and domain model live in
[`docs/PLAN.md`](docs/PLAN.md).

## Prerequisites

- Docker & Docker Compose
- JDK 21 (for local backend dev without Docker)
- Node 20 (for local frontend dev without Docker)

## Quick Start

```bash
cp .env.example .env
docker compose up --build
```

## Documentation

- [`docs/PLAN.md`](docs/PLAN.md) — global architecture, conventions, and
  domain model; read this first.
- [`docs/tasks/`](docs/tasks/) — one file per implementation task/branch,
  in dependency order.

## Project Status

This repository is being built incrementally, task by task, following the
roadmap in [`docs/PLAN.md`](docs/PLAN.md#7-ordered-task-list). See the
current task's file under `docs/tasks/` for what's implemented so far.

## License

[MIT](LICENSE)
