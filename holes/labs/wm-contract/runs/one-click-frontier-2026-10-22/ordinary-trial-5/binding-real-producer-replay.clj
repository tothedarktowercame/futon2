(require '[clojure.edn :as edn] '[clojure.java.shell :as sh]
 '[futon2.aif.declared-effect-record :as d]
 '[futon2.aif.close-retention :as retention]
 '[futon2.aif.occurrence-declared-effect-binding :as b])
(let [path "/home/joe/code/futon2/resources/wm/cascade-sources/T-repair-occ-444fb018.edn"
      target (:target (edn/read-string (slurp path)))
      sha (clojure.string/trim (:out (sh/sh "git" "-C" "/home/joe/code/futon2" "rev-parse" "ae69f5e7")))
      inputs {:target target :declaration {:path path} :base-revision {:repo "futon2" :sha sha}}
      r (d/build-record inputs)
      verifier (fn [current] (let [v (d/reconstruct inputs current)]
                              (if (:verified v) {:verified true :digest (:record/digest current)} v)))
      occurrence (retention/mint-occurrence
                  {:run-id "binding-offline-replay" :cohort-id "fixture" :attempt-id "fixture"
                   :selected-action {:kind :cascade-candidate :id :replay :target target}
                   :now (constantly (java.time.Instant/parse "2026-10-22T08:00:00Z"))
                   :uuid-fn #(java.util.UUID/fromString "00000000-0000-0000-0000-000000000001")})
      binding (b/build-binding {:occurrence occurrence :selected-mission target
                                :record r :record-verifier verifier
                                :recorded-at "2026-10-22T08:00:00Z" :bound-at "2026-10-22T08:00:01Z"})
      input {:occurrence occurrence :binding binding :record r
             :selected-mission target :record-verifier verifier}
      positive (b/verify-binding input)
      forged (assoc r :horizon {:status :declared :value 999 :authority :declaration/horizon-steps})
      forged (assoc forged :record/digest (d/record-digest forged))
      negative (b/verify-binding (assoc input :record forged))]
  (assert (true? (get-in r [:admission :eligible])))
  (assert (= :verified (:status positive)))
  (assert (= :refused (:status negative)))
  (prn {:positive positive :self-hashed-undeclared-horizon negative
        :base sha :record-digest (:record/digest r)
        :scope :standalone-historical-producer-binding-reconstruction-not-live-route}))
(shutdown-agents)
