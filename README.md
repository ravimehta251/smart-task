# SmartTask Enterprise

> A scalable, production-grade enterprise project management platform built with Spring Boot 3 — combining Jira + Trello + Slack capabilities into a single unified backend.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Security | Spring Security 6 + JWT |
| Database | MySQL 8 |
| ORM | Spring Data JPA + Hibernate |
| Migrations | Flyway |
| Validation | Jakarta Bean Validation |
| Documentation | OpenAPI 3 / Swagger UI |
| Build | Maven 3.9 |
| Containers | Docker + Docker Compose |
| Testing | JUnit 5 + Mockito |
| Async | Spring @Async with ThreadPoolTaskExecutor |

---

## Modules

| # | Module | Description |
|---|---|---|
| 1 | Authentication | Register, login, JWT, refresh tokens, email verification, password reset |
| 2 | Organization | Multi-tenant — manage companies/organizations |
| 3 | Team | Teams within organizations, member management |
| 4 | User | Employee profiles, roles, activation |
| 5 | Project | Project lifecycle, members, dashboard |
| 6 | Task | Core module — create, assign, track, filter tasks |
| 7 | Comment | Task-level discussion threads |
| 8 | File Attachment | Upload/download files attached to tasks |
| 9 | Notification | In-app notifications for task events |
| 10 | Activity Log | Full audit trail of every action |

---

## Quick Start with Docker

```bash
# 1. Clone the repository
git clone https://github.com/ravimehta251/smart-task
cd smarttask-enterprise

# 2. Start everything (MySQL + App)
docker compose up --build

# 3. API is live at:
#    http://localhost:8080/api/v1
#    Swagger UI: http://localhost:8080/api/v1/swagger-ui.html
```

---

## Running Locally (without Docker)

**Prerequisites:** Java 21, Maven 3.9+, MySQL 8

```bash
# 1. Create the database
mysql -u root -p -e "
  CREATE DATABASE smarttask_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE USER 'smarttask_user'@'localhost' IDENTIFIED BY 'smarttask_pass';
  GRANT ALL PRIVILEGES ON smarttask_db.* TO 'smarttask_user'@'localhost';
  FLUSH PRIVILEGES;
"

# 2. Run the application
./mvnw spring-boot:run

# Or on Windows:
mvnw.cmd spring-boot:run
```

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `smarttask_db` | Database name |
| `DB_USERNAME` | `smarttask_user` | DB username |
| `DB_PASSWORD` | `smarttask_pass` | DB password |
| `JWT_SECRET` | (hex key) | JWT signing key — **change in production** |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP host |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | — | Email sender address |
| `MAIL_PASSWORD` | — | Email sender password |
| `FILE_UPLOAD_DIR` | `./uploads` | Directory for uploaded files |
| `FRONTEND_URL` | `http://localhost:3000` | Used in email links |

---

## API Reference

All endpoints are prefixed with `/api/v1`.

