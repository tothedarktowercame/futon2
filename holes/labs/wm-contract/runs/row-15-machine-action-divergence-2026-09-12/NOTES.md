# Row 15 machineAction live-selector divergence

The registry tuple is `[:R16 :u "machineAction"]`, not R6. The bounded fixture
pins schema-27 trace form 3 and its explicit selection-law record: controller
head requested/applied, controller head rank 1, chosen rank 139, moved true,
and consulted ranking `:live-selector-id`.

The theorem does not dispute the internal controller-head implementation. It
proves that implementation returns the first non-no-op candidate (the pinned
rank-1 head) and that retained live rank 139 is different. A positive carrier
for the reason-bearing live-selector boundary remains open for Row 15 packet 4.
