# Repository Guidelines

## Project Structure & Module Organization
- Code: `src/main/kotlin/com/example/bossai/` (core, AI goals, registry) and `src/main/kotlin/com/example/bossai/client/` (client-only renderers).
- Resources: `src/main/resources/` with `META-INF/neoforge.mods.toml`, language files under `assets/boss_ai/lang/`, and data (loot tables) under `data/boss_ai/`.
- No tests yet. Add tests under `src/test/kotlin/` mirroring package layout.

## Build, Test, and Development Commands
- Build: `./gradlew build`
  - Produces a mod JAR in `build/libs`. Uses Java 21 toolchain.
- Run client (dev): `./gradlew runClient`
  - Launches Minecraft with the mod in a dev environment.
- Clean: `./gradlew clean`
- Optional model: place `config/boss_ai/boss_tactics.onnx` for AI tactic inference (fallback rules are used if missing).

## Coding Style & Naming Conventions
- Language: Kotlin. Follow official Kotlin style (4-space indent, 100–120 col soft limit).
- Packages: lowercase (`com.example.bossai`), Mod ID: `boss_ai` (lowercase, underscores allowed).
- Classes/Types: `UpperCamelCase`; functions/vars: `lowerCamelCase`; constants: `UPPER_SNAKE_CASE`.
- Keep changes minimal and focused; prefer small, composable functions. Avoid inline comments for obvious code.

## Testing Guidelines
- Framework: JUnit 5 (recommended). Place tests in `src/test/kotlin`.
- What to test: AI selection (`TacticsModel.selectTactic`), goal behaviors with small fakes/mocks.
- Naming: mirror source package; method names describe behavior, e.g., `selectsBurstAoeWhenClose()`.
- Run: `./gradlew test` (add once tests are introduced).

## Commit & Pull Request Guidelines
- Commits: use Conventional Commits (e.g., `feat: add spawn egg`, `fix: renderer crash`).
- Branches: `feature/<short-name>`, `fix/<short-name>`.
- PRs: include summary, rationale, screenshots/logs if UI/runtime behavior changes, and steps to verify (build/run commands). Reference issue IDs.
- Ensure `./gradlew build` passes before requesting review.

## Security & Configuration Tips
- Do not commit generated `run/` or `runs/` directories or private files; they are ignored by `.gitignore`.
- Keep external model files out of the repo; use the `config/boss_ai/` path locally.
