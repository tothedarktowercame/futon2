# Row 18 coherent snapshot writer census

Tree: `05ff643f256c3c3ab979c29faf003d481a9a8879`.

The canonical trip writer is `futon2.aif.tripwire/write-trip-report!`; its
`Files/write` call is inside `with-store-lock-for`. The canonical repair
writers are the private `write-new!`, `write-new-or-identical!`, and
`write-new-durable!` paths in `futon2.aif.repair-obligation`; all public
finding, implementation, verification, and resolution publication reaches
one of those functions, and each now takes the same canonical lock.

An `rg` census for `Files/write`, `spit`, and those writer names across
`src`, `scripts`, `test`, and `holes` found no additional production writer
to the canonical trip or repair roots. `tripwire_calibration/persist-coverage!`
writes its separate calibration report and runs trip/repair publication only
against temporary roots. Test and retained-run `spit` calls target fixtures,
not canonical stores.

The boundary is a Java `FileChannel` exclusive lock at
`/tmp/futon2-wm-interoceptive-snapshot.lock`, shared across processes and
reentrant only within the dynamically nested call in one JVM. The reader
holds it while enumerating and hashing all four stores; participating writers
hold it through immutable publication. This makes the captured bytes and
membership physically consistent at lock release. It does not make a trip
and its later repair finding one transaction: incomplete logical joins remain
constructor refusals. Unknown writers which bypass these owning functions are
outside the authority and cannot be called coordinated.

Source pins (sha256): lock `8636ec65bf8c734a3a7d4c3db5dd5137a5e7d80aecfad03d9c0f528d37b98844`;
manifest `284c96c48a255b4621150962988e10859692bf1e694d698d74fd56ba0f3649bc`;
tripwire `a75b2a571d76fa93486ff8807dc4fdc93a2f6ca125042f0d3087cbdcb933aeae`;
repair obligation `f61ede50822955695d5510248f0592883ed6f83d74be08a23ba37f649e33254a`.
