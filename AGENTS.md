# Project AGENTS

## 語言

- 以繁體中文（zh-TW）思考並回覆。

## Plan 執行流程

- `docs/plan.md` 是唯一的未完成計畫來源。
- `docs/specs/` 是唯一的 spec 來源；不要再把 spec 留在 `docs/` 根目錄。
- 所有規劃與 plan 執行必須走 `/do` skill 路由，不可繞過 `/do` 直接進行自訂流程。
- 在規劃階段，若流程使用 `$do` / `$pld`，必須把所有目前沒有被依賴阻塞的候選 plans 一次送進同一輪規劃與執行批次，不可只挑眼前的一個 plan 單獨往下做。
- 規劃時要盡可能一路推進到各個決策點，提前整理中途可能需要使用者決斷的問題，避免執行到一半才回頭補問。
- 同一輪送進 `$do` / `$pld` 的未阻塞 candidates，預設一路執行到完成並合併回主線後，再開始下一輪規劃；除非途中出現新阻塞、需求變更，或使用者明確改道。
- 開始做下一個 plan 前，先執行 `just plan-ready`（或相容別名 `just plan-next`），只從目前無相依 target 中挑選。
- 完成某個 plan 後，必須先把對應行為更新到 `docs/specs/*.md`，並在至少一份 spec 中留下該 plan keyword。
- spec 更新完成後，再執行 `just plan-done <target-or-keyword>`（或相容別名 `just plan-complete <target-or-keyword>`）。
- `just plan-done` 會先檢查 plan keyword 是否已出現在 `docs/specs/`，且命中的 spec 目前必須有 staged 或 unstaged 變更；未達條件時不得移除該 plan。
- `just plan-done` 會同時移除該 target，並清掉其他 target 對它的依賴標註。

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

## 收尾整合流程

- 當 implementation 完成且 review / 驗證通過後，必須執行 `/finishing-a-development-branch` 完成整合與清理流程。
- 任一分支只要已完成並合併（例如 merge / rebase + ff merge），都必須在同一收尾流程中完成清理（至少包含 branch 與對應 worktree），不可延後或跳過。

## Android 本機環境

- 本專案建置與測試依賴 Android SDK `platforms;android-35`、`build-tools;35.0.0`、`platform-tools`。
- 若 Gradle 出現 `SDK location not found`，優先檢查 `local.properties` 或 `ANDROID_HOME` / `ANDROID_SDK_ROOT`。
- `local.properties` 屬於本機檔案，不提交；需要時可設為 `sdk.dir=/home/jethro/Android/Sdk`。
