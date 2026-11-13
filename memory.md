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
- В `PATH` нет `cmake`, `conan`, `ninja` — подтверждено 2026-11-13, несмотря на попытку `python -m pip install --upgrade pip conan cmake ninja`
  (см. ниже, «Попытка сборки релиза»). Перед сборкой нужно поставить их заново — см. `docs/developers/Building_Windows.md` и `docs/developers/Conan.md`.
  Пресеты для Windows в `CMakePresets.json`: `windows-msvc-release`, `windows-msvc-ninja-release` и др.
- Visual Studio 2022 и Python 3.14 установлены (`C:\Program Files (x86)\Microsoft Visual Studio\2022`, `vswhere.exe` там же). MSVC-тулчейн доступен, просто не в PATH у Git Bash.
- Установлен Vulkan SDK 1.4.363.0 (`C:\VulkanSDK`) — пригодится для `clientsdl3` (SDL3/GPU).
- Данных H3 / пользовательского каталога VCMI (`Documents\My Games\vcmi`) на машине не найдено — игру запустить для проверки пока нельзя.
- `git submodule status` в корне репозитория показывает, что сабмодули уже проинициализированы (`dependencies`, `client/lib/discord-presence`, `launcher/lib/innoextract`, `test/googletest`).

## Попытка сборки релиза (2026-11-12/13, не завершена)

Пользователь попросил собрать релиз в `C:\dev\vcmi\releases` (Windows x64 MSVC, автоматическая установка тулчейна и зависимостей — подтверждено явно через вопрос пользователю).

Что сделано:
- Определена версия репозитория: `cmake_modules/VersionDefinition.cmake` → `1.8.0` (develop, релиз ещё не выпущен).
- Сабмодуль `dependencies` закреплён на коммите с датой `2026-10-13` — это совпадает с pre-release `2026-10-13` в `vcmi/vcmi-dependencies` на GitHub (проверено через `gh release list --repo vcmi/vcmi-dependencies`).
- Скачан `dependencies-windows-x64.txz` (~94 МБ) из этого релиза в `C:\Users\des\Downloads\dependencies-windows-x64.txz` — **файл лежит и готов**, не нужно качать заново.
- Запущен `python -m pip install --upgrade pip conan cmake ninja` в фоне — **не завершился / не подтверждён**: при проверке в новой сессии `conan`/`cmake`/`ninja` по-прежнему не найдены (`pip show` тоже ничего не находит). Возможно, процесс был прерван перезапуском сессии (таск `bpyc893xj` получил статус `stopped` с пометкой «didn't finish before the previous session ended»). **Нужно перезапустить установку и дождаться её явного завершения, прежде чем продолжать.**
- Каталог `C:\dev\vcmi\releases` и `C:\dev\vcmi\release` существуют, но пустые — релиз ещё не собран.

Следующие шаги (не начаты):
1. Переустановить `conan`, `cmake`, `ninja` через pip и дождаться завершения (проверять `--version` после, не полагаться на факт запуска команды).
2. `conan profile detect`, затем `conan cache restore "C:\Users\des\Downloads\dependencies-windows-x64.txz"` из корня `C:\dev\vcmi`.
3. `conan install . --output-folder=conan-msvc --build=never --profile=dependencies\conan_profiles\msvc-x64 -s "&:compiler.version=<версия из profile detect>" -s "&:build_type=Release"` (подробности — `docs/developers/Building_Windows.md`).
4. `cmake --preset windows-msvc-ninja-release` (или `windows-msvc-release` для VS-генератора), затем сборка и упаковка (CPack → ZIP/NSIS, см. `CMakeLists.txt` ~строка 910-980).
5. Итоговый артефакт положить/скопировать в `C:\dev\vcmi\releases`.

## Как работать

- Пушить только в форк (`origin`), отдельной веткой, а не в `develop`.
- Коммиты заканчивать строкой `Co-Authored-By`, как требует харнесс.
- Для ревью изменений есть скилл `vcmi-code-review`.
