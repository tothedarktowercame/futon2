;; Real-producer integration probe — zai-8, 2026-10-22.
;; Joins the pure binding (worktree futon2-declared-effect-binding, HEAD)
;; with the CORRECTED C2 producer (zai-2 job2384, commit 5460a97d branch
;; declared-effect-review, worktree futon2-declared-effect-review at
;; a2e54d1ae). Read-only: builds records via real git resolution + real C4
;; reads at PINNED revisions; writes nothing except printed result.
;; Run from /home/joe/code/futon2-declared-effect-binding:
;;   clojure -Sdeps '{:aliases {:c2 {:extra-paths ["/home/joe/code/futon2-declared-effect-review/src"]}}}' -M:c2 <this file>
(require '[futon2.aif.occurrence-declared-effect-binding :as binding]
         '[futon2.aif.declared-effect-record :as c2]
         '[futon2.aif.close-retention :as retention])

(def target "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")
(def before {:repo "futon2" :sha "ae69f5e7b1a1d9012742fddc6d14313ac9292e3d"}) ; ticket OPEN
(def after  {:repo "futon2" :sha "97e17e10f2695c481c55ddaebe9026f2d245663f"}) ; ticket DONE
(def build-inputs (fn [base] {:target target :base-revision base}))
(def adapter
  "Production adapter shape: the CORRECTED producer's reconstruct with
   PRODUCTION ports (injected ports are refused by reconstruct itself).
   reconstruct returns {:verified bool :expected sha :rebuilt sha}; its
   :verified already requires expected = rebuilt = the record's own digest,
   so a verified result is translated to the binding's accepted shape using
   the record's digest. Refusals pass through verbatim. This — not any
   status map — is what the binding accepts as authority."
  (fn [record]
    (let [r (c2/reconstruct (build-inputs (select-keys (:base-revision record)
                                                       [:repo :sha]))
                            record)]
      (if (true? (:verified r))
        {:verified true :digest (:record/digest record)}
        r))))

(def record (c2/build-record (build-inputs before)))
(def occurrence
  (retention/mint-occurrence
   {:run-id "run-int" :cohort-id "cohort-int" :attempt-id "attempt-int"
    :selected-action {:kind :cascade-candidate :id "T-repair/C2" :target target
                      :precedence []}
    :now (constantly (java.time.Instant/parse "2026-10-22T09:00:00Z"))
    :uuid-fn (fn [] (java.util.UUID/fromString
                     "00000000-0000-0000-0000-0000000000a1"))}))

;; POSITIVE — real record, real reconstruct authority, real occurrence.
(def b (binding/build-binding
         {:occurrence occurrence :selected-mission target :record record
          :record-verifier adapter
          :recorded-at "2026-10-22T09:00:00Z" :bound-at "2026-10-22T09:00:01Z"}))
(def v (binding/verify-binding
         {:occurrence occurrence :binding b :record record
          :record-verifier adapter :selected-mission target}))

;; NEGATIVE 1 — forged record: body mutated (horizon/wants), old digest kept,
;; and an echoing verifier returns the old verdict. Independent digest
;; re-derivation must refuse before any authority is consulted.
(def mutated (assoc record
                    :horizon {:status :absent :reason :horizon-not-declared}
                    :wants [] :active-wants []))
(def echo-calls (atom 0))
(def echo-verifier
  (fn [_] (swap! echo-calls inc) {:verified true :digest (:record/digest record)}))
(def forged-result
  (binding/verify-binding
   {:occurrence occurrence :binding b :record mutated
    :record-verifier echo-verifier :selected-mission target}))

;; NEGATIVE 2 — self-hashed binding: record digest swapped for the AFTER-base
;; record's digest (a real, different, well-formed record) and the binding
;; digest recomputed. Cross-equality must refuse.
(def after-record (c2/build-record (build-inputs after)))
(def swap-forged (assoc b :declared-effect/digest (:record/digest after-record)))
(def swap-self-hashed (assoc swap-forged :binding/digest
                             (binding/binding-digest swap-forged)))
(def swap-result
  (binding/verify-binding
   {:occurrence occurrence :binding swap-self-hashed :record record
    :record-verifier adapter :selected-mission target}))

;; NEGATIVE 3 — same occurrence/binding/record, different selection-cell
;; target.
(def mission-result
  (binding/verify-binding
   {:occurrence occurrence :binding b :record record
    :record-verifier adapter :selected-mission (str target "X")}))

;; NEGATIVE 4 — the AFTER base itself: no open declared want, admission not
;; eligible, so it must never bind even with a real :verified reconstruction.
(def after-reconstruct (adapter after-record))
(def after-build-result
  (try (binding/build-binding
        {:occurrence occurrence :selected-mission target :record after-record
         :record-verifier adapter
         :recorded-at "2026-10-22T09:00:00Z" :bound-at "2026-10-22T09:00:01Z"})
       (catch Exception e (:declared-effect-binding/refusal (ex-data e)))))

(println
 (pr-str
  {:schema :wm/binding-real-c2-integration-probe-v1
   :binding-worktree "futon2-declared-effect-binding 256b1b26f"
   :c2-commit "5460a97ddd1ee36be019f5611f0bda16f7702b83 (branch declared-effect-review; worktree at a2e54d1ae)"
   :record {:eligible? (get-in record [:admission :eligible])
            :refusals (get-in record [:admission :refusals])
            :declaration (:declaration record)
            :base-revision (:base-revision record)
            :digest (:record/digest record)}
   :positive {:built (contains? b :binding/digest)
              :binding-digest (:binding/digest b)
              :verify-status (:status v)}
   :forged-body-old-digest {:reason (:reason forged-result)
                            :typed-refusal (get-in forged-result [:details :refusal])
                            :verifier-consulted (pos? @echo-calls)}
   :record-digest-swap-self-hashed {:reason (:reason swap-result)}
   :selection-cell-target-mismatch {:reason (:reason mission-result)}
   :after-base-not-eligible {:reconstruct-verified (:verified after-reconstruct)
                             :admission-eligible (get-in after-record [:admission :eligible])
                             :build-refusal after-build-result}}))
(shutdown-agents)
