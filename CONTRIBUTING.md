# Contributing

Thank you for your interest in contributing to the Spring Boot Idempotency Starter.

## Development Setup

1. Clone the repository:

```bash
git clone https://github.com/IroshPerera/spring-boot-idempotency-starter.git
cd spring-boot-idempotency-starter
```

2. Make sure Java 17 or higher and Maven are installed.

3. Run the tests:

```bash
mvn clean test
```

## Making Changes

- Create a new branch for your change.
- Keep changes focused and small.
- Follow the existing Java and Spring Boot coding style.
- Add or update tests for new behaviour.
- Update the README when public behaviour changes.
- Do not commit secrets, credentials, or generated build files.

## Commit Messages

Use clear commit messages, for example:

```text
Add JDBC idempotency storage
Fix Redis response serialization
Improve request hash validation
```

## Pull Requests

Before opening a pull request:

```bash
mvn clean test
```

Please include:

- A short description of the change
- Tests added or updated
- Any configuration changes
- Any breaking changes

## License

By contributing to this project, you agree that your contributions will be licensed under the MIT License.