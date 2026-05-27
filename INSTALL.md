# futon2 — Install / Bring-up Notes

This document captures the architectural gotchas that surface during a clean bring-up of futon2's apps — especially the **Web War Machine** under `web/war-machine/`.  Written 2026-05-26 after Rob's mfuton evolver session surfaced four real items + an architecture question on first contact.

Treat this as the orientation any downstream user (or any operator running a clean checkout on a new machine) needs before debugging "missing files" or "stale URLs."

---

## TL;DR — what you need to bring up the Web War Machine

1. **futon3c must be present + serving on `:7070`** (the One JVM). It owns most of the `/api/alpha/*` endpoints the SPA depends on.
2. **`~/code/storage/futon2/web/war-machine/resources/public/index.html` must exist** (the SPA shell with inlined CSS). The in-repo path `resources/public/index.html` is a **symlink** to it, so a `git clone` alone will not give you the SPA host page.
3. **shadow-cljs proxy `:proxy-url "http://localhost:7070"` is correct, not stale** — it routes non-asset requests to futon3c, which is where the AIF + emacs-bridge endpoints live.
4. **`clojure -M:run` binds futon2's war-machine JVM on `:3110`** — that's a separate process from futon3c and serves only the `/api/war-machine` JSON snapshot (the canonical scan output). Everything else the SPA fetches is proxied to futon3c.

If these four pieces are in place, `npm run dev` will succeed and the live UI will render.

---

## Architecture — who serves what

The Web War Machine is **NOT a self-contained app.**  Like Web Arxana (`futon4/dev/web/webarxana/`), it's an SPA that depends on the **One JVM** (per `futon3c/CLAUDE.md` I-0): one Java process at `:7070` that hosts multiple services.

| Endpoint | Server | Where in code |
|---|---|---|
| `/api/war-machine` (legacy alias) | futon2 war-machine JVM on `:3110` | `web/war-machine/src/war_machine/server/core.clj` |
| `/api/alpha/war-machine` | futon2 war-machine JVM on `:3110` (BUT also served by futon3c on `:7070` per cache; the dev SPA hits futon3c) | same; mirrored in futon3c `transport/http.clj` |
| `/api/alpha/aif-stack/live` | **futon3c only** on `:7070` | `futon3c/src/futon3c/transport/http.clj` `handle-aif-stack-live` (~line 4380) |
| `/api/alpha/war-machine/show-in-emacs` | **futon3c only** on `:7070` | `futon3c/src/futon3c/transport/http.clj` `handle-show-in-emacs` (~line 4421) |
| static assets (`/`, `/js/*`, `/index.html`) | shadow-cljs dev-http on `:8710` (dev); `clojure -M:run` ring resource handler on `:3110` (prod) | shadow-cljs.edn `:dev-http` block; war-machine `server.core` `:not-found` |

So when you read the war-machine server's docstring saying "one data endpoint — `/api/war-machine` — returns the snapshot as JSON," that's accurate about THIS JVM's scope but stylistically misleading — the SPA depends on multiple endpoints owned by futon3c.  The shadow-cljs `:proxy-url` is what stitches them together in dev.

The same architecture applies to **Web Arxana** (`futon4/dev/web/webarxana/`): its app code lives in futon4, but its backend services come from the futon3c JVM.

---

## Common surprises during bring-up

### "index.html doesn't exist in the repo"

`web/war-machine/resources/public/index.html` is a **symlink**:

```
resources/public/index.html → /home/joe/code/storage/futon2/web/war-machine/resources/public/index.html
```

The symlink target is **out-of-repo**, in `~/code/storage/`.  A `git clone` of futon2 alone will NOT give you the SPA host page — you also need the corresponding `~/code/storage/` directory.

The 19,151-byte file at the target is the SPA shell with **inlined `<style>` block** (same pattern as Web Arxana — webarxana inlines its styles in `<style>` inside its index.html too).

**Why out-of-repo**: the SPA shell is treated as a build/deploy artefact for the operator's deployment shape, not as canonical source.  Changes to it should be made in the storage location and rsync'd to other machines manually.  This is a deliberate choice, not an oversight — but it does mean fresh clones need an extra step.

**Verification**:
```bash
test -f /home/joe/code/storage/futon2/web/war-machine/resources/public/index.html && echo "OK"
ls -la /home/joe/code/futon2/web/war-machine/resources/public/index.html
# Expected: lrwxrwxrwx ... -> /home/joe/code/storage/futon2/web/war-machine/resources/public/index.html
```

### "I can't find a CSS file"

