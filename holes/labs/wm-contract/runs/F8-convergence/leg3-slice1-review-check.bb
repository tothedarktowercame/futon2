#!/usr/bin/env bb
;; F8 leg 3 slice 1 REVIEW -- the reviewing seat's own check over the delivered
;; CONVERGENCE.edn. Recomputes, rather than reads:
;;   A. :id/:defines/:node/:formal against aif-equations.edn at HEAD (the file
;;      claims they are transcribed verbatim)
;;   B. every :licence pointer -- resolves, in bounds, and NOT ambiguous
;;   C. the F6 machine-record test on every rung at :witnessed or above
;;   D. rungs on the F6 scale, and :leading-leg consistent with the two rungs
;;   E. every "has no run identity" claim in a :basis, checked against the file
;;      it names rather than taken on the delivery's word
(require '[clojure.edn :as edn] '[clojure.string :as str] '[clojure.java.io :as io])

(def home (System/getenv "HOME"))
(def wm (str home "/code/futon2/holes/labs/wm-contract/"))
(def led (edn/read-string (slurp (str wm "CONVERGENCE.edn"))))
(def reg (edn/read-string (slurp (str wm "aif-equations.edn"))))
(def by-id (into {} (map (juxt :id identity) (:equations reg))))
(def fails (atom []))
(defn fail! [& xs] (swap! fails conj (str/join " " (map str xs))))

(def roots [(str home "/code/") (str home "/code/futon2/") wm
            (str home "/code/mathlib4/DarkTower/WarMachine/")])
(def ptr-re #"^([A-Za-z0-9_./-]+):(\d+)(?:-(\d+))?$")

(defn resolve-ptr [p]
  (if-let [[_ rel a b] (re-matches ptr-re p)]
    (let [hits (distinct (map #(.getCanonicalPath (io/file (str % rel)))
                              (filter #(.exists (io/file (str % rel))) roots)))]
      (cond (empty? hits) {:status :not-found}
            (> (count hits) 1) {:status :ambiguous :paths hits}
            :else (let [n (count (str/split-lines (slurp (first hits))))
                        a (parse-long a) b (some-> b parse-long)]
                    (if (or (> a n) (and b (or (> b n) (< b a))))
                      {:status :out-of-bounds :lines n}
                      {:status :ok :path (first hits) :lines n}))))
    {:status :malformed}))

(def run-identity-keys #{:run-id :runId :startedAt :tick-id :wm-run-id})

;; TWO tests, not one, because they disagree on a file in this ledger and the
;; disagreement is the finding rather than a nuisance.
;; f6? is the F6 predicate copied from generate_variable_situation_accounting.bb
;; :450-462 -- top level, or a top-level sequence of maps, and no deeper.
;; nested? asks whether the artifact carries a run identity ANYWHERE. A licence
;; that fails f6? and passes nested? names a real record the rule cannot see,
;; which is a different thing from a fixture with no run at all, and a checker
;; that reports only f6? cannot tell them apart.
(defn- read-edn [path]
  (try (edn/read-string {:default (fn [_ x] x)} (slurp path)) (catch Exception _ nil)))
(defn f6? [path]
  (and path (str/ends-with? path ".edn")
       (let [v (read-edn path)
             maps (cond (map? v) [v] (sequential? v) (filter map? v) :else [])]
         (boolean (some (fn [m] (some run-identity-keys (keys m))) maps)))))
(defn nested? [path]
  (and path (str/ends-with? path ".edn")
       (boolean (some #(and (map? %) (some run-identity-keys (keys %)))
                      (tree-seq coll? seq (read-edn path))))))
(defn run-identity? [path] (f6? path))

(def scale (:rung-order led))
(def idx (into {} (map-indexed (fn [i r] [r i]) scale)))

(println "=== A. VERBATIM FIELDS AGAINST aif-equations.edn AT HEAD ===")
(doseq [r (:rows led)]
  (let [e (by-id (:id r))]
    (when-not e (fail! "A" (:id r) "no such equation in the registry"))
    (doseq [k [:defines :node :formal]]
      (when (and e (not= (k r) (k e)))
        (fail! "A" (:id r) k "DIFFERS from registry:" (pr-str (k r)) "vs" (pr-str (k e)))))))
(printf "rows %d, registry equations %d, ids equal: %s%n"
        (count (:rows led)) (count (:equations reg))
        (= (set (map :id (:rows led))) (set (map :id (:equations reg)))))
(when-not (= (set (map :id (:rows led))) (set (map :id (:equations reg))))
  (fail! "A" "id sets differ"))

(println)
(println "=== B/C. EVERY :licence POINTER, AND THE MACHINE-RECORD TEST ===")
(printf "%-24s %-6s %-22s %-13s %-9s %s%n" "row" "leg" "rung" "resolve" "record?" "licence")
(doseq [r (:rows led), leg [:spec-leg :impl-leg]]
  (let [{:keys [rung licence]} (leg r)
        res (resolve-ptr licence)
        rec? (f6? (:path res))
        deep? (nested? (:path res))
        needs? (and (idx rung) (>= (idx rung) (idx :witnessed)))]
    (when-not (idx rung) (fail! "D" (:id r) leg "rung off the F6 scale:" rung))
    (when (not= :ok (:status res)) (fail! "B" (:id r) leg "licence" licence (:status res)))
    (when (and needs? (not rec?) (not deep?))
      (fail! "C" (:id r) leg "claims" rung "on" licence "which carries NO run identity at any depth"))
    (when (and needs? (not rec?) deep?)
      (println "  NOTE" (:id r) leg "claims" rung "on a record whose run identity is NESTED --"
               "the F6 predicate refuses it; the row must say so at its :basis"))
    (printf "%-24s %-6s %-22s %-13s %-9s %s%n" (:id r) (if (= leg :spec-leg) "spec" "impl")
            rung (name (:status res)) (cond rec? "top" deep? "nested" :else "no") licence)))

(println)
(println "=== D. :leading-leg AGAINST THE TWO ADJUDICATED RUNGS ===")
(doseq [r (:rows led)]
  (let [s (idx (get-in r [:spec-leg :rung])) i (idx (get-in r [:impl-leg :rung]))
        expect (cond (nil? s) :unknown (nil? i) :unknown
                     (> s i) :spec-ahead (< s i) :impl-ahead :else :level)]
    (when (not= expect (:leading-leg r))
      (fail! "D" (:id r) "leading-leg is" (:leading-leg r) "but rungs give" expect))
    (printf "  %-24s spec=%-22s impl=%-22s marked=%-12s computed=%s%n"
            (:id r) (get-in r [:spec-leg :rung]) (get-in r [:impl-leg :rung])
            (:leading-leg r) expect)))

(println)
(println "=== E. EVERY \"no run identity\" CLAIM, CHECKED AGAINST THE NAMED FILE ===")
;; A basis that says an artifact carries no run identity names that artifact.
;; Recompute the claim rather than believe it -- if one of these files DOES
;; carry a run identity the row is under-rung, which is a defect in the
;; direction the reviewer is least likely to notice.
(def named-artifacts (atom #{}))
(doseq [r (:rows led), leg [:spec-leg :impl-leg]]
  (let [b (get-in r [leg :basis])]
    (doseq [[_ f] (re-seq #"(runs/[A-Za-z0-9_./-]+\.(?:edn|txt))" (str b))]
      (swap! named-artifacts conj f))))
(doseq [f (sort @named-artifacts)]
  (let [p (str wm f) exists? (.exists (io/file p))]
    (printf "  %-52s exists=%-5s run-identity=%s%n" f exists?
            (if exists? (run-identity? p) "n/a"))
    (when-not exists? (fail! "E" f "named in a basis and does not exist"))))

(println)
(println "=== F. CERTIFICATE / :converged? COHERENCE ===")
(doseq [r (:rows led)]
  (let [c (:certificate r)]
    (when (and (:converged? r) (not (and (:green? c) (:accepted-run? c))))
      (fail! "F" (:id r) ":converged? true without a green accepted-run certificate"))
    (when (and (map? c) (not (:artifact c)) (not (:trailing-leg c)))
      (fail! "F" (:id r) "certificate is neither an artifact nor a trailing-leg reason"))))
(printf "  converged? true: %d of %d%n"
        (count (filter :converged? (:rows led))) (count (:rows led)))

(println)
(if (seq @fails)
  (do (println "REVIEW FINDINGS:") (doseq [f @fails] (println "  *" f))
      (printf "review-check: %d finding(s)%n" (count @fails)))
  (println "review-check: 0 findings"))
