#!/usr/bin/env bb
;; F8 leg 3 slice 1 -- INDEPENDENT PROBE, run by the reviewing seat BEFORE the
;; dispatch went out, so the packet's premises are checked rather than asserted.
;;
;; CONVERGENCE-draft.edn was written before leg 1 ran. Leg 1 declared a Lean
;; carrier on nine class-(a) rows, and its :code sweep rewrote :code fields.
;; The draft's own caveat says every rung is "draft-mechanical pending F8
;; adjudication". This probe prints, mechanically, the facts that adjudication
;; has to answer to. It makes NO ruling and writes NO registry.
;;
;;   1. draft spec-leg rung vs the registry's carrier at HEAD, per row
;;   2. every basis pointer in the draft, resolved or not, ambiguity refused
;;   3. the F6 machine-record test applied to every impl-leg licence
;;   4. what F3-form certificate artifacts exist on disk today
;;   5. the F6 rung scale, read from its generator rather than remembered
(require '[clojure.edn :as edn] '[clojure.string :as str] '[clojure.java.io :as io]
         '[babashka.process :refer [shell]])

(def home (System/getenv "HOME"))
(def wm (str home "/code/futon2/holes/labs/wm-contract/"))
(def draft (edn/read-string (slurp (str wm "CONVERGENCE-draft.edn"))))
(def reg (edn/read-string (slurp (str wm "aif-equations.edn"))))
(def u35 (edn/read-string (slurp (str wm "runs/U35-lean-state/lean-state-report.edn"))))

(def by-id (into {} (map (juxt :id identity) (:equations reg))))
(def join-by-id (into {} (map (juxt :equation identity) (get-in u35 [:equation-lean-join :rows]))))

;; --- pointer resolution -----------------------------------------------------
;; Suffix resolution against three roots, and an ambiguous match is a REFUSAL
;; rather than a first-match guess (the slice-2 review's finding: futon2 carries
;; two efe.clj, and a first-match resolver answers about the wrong file).
(def roots [(str home "/code/") (str home "/code/futon2/") wm
            ;; The draft cites Lean sites by bare filename inside prose
            ;; ("variationalFreeEnergy def, Holes.lean:6828"). pointer_check.bb
            ;; carries the same root (p4ng/empirics-futon/pointer_check.bb:32),
            ;; so leaving it out here would report a probe limitation as a draft
            ;; defect. Ambiguity across roots is still refused below.
            (str home "/code/mathlib4/DarkTower/WarMachine/")])
(def ptr-re #"([A-Za-z0-9_./-]+\.(?:clj|cljc|bb|edn|lean|tex|md|txt|py|sh)):(\d+)(?:-(\d+))?")

(defn resolve-path [rel]
  (let [hits (filter #(.exists (io/file (str % rel))) roots)]
    (cond (empty? hits) {:status :not-found}
          (> (count (distinct (map #(.getCanonicalPath (io/file (str % rel))) hits))) 1)
          {:status :ambiguous :paths (mapv #(str % rel) hits)}
          :else {:status :ok :path (str (first hits) rel)})))

(defn check-pointer [rel a b]
  (let [r (resolve-path rel)]
    (if (not= :ok (:status r))
      (assoc r :pointer (str rel ":" a (when b (str "-" b))))
      (let [n (count (str/split-lines (slurp (:path r))))
            a (parse-long a) b (some-> b parse-long)]
        (cond (> a n) {:status :out-of-bounds :pointer (str rel ":" a) :lines n}
              (and b (or (> b n) (< b a))) {:status :out-of-bounds :pointer (str rel ":" a "-" b) :lines n}
              :else {:status :ok :path (:path r) :pointer (str rel ":" a (when b (str "-" b))) :lines n})))))

(defn pointers-in [s] (when (string? s) (re-seq ptr-re s)))

;; A licence reaches :witnessed or above only if the artifact it names carries a
;; run identity. Test and key set copied from the F6 generator
;; (futon2/scripts/generate_variable_situation_accounting.bb:428, :451-463).
(def run-identity-keys #{:run-id :runId :startedAt :tick-id :wm-run-id})
(defn machine-record? [path]
  (and path (str/ends-with? path ".edn")
       (try (let [v (edn/read-string (slurp path))]
              (boolean (and (map? v) (some run-identity-keys (keys v)))))
            (catch Exception _ false))))

(println "=== F8 leg 3 slice 1 independent probe ===")
(printf "draft   %s (%d rows, as-of %s)%n" "CONVERGENCE-draft.edn" (count (:rows draft)) (:as-of draft))
(printf "registry aif-equations.edn as-of %s, %d equations%n" (:as-of reg) (count (:equations reg)))
(let [h (str/trim (:out (shell {:out :string :dir (str home "/code/mathlib4")} "git rev-parse HEAD")))]
  (printf "mathlib4 HEAD %s%n" h)
  (printf "U35 report finished-at %s%n" (get-in u35 [:typecheck :finished-at])))

(println)
(println "=== 1. DRAFT SPEC-LEG vs THE REGISTRY CARRIER AT HEAD ===")
(println "  A draft row saying 'not in Lean' whose registry row now declares a carrier is STALE.")
(printf "%-24s %-20s %-34s %-14s %s%n" "row" "draft spec rung" "registry :lean at HEAD" ":lean-status" "verdict")
(def spec-stale (atom []))
(doseq [r (:rows draft)]
  (let [e (by-id (:id r))
        drung (get-in r [:spec-leg :rung])
        dbasis (get-in r [:spec-leg :basis])
        says-none? (str/includes? (str dbasis) "not in Lean")
        has? (some? (:lean e))
        verdict (cond (and says-none? has?) :STALE-carrier-now-declared
                      (and (not says-none?) (not has?)) :STALE-carrier-withdrawn
                      (and says-none? (not has?)) :agrees-no-carrier
                      :else :agrees-carrier)]
    (when (str/starts-with? (name verdict) "STALE") (swap! spec-stale conj (:id r)))
    (printf "%-24s %-20s %-34s %-14s %s%n" (:id r) drung
            (let [s (str (:lean e))] (if (> (count s) 33) (str (subs s 0 30) "...") s))
            (str (:lean-status e)) (name verdict))))
(printf "STALE spec legs: %d %s%n" (count @spec-stale) (pr-str @spec-stale))

(println)
(println "=== 1b. REGISTRY :status FIELDS THE DRAFT DOES NOT CARRY ===")
(doseq [e (:equations reg) :when (:status e)]
  (printf "  %-24s :status %s%n" (:id e) (:status e)))

(println)
(println "=== 2. EVERY BASIS POINTER IN THE DRAFT ===")
(def bad (atom []))
(defn scan! [label s]
  (doseq [[_ rel a b] (pointers-in s)]
    (let [res (check-pointer rel a b)]
      (when (not= :ok (:status res))
        (swap! bad conj [label (:pointer res) (:status res)]))
      (printf "  %-46s %-52s %s%n" label (str rel ":" a (when b (str "-" b))) (name (:status res))))))
(doseq [[i c] (map-indexed vector (:caveats draft))] (scan! (str "caveat[" i "]") c))
(scan! ":registry-basis" (:registry-basis draft))
(scan! ":name-hazard-basis" (:name-hazard-basis draft))
(doseq [r (:rows draft)]
  (scan! (str (:id r) " spec-leg") (get-in r [:spec-leg :basis]))
  (scan! (str (:id r) " impl-leg") (get-in r [:impl-leg :basis]))
  (when (map? (:certificate r))
    (scan! (str (:id r) " cert :artifact") (get-in r [:certificate :artifact]))
    (scan! (str (:id r) " cert :basis") (get-in r [:certificate :basis]))))
(printf "unresolved or out-of-bounds: %d%n" (count @bad))
(doseq [b @bad] (printf "  BAD %s%n" (pr-str b)))

(println)
(println "=== 3. THE F6 MACHINE-RECORD TEST ON EVERY IMPL-LEG LICENCE ===")
(println "  F6: :witnessed and above assert a RECORD exhibits the quantity, and the")
(println "  test is whether the licensed artifact carries a run identity")
(println "  (generate_variable_situation_accounting.bb:417-428).")
(printf "%-24s %-14s %-9s %s%n" "row" "draft impl rung" "record?" "licences")
(def unlicensed (atom []))
(doseq [r (:rows draft)]
  (let [rung (get-in r [:impl-leg :rung])
        ps (pointers-in (get-in r [:impl-leg :basis]))
        paths (keep (fn [[_ rel a b]] (:path (check-pointer rel a b))) ps)
        rec? (boolean (some machine-record? paths))]
    (when (and (contains? #{:witnessed :constructed :wired :validated :run-correlated} rung) (not rec?))
      (swap! unlicensed conj (:id r)))
    (printf "%-24s %-14s %-9s %s%n" (:id r) rung (if rec? "yes" "NO")
            (str/join ", " (map (fn [[_ rel a b]] (str rel ":" a (when b (str "-" b)))) ps)))))
(printf "impl legs at :witnessed+ with NO machine record: %d %s%n"
        (count @unlicensed) (pr-str @unlicensed))

(println)
(println "=== 4. F3-FORM CERTIFICATE ARTIFACTS ON DISK ===")
(doseq [d (sort (map #(.getName %) (filter #(.isDirectory %) (.listFiles (io/file (str wm "runs"))))))
        :when (re-find #"^F\d" d)]
  (printf "  runs/%s: %s%n" d (pr-str (sort (map #(.getName %) (.listFiles (io/file (str wm "runs/" d))))))))

(println)
(println "=== 5. THE F6 RUNG SCALE, READ FROM ITS GENERATOR ===")
(let [src (slurp (str home "/code/futon2/scripts/generate_variable_situation_accounting.bb"))
      m (re-find #"\(def rung-scale\s*\n?\s*(\[[^\]]*\])" src)]
  (println "  futon2/scripts/generate_variable_situation_accounting.bb")
  (println "  " (str/replace (or (second m) "NOT FOUND") #"\s+" " ")))
(let [acc (edn/read-string (slurp (str wm "variable-situation-accounting.edn")))]
  (printf "  accounting registry :axes :rung -> %s%n" (pr-str (get-in acc [:axes :rung]))))

(println)
(printf "PROBE SUMMARY stale-spec-legs=%d unresolved-pointers=%d unlicensed-witnessed-impl-legs=%d%n"
        (count @spec-stale) (count @bad) (count @unlicensed))
