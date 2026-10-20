"""List every line citation in a Lean file with what it names at given code shas."""
import json, re, sys
rows = json.load(open(sys.argv[1])); lean = open(sys.argv[2]).read().splitlines()
defns = [r for r in rows if r["kind"] == "defn"]
src = {}
for d in defns:
  for i, l in enumerate(d["source"].split("\n")): src[(d["tag"], d["line"] + i)] = l
PAT = re.compile(r"\b([PCRASTLIU])(?::| lines? )(\d+)(?:-(\d+))?")
for ln, text in enumerate(lean, 1):
  for m in PAT.finditer(text):
    t, a = m.group(1), int(m.group(2)); b = int(m.group(3) or a)
    fn = next((d["fn"] for d in defns if d["tag"] == t and d["line"] <= a <= d["end-line"]), None)
    sites = [f'{s["kind"]}:{s.get("reason") or s.get("status")}' for s in rows
             if s["tag"] == t and s["kind"] not in ("defn", "call") and a <= s["line"] <= b]
    code = " | ".join(src.get((t, n), "?").strip() for n in range(a, min(b, a + 2) + 1))
    print(f"L{ln}\t{m.group(0)}\t{fn}\t{','.join(sites)}\t{code[:110]}")
