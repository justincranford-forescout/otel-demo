# copilot.instructions.md

## Project Overview
This repository is a Java Spring Boot application using Gradle. It demonstrates OpenTelemetry (otel) integration for distributed tracing and observability.

## Coding Guidelines
- Use Java 17+ features where appropriate.
- Follow standard Spring Boot conventions for controllers, services, and repositories.
- Use dependency injection via `@Autowired` or constructor injection.
- Write REST endpoints using `@RestController` and `@RequestMapping`.
- Use Gradle for dependency management and builds.
- Place configuration in `application.properties` or `application.yml`.

## Testing
- Write unit tests using JUnit 5.
- Use Mockito for mocking dependencies.
- Place tests in `src/test/java`.

## Documentation
- Document public classes and methods with Javadoc.
- Add comments for complex logic.

## Pull Request Requirements
- Ensure all tests pass.
- Follow the code style and guidelines above.
- Include a clear description of changes.

## Copilot Usage
- Generate code that adheres to these guidelines.
- Prefer idiomatic Java and Spring Boot patterns.
- Suggest improvements for observability and tracing.