### Authentication
| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/register` | Register new account |
| POST | `/auth/login` | Login → JWT tokens |
| POST | `/auth/refresh-token` | Refresh access token |
| POST | `/auth/logout` | Revoke refresh tokens |
| GET | `/auth/verify-email?token=` | Verify email address |
| POST | `/auth/forgot-password` | Request password reset email |
| POST | `/auth/reset-password` | Reset password with token |
| POST | `/auth/change-password` | Change password (authenticated) |
| GET | `/auth/me` | Get current user profile |

### Organizations
| Method | Endpoint | Description |
|---|---|---|
| POST | `/organizations` | Create organization |
| GET | `/organizations` | List all (with search, pagination) |
| GET | `/organizations/{id}` | Get by ID |
| PUT | `/organizations/{id}` | Update |
| DELETE | `/organizations/{id}` | Soft delete |
| PATCH | `/organizations/{id}/status` | Change status |

### Teams
| Method | Endpoint | Description |
|---|---|---|
| POST | `/teams` | Create team |
| GET | `/teams/{id}` | Get by ID |
| GET | `/teams/organization/{orgId}` | List by org |
| PUT | `/teams/{id}` | Update |
| DELETE | `/teams/{id}` | Delete |
| POST | `/teams/{teamId}/members/{userId}` | Add member |
| DELETE | `/teams/{teamId}/members/{userId}` | Remove member |

### Users
| Method | Endpoint | Description |
|---|---|---|
| GET | `/users` | Search users (pagination, filter, sort) |
| GET | `/users/{id}` | Get by ID |
| GET | `/users/me` | Own profile |
| PUT | `/users/{id}` | Update profile |
| DELETE | `/users/{id}` | Soft delete |
| PATCH | `/users/{id}/role` | Change role |
| PATCH | `/users/{id}/deactivate` | Deactivate |
| PATCH | `/users/{id}/activate` | Activate |

### Projects
| Method | Endpoint | Description |
|---|---|---|
| POST | `/projects` | Create project |
| GET | `/projects/{id}` | Get by ID |
| GET | `/projects/organization/{orgId}` | List by org |
| GET | `/projects/my` | My projects |
| PUT | `/projects/{id}` | Update |
| DELETE | `/projects/{id}` | Delete |
| PATCH | `/projects/{id}/archive` | Archive |
| PATCH | `/projects/{id}/status` | Change status |
| POST | `/projects/{id}/members/{userId}` | Add member |
| DELETE | `/projects/{id}/members/{userId}` | Remove member |
| GET | `/projects/{id}/dashboard` | Task statistics |

### Tasks
| Method | Endpoint | Description |
|---|---|---|
| POST | `/tasks` | Create task |
| GET | `/tasks/{id}` | Get by ID |
| GET | `/tasks/project/{projectId}` | Search in project |
| GET | `/tasks/my` | My assigned tasks |
| PUT | `/tasks/{id}` | Update |
| DELETE | `/tasks/{id}` | Delete |
| PATCH | `/tasks/{taskId}/assign/{userId}` | Assign to user |
| PATCH | `/tasks/{taskId}/status` | Change status |

### Comments
| Method | Endpoint | Description |
|---|---|---|
| POST | `/tasks/{taskId}/comments` | Add comment |
| GET | `/tasks/{taskId}/comments` | List comments |
| PUT | `/tasks/{taskId}/comments/{id}` | Edit comment |
| DELETE | `/tasks/{taskId}/comments/{id}` | Delete comment |

### File Attachments
| Method | Endpoint | Description |
|---|---|---|
| POST | `/attachments/tasks/{taskId}/upload` | Upload file |
| GET | `/attachments/tasks/{taskId}` | List task attachments |
| GET | `/attachments/{id}/metadata` | File metadata |
| GET | `/attachments/{id}/download` | Download file |
| DELETE | `/attachments/{id}` | Delete file |

### Notifications
| Method | Endpoint | Description |
|---|---|---|
| GET | `/notifications` | All notifications |
| GET | `/notifications/unread` | Unread only |
| GET | `/notifications/unread/count` | Unread count |
| PATCH | `/notifications/{id}/read` | Mark as read |
| PATCH | `/notifications/read-all` | Mark all as read |
| DELETE | `/notifications/{id}` | Delete |

### Activity Logs
| Method | Endpoint | Description |
|---|---|---|
| GET | `/activity-logs` | All logs (admin) |
| GET | `/activity-logs/entity/{type}/{id}` | Logs for entity |
| GET | `/activity-logs/user/{userId}` | Logs by user |

---

## Roles & Permissions

| Role | Capabilities |
|---|---|
| `SUPER_ADMIN` | Full system access |
| `ORGANIZATION_ADMIN` | Manage own organization |
| `PROJECT_MANAGER` | Manage projects and tasks |
| `TEAM_LEAD` | Manage assigned team |
| `DEVELOPER` | Create/update assigned tasks |
| `TESTER` | Update testing-related tasks |
| `VIEWER` | Read-only access |

---

## Running Tests

```bash
./mvnw test
# or on Windows:
mvnw.cmd test
```

---

## Architecture

```
Client
  │
  ▼
Spring Boot API (port 8080)
  │
  ├── SecurityFilterChain (JWT filter → role-based access)
  │
  ├── Controllers  (REST endpoints, input validation)
  │       │
  ├── Services     (business logic, transactions)
  │       │
  ├── Repositories (Spring Data JPA queries)
  │       │
  └── MySQL 8      (Flyway-managed schema)
```

**Cross-cutting concerns run async** (non-blocking):
- `ActivityLogService` — fire-and-forget audit writes  
- `NotificationService` — async in-app notification creation  
- `EmailService` — async email dispatch via Spring Mail
