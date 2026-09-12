#!/usr/bin/env bb
;; Disposable controls for derive_certificate.bb. Writes only under the output
;; directory argument; genuine RUN4 records and checked contracts are read-only.

(load-file "holes/labs/wm-contract/derive_certificate.bb")

(def outdir (or (first *command-line-args*)
                "holes/labs/wm-contract/runs/certificate-v1-emitter-controls-2026-09-12"))
(def scratch (str outdir "/scratch"))
(fs/create-dirs scratch)

(def commission-dir
  "/home/joe/code/futon3c/holes/labs/wm-contract/runs/RUN4-F11-production-successor-2026-09-12/production-fold-commissioning-v5/run4-f11-production-successor-20260912-v1/attempt-001")
(def original-dir
  "/home/joe/run4/F11-production-successor-20260912/cohort/run4-f11-production-successor-20260912-v1/attempt-001")
(def route-source
  "/home/joe/run4/F11-production-successor-20260912/run-records/tick-run-record-7b1a1c3e-171e-4bdf-94dd-a27cd5297bd0.edn")
(def control-map "/home/joe/code/p4ng/empirics-futon/control-map-edges.edn")
(def registry "/home/joe/code/futon2/holes/labs/wm-contract/aif-equations.edn")
(def holes-contract "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json")
(def machine-contracts
  "/home/joe/code/mathlib4/DarkTower/WarMachine/machine-contracts/machine-contracts.json")
(def contract-manifest
  "/home/joe/code/mathlib4/DarkTower/WarMachine/machine-contracts/manifest.json")

(def names ["time-step" "selection" "construction" "dispatch" "build"
            "adjudication" "closed"])
(defn paths-at [dir]
  (mapv (fn [n name] (format "%s/%03d-%s.edn" dir n name))
        (range 1 8) names))
(def commission-paths (mapv #(format "%s/%03d-%s.edn" commission-dir %1 %2)
                            (range 1 4) (take 3 names)))

(defn write-edn! [path x] (spit path (str (pr-str x) "\n")) path)
(defn copy-edn! [source target] (write-edn! target (edn/read-string (slurp source))))

(doseq [[source target] (map vector commission-paths (take 3 (paths-at scratch)))]
  (copy-edn! source target))

(let [p (nth (paths-at scratch) 2)
      r (edn/read-string (slurp p))]
  (write-edn! p (-> r
                    (assoc-in [:payload :judgment :shape-validation]
                              {:validator "valid-fold-output-v1" :version "553a8bc9"
                               :ok true :findings []})
                    (assoc-in [:payload :judgment :correspondence-validation]
                              {:validator "validate-fold-correspondence" :version "553a8bc9"
                               :ok true :findings []}))))

(doseq [[n type stamp] [[4 :dispatch "2026-09-12T17:10:19Z"]
                        [5 :build "2026-09-12T17:10:20Z"]
                        [6 :adjudication "2026-09-12T17:10:21Z"]
                        [7 :closed "2026-09-12T17:10:22Z"]]]
  (write-edn! (nth (paths-at scratch) (dec n))
              {:event/schema-version 1
               :cohort/id :run4-f11-production-successor-20260912-v1
               :attempt/id "attempt-001" :attempt/ordinal 1
               :event/sequence n :checkpoint/type type :recorded-at stamp
               :payload {:judgment (cond-> {:fixture-only true}
                                     (= n 7) (assoc :outcome :fixture-completed))}}))

