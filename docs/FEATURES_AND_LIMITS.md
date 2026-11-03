# Features and implementation status

| Area | In this release | Not included yet |
|---|---|---|
| Launcher/UI | Native main activity, navigation and screens | Tablet/desktop adaptive workspaces |
| Projects | Local create/list/rename/delete and templates | Project version history and autosync |
| Files/editor | Create/open/edit/save, simple token coloring | LSP, autocomplete, formatting, refactoring, diagnostics from compilers |
| ZIP | Import/export using SAF, traversal and size guards | Cloud backup |
| GitHub | PAT validation, repositories, upload, source ZIP import, workflow dispatch/history and artifact ZIP download through Android document picker | Full Git command-line integration, OAuth device flow, pull-request UI |
| AI | OpenAI-compatible, Gemini, Claude request clients | Inline code edits, streaming, conversation history and model catalog sync |
| Security | Keystore encrypted secrets, SHA-256 and optional VirusTotal upload | Local malware engine, code signing, reproducible-build attestation |
| Extensions | Metadata registry and enable/disable flag | Sandboxed extension execution, SDK and marketplace |
| Localization | 16 locale choices and RTL direction | Complete translation of every screen and dialog in all 16 languages |
| Build | CI workflow builds XCode Studio debug APK | On-device build toolchains for all languages and automatic signing/release |

A feature listed in the second column is present in code, but external services still require valid credentials and permissions.
