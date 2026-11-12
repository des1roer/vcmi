# Заметки для ИИ-агентов (локальный checkout VCMI)

Обновлено: 2026-11-12. Основные правила проекта — в `AGENTS.md`, здесь только то, чего там нет.

## Репозиторий

- Путь: `C:\dev\vcmi`, ветка по умолчанию — `develop`.
- `origin` = форк `git@github.com:des1roer/vcmi.git` (сюда можно пушить).
- `upstream` = `https://github.com/vcmi/vcmi.git` (только fetch; PR/issue/комментарии туда не создавать — см. `AGENTS.md` и `CONTRIBUTING.md#use-of-ai`).
- Синхронизация форка: `git fetch upstream && git merge --ff-only upstream/develop`.
- Пользователь GitHub: `des1roer`. `gh` CLI установлен (`C:\Program Files\GitHub CLI`).

## Окружение

- Windows 11, инструмент Bash — это Git Bash (POSIX-пути `/c/dev/vcmi`).
- В `PATH` нет `cmake`, `conan`, `ninja`, `cl`: сборки в этом окружении пока не настраивались, каталога сборки нет.
  Перед сборкой нужно поставить Visual Studio (MSVC), CMake и Conan — см. `docs/developers/Building_Windows.md` и `docs/developers/Conan.md`.
  Пресеты для Windows в `CMakePresets.json`: `windows-msvc-release`, `windows-msvc-ninja-release` и др.
- Установлен Vulkan SDK 1.4.363.0 (`C:\VulkanSDK`) — пригодится для `clientsdl3` (SDL3/GPU).
- Данных H3 / пользовательского каталога VCMI (`Documents\My Games\vcmi`) на машине не найдено — игру запустить для проверки пока нельзя.

## Как работать

- Пушить только в форк (`origin`), отдельной веткой, а не в `develop`.
- Коммиты заканчивать строкой `Co-Authored-By`, как требует харнесс.
- Для ревью изменений есть скилл `vcmi-code-review`.
