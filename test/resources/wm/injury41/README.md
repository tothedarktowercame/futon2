# Injury 41a00c62 acceptance fixtures (run fc464172, 2026-10-23)

Retained, not synthetic.  Every file here is a verbatim projection of a
retained witness; the acceptance test derives its synthetic variants from
these by a named string substitution and labels them as such.

| file | source | projection |
|---|---|---|
| `selected-entry-fc464172.edn` | `data/wm-on-prod-history/checkpoints/2026-10-23-fc464172-4f16-43d9-a055-0296ad4494b5/attempt-002.edn` (sha256 `c341ae72601fe2b7b3d25c623a8ec54541b4c65859ee3f3611148e3833a94634`, 15.5 MB, gitignored) | the `:selected-entry` value only (the one key `execute-selected-cascade!` reads); `pr-str` round-trip equal to the checkpoint value |
| `zai-2-author-reply.txt` | Agency job `invoke-1792764980051-2660-61df01f8` (zai-2, mode work), via the inbox bellback `~/.claude/agency-inbox/wm-full-loop/auto-bellback-invoke-1792764980051-2660-61df01f8.json` (sha256 `70002749ed5f8c1c…`) | the reply text between the bellback header line and the two automated trailer lines; sha256 `16a7d7363e289d95a15dd55b40d2938650ce1214bcebe540a5885e91da5faaf0` |
| `zai-3-review-reply.txt` | Agency job `invoke-1792765126468-2663-1905e6a1` (zai-3, mode work), bellback `auto-bellback-invoke-1792765126468-2663-1905e6a1.json` (sha256 `1a951b4ed5d7331e…`) | same projection; sha256 `30585f303fa3dfc8b5588ae647b84d2fa5ce8ee899a76ba978b9554ba87a9448` |

Copies of the two bellbacks are also kept outside the repo at
`~/notes/wm-next/offline-repair/injury41/current-risk/`, because Agency job
events are rolling-expiry.

The author reply names `"var": "clauses"`: the bare decision map in
`futon2.aif.wm.authored.req-fbad4b8b` (commit `a5f8c4d7d`, source sha256
`a5e34ef1cfa0f69c17ccac1fb6dbb7f04bac7cf891c0faa4546aa7eb4ab5813b`), not the
guarded function `decisions`.  That is the trigger of injury 41.
