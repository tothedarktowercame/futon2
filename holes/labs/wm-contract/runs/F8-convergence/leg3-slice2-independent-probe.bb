#!/usr/bin/env bb
;; F8 leg 3 slice 2 -- the REVIEWING seat's independent probe, written and
;; committed BEFORE the implementation packet goes out, so the premises the
;; packet states are measured here rather than asserted there.
;;
;; It measures seven things the slice-2 checker specification in
;; C531-F8-convergence-ledger.md depends on. Two of them the specification gets
;; wrong, and that is the point of running this first.
(require '[clojure.edn :as edn] '[clojure.string :as str] '[clojure.java.io :as io])

(def home (System/getenv "HOME"))
(def code (str home "/code/"))
(def wm (str code "futon2/holes/labs/wm-contract/"))
(def led (edn/read-string (slurp (str wm "CONVERGENCE.edn"))))
(def reg (edn/read-string (slurp (str wm "aif-equations.edn"))))
(def by-id (into {} (map (juxt :id identity) (:equations reg))))
(def roots [code (str code "futon2/") wm (str code "mathlib4/DarkTower/WarMachine/")])
(def ptr-re #"^([A-Za-z0-9_./-]+):(\d+)(?:-(\d+))?$")
(def run-identity-keys #{:run-id :runId :startedAt :tick-id :wm-run-id})

(defn resolve-ptr [p]
  (when-let [[_ rel a b] (re-matches ptr-re (str p))]
    (let [hits (distinct (map #(.getCanonicalPath (io/file (str % rel)))
                              (filter #(.exists (io/file (str % rel))) roots)))]
      (cond (empty? hits) {:status :not-found}
            (> (count hits) 1) {:status :ambiguous :paths hits}
            :else (let [n (count (str/split-lines (slurp (first hits))))]
                    (if (or (> (parse-long a) n) (and b (> (parse-long b) n)))
                      {:status :out-of-bounds :lines n :path (first hits)}
                      {:status :ok :path (first hits) :lines n}))))))

(defn read-edn [path] (try (edn/read-string {:default (fn [_ x] x)} (slurp path)) (catch Exception _ nil)))
;; the F6 predicate as written (top level or top-level sequence of maps) and the
;; anywhere-in-the-tree question, kept apart: a record the rule cannot see is a
;; different thing from a fixture with no run at all.
(defn f6? [path]
  (boolean (and path (str/ends-with? path ".edn")
                (let [v (read-edn path)
                      maps (cond (map? v) [v] (sequential? v) (filter map? v) :else [])]
                  (some (fn [m] (some run-identity-keys (keys m))) maps)))))
(defn nested? [path]
  (boolean (and path (str/ends-with? path ".edn")
                (some #(and (map? %) (some run-identity-keys (keys %)))
                      (tree-seq coll? seq (read-edn path))))))

(def legs (for [r (:rows led) leg [:spec-leg :impl-leg]] [(:id r) leg (get r leg)]))

(println "=== 1. ROW SET AGAINST THE EQUATION REGISTRY ===")
(let [rows (set (map :id (:rows led))) eqs (set (map :id (:equations reg)))]
  (println "ledger rows" (count rows) " registry equations" (count eqs) " equal?" (= rows eqs))
  (println "  ledger-only" (sort (remove eqs rows)) " registry-only" (sort (remove rows eqs))))

(println)
(println "=== 2. CAVEATS: THE SPEC SAYS THREE, THE FILE CARRIES N ===")
(println "count" (count (:caveats led)))
(doseq [[i c] (map-indexed vector (:caveats led))]
  (println " " i (subs c 0 (min 72 (count c)))))
(println "FINDING if count is not 3: C531's slice-2 line says 'pin the three still-live caveats'.")

(println)
(println "=== 3. LICENCES: RESOLVE, AND THE TWO RUN-IDENTITY TESTS ===")
(let [rs (for [[id leg m] legs
               :let [p (:licence m) r (resolve-ptr p)
                     path (:path r)]]
           {:id id :leg leg :rung (:rung m) :licence p :status (:status r)
            :f6 (f6? path) :nested (nested? path)})]
  (println "licences" (count rs)
           " unresolved" (count (remove #(= :ok (:status %)) rs))
           " f6-true" (count (filter :f6 rs))
           " nested-only" (count (filter #(and (not (:f6 %)) (:nested %)) rs)))
  (doseq [r rs :when (or (not= :ok (:status r)) (:f6 r) (:nested r))]
    (println " " (:id r) (:leg r) (:rung r) (:status r) :f6 (:f6 r) :nested (:nested r) (:licence r))))

(println)
(println "=== 4. THE RULE POINTERS THE LEDGER CITES, READ AT HEAD ===")
(doseq [[label p] [[":rung-rule" "futon2/scripts/generate_variable_situation_accounting.bb:417-428"]
                   [":rung-rule-limit" "futon2/scripts/generate_variable_situation_accounting.bb:450-462"]]]
  (let [r (resolve-ptr p)]
    (println " " label (:status r) p)))
(println "  417-428 mentions run-identity-keys?"
         (str/includes? (slurp (str code "futon2/scripts/generate_variable_situation_accounting.bb")) "run-identity-keys"))
(println "  450-462 defines machine-record??"
         (boolean (re-find #"defn machine-record\?" (slurp (str code "futon2/scripts/generate_variable_situation_accounting.bb")))))

(println)
(println "=== 5. IS THERE A CODE-BACKED AUTHORITY FOR 'ACCEPTED RUN'? ===")
(let [pin-path (str code "futon2/data/wm-step/w1/pin/pin.edn")
      pin (read-edn pin-path)
      accepted (:pin/accepted-steps pin)]
  (println "  pin" (if pin "exists" "NOT FOUND") pin-path)
  (println "  wm_step_observe.bb:111 previous-accepted-run reads :pin/accepted-steps:"
           (str/includes? (slurp (str wm "wm_step_observe.bb")) ":pin/accepted-steps"))
  (println "  accepted run ids" (mapv :run-id accepted))
  (println "  ledger certificates declaring :accepted-run? true:"
           (count (filter #(get-in % [:certificate :accepted-run?]) (:rows led))))
  (doseq [r (:rows led) :let [c (:certificate r)] :when (:artifact c)]
    ;; the artifact path is repo-relative, so it is resolved through the same
    ;; roots as a licence rather than glued onto the code root -- a first
    ;; version of this probe did the latter and reported "exists false" about a
    ;; file that is committed, which is the measurement error the probe exists
    ;; to avoid making in the packet.
    (let [rp (resolve-ptr (str (:artifact c) ":1"))
          art (read-edn (:path rp))]
      (println "  cert artifact" (:id r) (:artifact c)
               "resolves" (:status rp)
               "exists" (boolean art)
               "carries-run-identity-anywhere"
               (boolean (some #(and (map? %) (some run-identity-keys (keys %))) (tree-seq coll? seq art)))
               "in-pin-accepted" (boolean (some #(= (:run-id %) (:artifact c)) accepted)))))
  (println "  SO: :accepted-run? is RECOMPUTABLE against the pin, not only declarable."))

(println)
(println "=== 6. VERBATIM IDENTITY FIELDS AGAINST THE REGISTRY ===")
(let [drift (for [r (:rows led)
                  :let [e (by-id (:id r))]
                  k [:defines :node :formal]
                  :when (and e (not= (get r k) (get e k)))]
              [(:id r) k (get r k) (get e k)])]
  (println "fields compared" (* 3 (count (:rows led))) " drifted" (count drift))
  (doseq [d drift] (println " " (pr-str d))))

(println)
(println "=== 7. RUNG VOCABULARY IN USE ===")
(println "  scale" (:rung-order led))
(println "  used " (frequencies (map (fn [[_ _ m]] (:rung m)) legs)))
(println "  off-scale" (remove (set (:rung-order led)) (distinct (map (fn [[_ _ m]] (:rung m)) legs))))
(println "  converged? true rows" (count (filter :converged? (:rows led))))