(def fixture-route (str scratch "/route.edn"))
(write-edn! fixture-route
            {:run/id "fixture-certificate-v1"
             :route [{:fromNode "R20" :toNode "R12" :via "fixture" :at_ "2026-09-12T17:10:23Z"}
                     {:fromNode "R12" :toNode "R2" :via "fixture" :at_ "2026-09-12T17:10:24Z"}
                     {:fromNode "R2" :toNode "R7" :via "fixture" :at_ "2026-09-12T17:10:25Z"}
                     {:fromNode "R7" :toNode "R3" :via "fixture" :at_ "2026-09-12T17:10:26Z"}
                     {:fromNode "R3" :toNode "R8" :via "fixture" :at_ "2026-09-12T17:10:27Z"}
                     {:fromNode "R8" :toNode "R5" :via "fixture" :at_ "2026-09-12T17:10:28Z"}
                     {:fromNode "R5" :toNode "R6" :via "fixture" :at_ "2026-09-12T17:10:29Z"}
                     {:fromNode "R6" :toNode "R14" :via "fixture" :at_ "2026-09-12T17:10:30Z"}
                     {:fromNode "R14" :toNode "TRACE" :via "fixture" :at_ "2026-09-12T17:10:31Z"}]})

(def fixture-registry (str scratch "/aif-equations.edn"))
(let [r (edn/read-string (slurp registry))]
  (write-edn! fixture-registry
              (update r :equations
                      (fn [rows] (mapv #(if (= :observe (:id %))
                                         (assoc % :status :realised-fixture)
                                         %) rows)))))

(defn hashes [paths]
  (into {} (map (fn [p] [p (sha256-file p)])) paths))

(defn base-config
  ([] (base-config (paths-at scratch) fixture-registry machine-contracts))
  ([checkpoints registry-path contracts-path]
   (let [all (concat checkpoints [fixture-route control-map registry-path holes-contract
                                  contracts-path contract-manifest])]
     {:checkpoint-paths checkpoints :route-record-path fixture-route
      :control-map-path control-map :registry-path registry-path
      :holes-contract-path holes-contract :machine-contracts-path contracts-path
      :contract-manifest-path contract-manifest :expected-source-hashes (hashes all)
      :tick-equations [{:registry-row :observe :source-sha256 (sha256-file fixture-route)
                        :pointer [:route]}]
      :declaration-revisions {:observe "a4c2276d515730a32db27f97f1fd955a9bae1433"}
      :checker {:generator "derive_certificate.bb" :mathlib-git-sha "f4fb8c2713cb4c32916d5b2f6d6db439a6b6ddc2"}
      :scope required-scope})))

(defn control [id config expected]
  (let [result (derive-certificate config)
        pass? (and (:certificate/refused result)
                   (= expected (:refusal/class result)))]
    {:control id :expected expected :observed (:refusal/class result)
     :pass? pass? :refusal result :output-written? false}))

(def genuine-original
  (control :genuine-original-f11
           (assoc (base-config) :checkpoint-paths (paths-at original-dir)
                  :expected-source-hashes {})
           :deliverable-chain-incomplete))
(def genuine-commission
  (control :genuine-commission-incomplete
           (assoc (base-config) :checkpoint-paths commission-paths
                  :expected-source-hashes {})
           :checkpoint-chain-incomplete))

(def mutated-construction (str scratch "/003-construction-digest-mismatch.edn"))
(let [r (edn/read-string (slurp (nth (paths-at scratch) 2)))]
  (write-edn! mutated-construction
              (assoc-in r [:payload :judgment :wiring :terminals] [:mutated])))
(def digest-paths (assoc (paths-at scratch) 2 mutated-construction))

(def divergence-construction (str scratch "/003-construction-divergence.edn"))
(let [r (edn/read-string (slurp (nth (paths-at scratch) 2)))]
  (write-edn! divergence-construction
              (assoc-in r [:payload :judgment :selection-enaction]
                        {:verdict :typed-divergence :class :fixture
                         :selected {:type :a} :enacted {:type :b}})))
(def divergence-paths (assoc (paths-at scratch) 2 divergence-construction))

(def missing-registry (str scratch "/registry-missing.edn"))
(let [r (edn/read-string (slurp fixture-registry))]
  (write-edn! missing-registry
              (update r :equations
                      (fn [rows] (mapv #(if (= :observe (:id %))
                                         (assoc % :lean "doesNotExist") %) rows)))))

