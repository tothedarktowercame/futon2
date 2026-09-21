# H3 observation-clock premise mismatch

Status: blocked on an observation-placement schema ruling; no H3 implementation made.
Date: 2026-09-21. Follows accepted discovery `3e9a3141` and the five-increment authorization.

## What failed the premise check

Ruling 2 specifies the cascade declaration's own observation placement, described as a tau/schedule the loader already validates. At the inspected source, that interface does not exist. `cascade_sources.clj:147` invokes `live-c/preference-schedule`; `live_c.clj:321–332` validates **preference** placement under `:c-schedule`. It accepts terminal preference placement and a uniform earlier preference distribution. It does not specify when completed-task evidence was observed.

All four declaration files inspected contain that preference schedule. None declares an observation placement. The loader's output contains `:preference-schedules`, not an observation schedule. This is consistent with the authorized missing-placement HOLD, but it means the proposed existing production timing input has no producer or loader contract. Reinterpreting `:c-schedule` as observation timing would make preferences determine measurement timing without a declaration to that effect.

## Executed control

A fresh tooling JVM called the **real** `cascade-sources/load-declared` twice over a temporary declaration based on M-wm-08-external-f2. Facts, wants, locators, interpretations and candidates were emptied identically in both arms to avoid external observation/source reads. The second arm added only `:observation-schedule {:tau :not-a-step}`.

Both loads succeeded. Apart from file/read provenance, their output was identical. The invented malformed observation field was ignored, not validated or returned. `probe.edn` retains this result; `source-pins.json` pins the loader, preference validator and all four inspected declarations. This control demonstrates that adding an apparent timing field to data alone cannot feed H3.

Executed command: `clojure -M:test /tmp/h3-clock-probe.clj`, exit 0. This was a read-only loader probe using temporary files, not a test-suite run or registered warrant. No shared JVM reload. Reproduction source:

```clojure
(require '[clojure.edn :as edn] '[clojure.java.io :as io]
         '[futon2.aif.cascade-sources :as sources])
(let [dir (.toFile (java.nio.file.Files/createTempDirectory "h3-clock-probe" (make-array java.nio.file.attribute.FileAttribute 0)))
      file (io/file dir "source.edn")
      base (-> (edn/read-string (slurp "resources/wm/cascade-sources/M-wm-08-external-f2.edn"))
               (assoc :facts #{} :want #{} :locators {} :patterns {} :interpretation-receipts {} :candidates []))
      load-one (fn [d] (spit file (pr-str d)) (sources/load-declared (.getPath dir)))
      a (load-one base)
      b (load-one (assoc base :observation-schedule {:tau :not-a-step}))
      shape (fn [x] (dissoc x :files :read-occurrences))]
  (try
    (prn {:probe :real-declaration-loader-observation-clock
          :base-accepted true
          :preference-schedule (:preference-schedules a)
          :base-output-keys (vec (sort (keys a)))
          :malformed-observation-schedule-accepted true
          :semantic-output-unchanged (= (shape a) (shape b))
          :observation-schedule-in-output (contains? b :observation-schedule)
          :control "Only the unrecognized observation-schedule key changed; provenance hashes intentionally differ."})
    (finally (.delete file) (.delete dir))))
```

## Proposed precise amendment

Authorize a distinct optional declaration field, for example:

```clojure
:observation-schedule {:tau {:value 1 :status :declared}}
```

The value above illustrates syntax only: no current mission is assigned step 1 by this report. Require a nonnegative integer and check it against the frozen comparison's model horizon. The declaring agent must supply the actual step; no terminal default, no copying of the preference schedule. The loader must validate and preserve the field per target; dispatch captures that declared value and the declaration pin, and completion carries it unchanged into the retrospective event. Missing placement retains the authorized typed HOLD; malformed placement refuses. The joint-family calibration uses the selected task's declared event time over the same frozen comparison, with model compatibility checked explicitly.

Controls to add after amendment: missing declaration holds; malformed/noninteger/negative/out-of-horizon timing refuses; a declared nonterminal step is preserved through loader/dispatch/read-back; changing preference placement does not change observation timing; tampered declaration/occurrence binding refuses.

This changes the loader schema and needs an honest declaring producer, rather than pretending an existing schedule already means observations. Please confirm this amendment, or identify the intended existing observation-placement contract. The other four rulings remain accepted; the numerical wrapper defect remains unfixed pending this stop.

## Shared-checkout boundary

The checkout already has concurrent edits in the runner, selector, observation evaluator and task-authority files. None was edited or staged by this task. This commit contains only the stop note and probe evidence. Whole-tree cleanliness is not claimed.

No learning closure is claimed. As required for the eventual receipt: a converged solver that the wrapper discards does not demonstrate learning.
