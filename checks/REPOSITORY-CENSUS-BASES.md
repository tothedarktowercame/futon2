# Repository census bases

An audit or census that reports repository state must register its observation
basis in `repository-census-bases.edn`. Each entry names the artifact, its kind,
the full commit for every repository observed, and the exact source files whose
state supports the result. Line numbers may aid reading but are not identity.

The checker compares each cited file between the recorded commit and the
current tree. A changed subject produces `:possibly-stale` at exit 0: movement
does not prove the census wrong. Missing commits, malformed entries, missing
artifacts, and untracked subjects are unavailable evidence and fail loudly.

Only registered audit/census artifacts are in scope. This deliberately avoids
turning every prose document or every repository commit into a freshness alarm.
New repository-state censuses must add a registry entry in their delivery.
