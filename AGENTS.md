# Project AGENTS

## 語言

- 以繁體中文（zh-TW）思考並回覆。

## Plan 執行流程

- `docs/plan.md` 是唯一的未完成計畫來源。
- `docs/specs/` 是唯一的 spec 來源；不要再把 spec 留在 `docs/` 根目錄。
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