The CSS lives in the `<style>` block inside `index.html` — see above.  There is no separate `.css` file.  The 85+ class-name references in `src/war_machine/client/*.cljs` (`.toolbar`, `.title`, `.legend-card`, etc.) are styled by that inlined block.

If you want to extend styles, edit the `<style>` block in the symlink target (`~/code/storage/futon2/web/war-machine/resources/public/index.html`).

### "The `/api/alpha/aif-stack/live` endpoint doesn't exist"

It does — in **futon3c**, not in futon2.  If your tree lacks futon3c, or your futon3c checkout predates the endpoint's commit (e.g. you're on a snapshot from before 2026-05-22), you won't find it.

Today's check (run from any host that has futon3c on the classpath):
```bash
grep -n "aif-stack/live" /home/joe/code/futon3c/src/futon3c/transport/http.clj
# Expected: handler defn around line 4380; route case around line 4625
```

Live endpoint check:
```bash
curl -sS http://localhost:7070/api/alpha/aif-stack/live | python3 -m json.tool | head -20
# Expected: JSON with :reading :next-move-live + :reading :next-move + :stack-nodes + :live-mission-count
```

### "The `/api/alpha/war-machine/show-in-emacs` endpoint doesn't exist"

Same shape as above — owned by futon3c, route at line 4629 of `transport/http.clj`.

### "shadow-cljs `:proxy-url` looks stale"

It points at `http://localhost:7070` (futon3c).  This is CORRECT.  Confusion arises when futon3c isn't running or the operator isn't aware that futon3c serves the SPA's secondary endpoints.

If `npm run dev` fails because the proxy can't reach the target: start futon3c first (`cd ~/code/futon3c && clojure -M:run` from its own deps.edn alias).

### "futon3c is a giant repo — do I need it all?"

Yes, for a full bring-up.  futon3c is the One JVM (per its `CLAUDE.md` I-0): it hosts everything that serves at runtime (futon3c API on :7070; WebArxana app on :3100; substrate-2 on :7071; futon1a hyperedge store; Drawbridge nREPL on :6768).  A futon2-only checkout will produce a half-working SPA where `/api/war-machine` returns valid JSON but the AIF + emacs-bridge surfaces are 404.

---

## Quick reproducible verification — full bring-up check

Run these in order:

```bash
# 1. futon3c JVM is up + serves the load-bearing endpoints
curl -sS http://localhost:7070/api/alpha/aif-stack/live | jq '.live?' | grep -q true && echo "AIF stack OK"
curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:7070/api/alpha/war-machine
# Expected: 200

# 2. futon2 war-machine JVM is up + serves the snapshot
curl -sS http://localhost:3110/api/war-machine | jq '.["scan-window"]' | grep -q days && echo "snapshot OK"

# 3. The SPA shell is reachable
test -f /home/joe/code/storage/futon2/web/war-machine/resources/public/index.html && echo "index.html OK"
test -L /home/joe/code/futon2/web/war-machine/resources/public/index.html && echo "symlink OK"

# 4. shadow-cljs dev server (only needed if editing CLJS)
curl -sS http://localhost:8710/index.html | head -3 | grep -q "<html" && echo "dev-http OK"

# 5. Compile + run e2e tests
cd ~/code/futon2/web/war-machine
npm run build
npm run test:e2e
```

If all five pass, the Web War Machine is operationally healthy.

---

## Provenance + acknowledgements

This INSTALL document was prompted by Rob's mfuton evolver session (2026-05-26), which surfaced four "missing file/endpoint" findings on a clean bring-up walk:

1. `resources/public/index.html` (the symlink-to-storage)
2. The 85+ unstyled class names (resolved by recognising styles are inlined in #1)
3. `/api/alpha/aif-stack/live` (owned by futon3c)
4. `/api/alpha/war-machine/show-in-emacs` (owned by futon3c)

Plus the architecture question of who owns the `/api/alpha/*` surface.

Rob's full inventory (with reproducible verification commands per row): `gh/mfuton/holes/missions/M-futon-full-system-operation-and-onboarding/M-futon2-onboarding-and-familiarization/web-war-machine-uncommitted-or-missing-inventory.md`

Rob's methodology gap on outbound HTTP endpoint enumeration (the cljs `client/api.cljs` call sites being important inventory targets) is sharp — captured for the futon-side too in `futon3c/holes/missions/M-action-cost-modelling.md` §3.4 (the trace affordance design — operator-visible provenance of "what data feeds this number?").

— Joe (paired with claude-1 in the war-machine-pilot peripheral, emacs-repl 2026-05-26)