(def ambiguous-contracts (str scratch "/contracts-ambiguous.json"))
(let [bundle (read-json-file machine-contracts)
      first-contract (first (:contracts bundle))]
  (spit ambiguous-contracts
        (json/generate-string (update bundle :contracts conj first-contract))))

(def controls
  [genuine-original genuine-commission
   (control :source-byte-hash-mismatch
            (assoc (base-config) :expected-source-hashes
                   {(first (paths-at scratch)) (apply str (repeat 64 "0"))})
            :source-digest-mismatch)
   (control :checkpoint-removed (assoc (base-config) :checkpoint-paths
                                       (pop (paths-at scratch)))
            :checkpoint-chain-incomplete)
   (control :checkpoint-reordered
            (assoc (base-config) :checkpoint-paths
                   (assoc (paths-at scratch) 0 (second (paths-at scratch))
                          1 (first (paths-at scratch))))
            :checkpoint-sequence-invalid)
   (control :deliverable-digest-mismatch (base-config digest-paths fixture-registry machine-contracts)
            :deliverable-digest-mismatch)
   (control :divergence-missing-grounds
            (base-config divergence-paths fixture-registry machine-contracts)
            :divergence-grounds-missing)
   (control :declaration-missing (base-config (paths-at scratch) missing-registry machine-contracts)
            :declaration-missing)
   (control :declaration-ambiguous
            (base-config (paths-at scratch) fixture-registry ambiguous-contracts)
            :declaration-ambiguous)
   (control :declaration-wrong-revision
            (assoc (base-config) :declaration-revisions {:observe "wrong-revision"})
            :declaration-revision-mismatch)
   (control :mandatory-scope-missing
            (assoc (base-config) :scope (disj required-scope :not-r1-r17))
            :mandatory-scope-missing)])

(def positive (derive-certificate (base-config)))
(when (:certificate/refused positive)
  (throw (ex-info "positive fixture refused" {:result positive})))
(def typed-divergence-path (str scratch "/003-construction-typed-divergence.edn"))
(let [r (edn/read-string (slurp (nth (paths-at scratch) 2)))]
  (write-edn! typed-divergence-path
              (assoc-in r [:payload :judgment :selection-enaction]
                        {:verdict :typed-divergence :class :fixture-override
                         :grounds "fixture positive control" :selected {:type :a}
                         :enacted {:type :b}})))
(def typed-positive
  (derive-certificate
   (base-config (assoc (paths-at scratch) 2 typed-divergence-path)
                fixture-registry machine-contracts)))

(when (or (:certificate/refused typed-positive) (not-every? :pass? controls))
  (throw (ex-info "certificate emitter controls failed"
                  {:failed (remove :pass? controls) :typed-positive typed-positive})))

(emit! positive (str outdir "/fixture-certificate.edn")
       (str outdir "/fixture-certificate.json"))
(emit! (derive-certificate (base-config)) (str outdir "/fixture-certificate-second.edn")
       (str outdir "/fixture-certificate-second.json"))
(when-not (= (slurp (str outdir "/fixture-certificate.edn"))
             (slurp (str outdir "/fixture-certificate-second.edn")))
  (throw (ex-info "non-deterministic EDN emission" {})))

(write-edn! (str outdir "/controls.edn")
            {:schema :wm/runtime-certificate-v1-emitter-controls
             :fixture-only true :controls controls
             :typed-divergence-positive {:pass? true
                                         :verdict (get-in typed-positive
                                                          [:deliverable-chain
                                                           :selection-enaction :verdict])}
             :deterministic? true
             :fixture-certificate-sha256 (sha256-file (str outdir "/fixture-certificate.edn"))})
(println (pr-str {:ok true :controls (count controls) :deterministic true
                  :outdir outdir}))
