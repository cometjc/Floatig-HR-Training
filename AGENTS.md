# Project AGENTS

## 語言

- 以繁體中文（zh-TW）思考並回覆。

## Plan 執行流程

- `docs/plan.md` 是唯一的未完成計畫來源。
- `docs/specs/` 是唯一的 spec 來源；不要再把 spec 留在 `docs/` 根目錄。
- 所有規劃與 plan 執行必須走 `/do`；路由、AUQ、review continuity、完成條件以 `/do` skill 規則為準。
- 開始做下一個 plan 前，先執行 `just plan-ready`（或相容別名 `just plan-next`），只從目前無相依 target 中挑選。
- 完成某個 plan 後，必須先把對應行為更新到 `docs/specs/*.md`，並在至少一份 spec 中留下該 plan keyword。
- 若使用 `/pld`，`plan-done` 由 coordinator 統一執行（lane 只處理實作 + spec）。

## Spec 驗證流程

- 當需求涉及 spec 整理或落地確認時，優先把 spec 切成可獨立驗證的單位，例如 screens、overlay、prediction。
- 若使用者要求平行確認 spec 落地狀況，可派多個 subagent 分頭比對 spec 與實作，再整合成單一差異清單。
- 整合差異時，優先標出：
  - spec 已寫但實作不符
  - 實作已有但 spec 未記錄
  - 哪些 plan 可因 spec 已落地而標記完成

## 開發工作區

- 預設使用 repo 內的 `.worktrees/` 作為隔離開發目錄。
- `.worktrees/` 必須保持在 `.gitignore` 中；建立新的實作分支前先確認沒有被移除。
- 任何會寫入檔案的實作工作開始前，必須先執行 `/using-git-worktrees` 以建立/確認隔離工作區。
- lane/subagent git context 檢查統一使用：`$HOME/.agents/skills/pld/scripts/ensure_git_context.sh`。
- `/pld` 執行細節（preflight、dispatch mode、驗證分級、整合策略）以 `/pld` skill 規則為準。

## 收尾整合流程

- 當 implementation 完成且 review / 驗證通過後，必須執行 `/finishing-a-development-branch` 完成整合與清理流程。
- 任一分支只要已完成並合併（例如 merge / rebase + ff merge），都必須在同一收尾流程中完成清理（至少包含 branch 與對應 worktree），不可延後或跳過。

## Android 本機環境

- 本專案建置與測試依賴 Android SDK `platforms;android-35`、`build-tools;35.0.0`、`platform-tools`。
- 若 Gradle 出現 `SDK location not found`，優先檢查 `local.properties` 或 `ANDROID_HOME` / `ANDROID_SDK_ROOT`。
- `local.properties` 屬於本機檔案，不提交；需要時可設為 `sdk.dir=/home/jethro/Android/Sdk`。
