# E6b store protocol review — 20666

Subjectc65b241f, seven source pins match. Documentation review only; no store creation or tests.

Adopt proposed single HEAD selecting immutable state/application transaction as an isolated implementation design, with lead corrections in the note. Direct CREATE_NEW streaming to digest path is unsafe under torn writes; use synced temporary bytes and atomic no-overwrite final publication. Keep OS lease for owner lifetime even while preparing proposals outside the process lock. Retry identity must fix committed-at or reuse existing committed record. Genesis needs explicit schema and interrupted initialization behavior; chain generations/store identity must be exact and acyclic. Local chain consistency does not prove no rollback without an external expected head.

No production safety or storage enforcement accepted yet. Next worker independently reviews corrections then implements only an isolated tempfile store with fault injection and same-/cross-process exclusion; no production root, migration, controller, emitter or runtime action. Complete externally witnessed outcome/source ownership and completeness acceptance remain unavailable.
