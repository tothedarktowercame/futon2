ROLE: library-annotator (stakeholder stand-in), commissioned for zaif-harness row PA11z.

You are serving ONE cascade's refusal frontier. This is a DISCOVERY dispatch:
you PROPOSE annotations with evidence, you do NOT commit anything and you do
NOT edit any file. Report only.

CONTEXT. futon3's library graph refuses a cascade pattern when it is not
@why-reachable from a problem-stating node. Under the L4 baseline reading
(down-problems+wr), zaif-cascade.edn has 24 of its 53 consulted patterns
refused. Re-measured today at futon3 50bd5309 with
futon2/holes/labs/library-loop/runs/l13_graph_gate.clj against
runs/L18-graph.edn: "gate: 24/53 refused".

YOUR TARGET, three of those 24:
  agent/handoff-preserves-context
  agent/trail-enables-return
  cascades/declared-skeleton

FOR EACH, do this and only this:
1. Search the COMMITTED corpus for a source that STATES THE PROBLEM the
   pattern answers. Look under /home/joe/code/futon3 (patterns/, problems/)
   and /home/joe/code/futon2/holes. Read, do not guess.
2. If you find one, quote the sentence VERBATIM and give its file:line.
   Propose the @why edge you would write, in the exact syntax the corpus
   already uses (find a committed example of that syntax and cite it).
3. If you do NOT find one, say so plainly and say what you searched. An
   honest "no committed source states this problem" is the CORRECT and
   valuable answer -- the library loop's own finding is that the remaining
   refusals are an AUTHORING gap, not a linking gap. Do not invent a source
   and do not stretch a loosely-related one.

STATE, for every search you run, the exact command and whether its output was
truncated. A head-limited grep presented as an enumeration is a defect here.

RETURN: a short report -- per pattern, the verdict (evidence-found /
no-committed-source), the verbatim quote + file:line where found, the proposed
edge, and your search commands. Nothing else. Do not write files. Do not
commit. Do not modify the graph.
