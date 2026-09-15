(ns controls
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [witnesses.node-witness :as nw]))

(def packet "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1")
(def prior-packet "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-float-carrier-1")
(def fragment "checks/witness-fragments/PredictiveOutcomeKernel.edn")
(def roots {"futon2" "/home/joe/code/futon2" "mathlib4" "/home/joe/code/mathlib4"
            "p4ng" "/home/joe/code/p4ng"})
(defn read-edn [p] (nw/read-edn (slurp p)))
(defn write-edn [p x] (spit p (with-out-str (pp/pprint x))))
(defn locators [x]
  (filter #(and (map? %) (:repo %) (:path %) (:sha256 %)) (tree-seq coll? seq x)))
(defn pinned [repo path]
  (select-keys (nw/read-path! (nw/context roots) repo path "preparation") [:repo :path :sha256]))
(defn old-entry [] (first (:node-witnesses (read-edn (str packet "/old-fragment.edn")))))
(defn new-entry [] (first (:node-witnesses (read-edn fragment))))

(defn inventory []
  (let [files (sort-by str (filter #(.endsWith (.getName %) ".edn")
                                 (.listFiles (io/file "checks/witness-fragments"))))
        records (vec (for [f files l (locators (read-edn f))]
                       (let [actual (nw/read-path! (nw/context roots) (:repo l) (:path l) "inventory")]
                         {:fragment (str f) :locator l :actual (:sha256 actual)
                          :matches? (= (:sha256 l) (:sha256 actual))})))]
    (prn {:fragments (count files) :locators (count records)
          :stale (filterv #(not (:matches? %)) records) :records records})))

(defn prepare []
  (let [old (old-entry)
        artifact (merge (:artifact old)
                        (pinned "mathlib4" (get-in old [:artifact :path])))
        subject (merge (:subject-artifact old)
                       (pinned "mathlib4" (get-in old [:subject-artifact :path])))
        evidence {:formal-author (pinned "futon2" (str prior-packet "/RECEIPT.md"))
                  :formal-review (pinned "futon2" (str prior-packet "/REVIEW-claude-3.md"))
                  :formal-bindings (pinned "futon2" (str prior-packet "/successor-binding.json"))
                  :model-support-laws (pinned "mathlib4" "DarkTower/WarMachine/MachineModelSpec.lean")
                  :retained-row (pinned "mathlib4" "DarkTower/WarMachine/FloatCarriedRowCorrespondence.lean")}
        new (-> old
                (dissoc :verification :review :retained-evidence)
                (assoc :id "R4-forward-model-float-carried-formal-support-v2"
                       :status :proposed :as-of "2026-09-15"
                       :scope :constructed-formal-witness-support-laws
                       :artifact artifact :subject-artifact subject
                       :claim "Constructed formal witness and support laws only: the advanceTwiceRow and cascadeRow FloatCarriedRow constructors discharge duplicate-free support, zero mass off support and the unchanged near-normalization bound; productionPinnedFloatCarried proves the stated constructed positional composition and bound. No fresh observations, production composition correspondence or exact normalization of approximate rows is claimed."
                       :evidence-limit "Formal source evidence only; pending subject-bound verification and independent review, not admitted. The retained approximate row has total 1 + 1/2^55. No historical production acceptance transfers."
                       :retained-evidence evidence))]
    (write-edn (str packet "/successor-subject.edn") (nw/subject new))
    (write-edn (str packet "/verification-preparation.edn")
               {:subject (nw/subject new) :status :prepared-not-admitted
                :author "codex-4" :reviewer "claude-3" :review-status :pending
                :basis-commits {:mathlib4 "f40c936a64227ba81592a71f3d937e6fbdbf0e4c"
                                :futon2-author "6f494738" :futon2-review "e99c08c4"}
                :dependencies (vec (concat [artifact subject] (vals evidence)))
                :accepted-control-records
                (mapv #(pinned "futon2" (str prior-packet "/logs/" %))
                      ["canonical-build.json" "canonical-build.stdout" "Axioms.json" "Axioms.stdout"
                       "Positive.json" "Positive.stdout" "Duplicate.json" "Duplicate.stdout"
                       "Hidden.json" "Hidden.stdout" "OutsideBound.json" "OutsideBound.stdout"
                       "RetainedConversion.json" "RetainedConversion.stdout"])
                :limit "Preparation from accepted prior formal controls, not a newly executed Lean verification receipt or an approved review of this successor subject."})
    (write-edn fragment (assoc (read-edn (str packet "/old-fragment.edn")) :node-witnesses [new]))
    (prn {:old (:id old) :new (:id new) :status :proposed})))

(defn main [mode]
  (case mode
    "inventory" (inventory)
    "prepare" (prepare)
    "old-pins" (doseq [k [:artifact :subject-artifact]]
                 (try (nw/resolve! (nw/context roots) (get (old-entry) k) (:id (old-entry)))
                      (throw (ex-info "Old pin unexpectedly resolved" {:field k}))
                      (catch clojure.lang.ExceptionInfo e
                        (when-not (= :node-witness-pin-mismatch (:error (ex-data e))) (throw e))
                        (prn (assoc (ex-data e) :field k)))))
    "new-pins" (let [w (new-entry)
                     ls (vec (concat (locators w)
                                     (locators (read-edn (str packet "/verification-preparation.edn")))))]
                 (doseq [l ls] (nw/resolve! (nw/context roots) l (:id w)))
                 (prn {:pass? true :resolved (count ls) :scope :source-pins-only}))
    "tamper-pin" (let [w (new-entry)]
                   (nw/resolve! (nw/context roots)
                                (assoc (:artifact w) :sha256 (apply str (repeat 64 "0"))) (:id w)))
    "borrow-review" (nw/receipt! (nw/context roots)
                                 (assoc (new-entry) :review (:review (old-entry))) :review)
    "reuse-id" (let [[directory] (next *command-line-args*)]
                 (write-edn (str directory "/PredictiveOutcomeKernel.edn")
                            (assoc (read-edn fragment) :node-witnesses
                                   [(assoc (new-entry) :id (:id (old-entry)))])))
    (throw (ex-info "Unknown mode" {:mode mode}))))

(try
  (main (first *command-line-args*))
  (catch Exception e
    (binding [*out* *err*] (prn (merge {:message (.getMessage e)} (ex-data e))))
    (System/exit 1)))
