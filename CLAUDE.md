# NoExit (renewal)

## Shell conventions (this repo, Windows + Git Bash)
- Run commands from the repository root. Do not `cd` into subfolders; use root-relative paths (e.g. `cat src/main/java/...`, `grep -rn X src/`).
- Never read or print `local-env.ps1`. Required env vars are already loaded in the session.
- Stop the app by PID only: `netstat -ano | grep LISTENING | grep ':8080 '` then `taskkill //PID <pid> //F`. Never kill all java processes.
- Local sessions: commit when asked; never push (a human pushes).
- GitHub Actions (claude-code-action): push only to your own claude/* working branch with the provided git-*.sh scripts, one script per Bash call (do not chain them with && or ;). Never push to main.
- JSON bodies with Korean text: write the body to a file with `printf` and send it with `curl --data-binary @file` (inline Korean is sent as CP949). `python` is not available (Windows Store stub).
- Seed accounts (InitialDataLoader): `user@test.com` / `owner@test.com`, password `12341234`; login body needs `"role": "USER" | "OWNER"`.
- Git: the working directory is already the repository root, so run `git ...` directly (no `git -C <path>`).
- Edit files (including ticking plan checkboxes) with the Edit tool, not `sed -i`.
- Seed owner `test@test.com` / `12341234` also logs in as OWNER (password now encoded in InitialDataLoader).