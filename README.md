# Game progress API

## Overall
The diploma project is a set of technical components and the implementation of game logic for a platform game. The system consists of a client application developed using the Unity game engine, a backend service, and a database.

Game Progress API is a backend service for managing user game progress.
The API provides functionality for:
- CRUD for users, levels and achievements;
- saving and updating player progress;
- obtaining the current state of the game;
- working with levels, achievements, and statistics;
- integration with client game applications.

The service is designed as a scalable and extensible solution suitable for use in a production environment.

## Technology Stack
Key technologies used in the project:

- Java 21
- Spring Boot(Spring Web (REST API), Spring Data JPA, Spring Security)
- Hibernate
- PostgreSQL
- Maven
- Docker
- GitHub Actions
- JWT
- OpenAPI (Swagger)

## Application Architecture

The application follows a layered architecture pattern, separating concerns
between presentation, business logic, and data access.

### Layers

- **Controller Layer**
  Handles HTTP requests and responses and exposes REST endpoints.

- **Service Layer**
  Contains the core business logic and orchestrates application workflows.

- **Repository Layer**
  Provides access to the database using Spring Data JPA.

- **Domain Layer**
  Contains domain entities and dtos.

### Request Flow

Client(Game) → Controller → Service → Repository → Database

### Swagger
All API endpoints are available in Swagger. You can view them here: https://game-progress-api.onrender.com/swagger-ui/index.html#/

## Containerization

The project is containerized using Docker, which allows developers to
quickly run the entire environment locally during development and ensures
consistent deployment across different environments.

### Main Containers

- **Backend API** – the Java Spring Boot application running the REST API.
- **PostgreSQL** – relational database for storing game progress data.
- **Flyway** – database migration tool to automatically manage schema changes.
- **Prometheus** – monitoring system collecting metrics from the application.
- **Postgres Exporter** – exports PostgreSQL metrics to Prometheus for monitoring.

**Instructions how to use Docker:**
1) Clone repositories with API and DB code so clone this one and from the link https://github.com/KatTihanovich/game_progress_db
2) Use .env.example file to create .env and fill it with your enviroment variables. For paths to db migrations use this one where you cloned repository with db
3) Then go to the terminal and to the folder docker where you need to run command *docker-compose up --build -d*
4) Then it is possible to check the result with comand *docker-compose ps* or to post link *http://localhost:8080/actuator/health* into a browser. The status should be UP

## CI/CD
The project uses **GitHub Actions** for continuous integration and deployment.

### Workflow Triggers

- **Push and Pull Requests** to `dev` and `master` branches trigger the CI workflow.
- **Push to `master` branch** triggers deployment.
- Manual dispatch is also supported.

### Pipeline Steps

1. **Build** – compiles the application.
2. **Linting** – checks code style and formatting.
3. **Unit Tests** – runs automated unit tests.
4. **Integration Tests** – verifies integration between components.
5. **Code Coverage & Quality Check** – ensures coverage and passes **SonarCloud Quality Gate (80%)**.
6. **Deployment** – after successful completion of all steps, the application is deployed to **Render**.

After deployment the web application is available here: https://game-progress-api.onrender.com

## Testing and Code Quality

The project follows best practices to ensure high code quality and reliability.

### Testing

- **Unit Tests** – implemented using JUnit and Mockito.  
- **Integration Tests** – run with H2 in-memory database to verify interactions between components.  
- All tests are executed automatically in the CI/CD pipeline.

### Code Quality

- **Code Coverage** – measured with JaCoCo; quality gate enforced via SonarCloud.  
- **Static Code Analysis & Linting** – Checkstyle ensures consistent code style and detects potential issues.

This setup ensures that the code is well-tested, maintainable, and meets defined quality standards.
