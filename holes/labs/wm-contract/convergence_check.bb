#!/usr/bin/env bb

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.set :as set]
         '[clojure.string :as str])

(def schema :wm/convergence-v1)
(def rungs [:named :type-transcribed :formula-transcribed :witnessed
            :constructed :wired :validated :run-correlated])
(def run-keys #{:run-id :runId :startedAt :tick-id :wm-run-id :run/id})
(def required-caveats ["BOX-5 STALENESS" "R5 CERTIFICATE LIMIT"
                       "R17 CLASS-(b) DIVERGENCE" "retired scalar F"])

(defn finding! [code & xs]
  (println (str code) (str/join " " (map pr-str xs)))
  false)

(defn tree-values [x] (tree-seq coll? seq x))
(defn nested-run-ids [x]
  (set (for [v (tree-values x) :when (map? v)
             [k value] v :when (and (run-keys k) (some? value))]
         (str value))))
(defn f6-machine-record? [x]
  (let [records (cond (map? x) [x]
                      (and (sequential? x) (every? map? x)) x
                      :else [])]
    (boolean (some #(some run-keys (keys %)) records))))

(defn source-files [roots]
  (->> roots (mapcat file-seq) (filter #(.isFile ^java.io.File %))
       (remove #(let [p (.getPath ^java.io.File %)]
                  (or (str/includes? p "/.git/") (str/includes? p "/target/"))))
       (map #(.getCanonicalFile ^java.io.File %)) distinct vec))
(defn suffix-matches [files rel]
  (filterv #(str/ends-with? (str/replace (.getPath ^java.io.File %) "\\" "/")
                           (str "/" (str/replace rel #"^/+" ""))) files))
(defn line-count [file]
  (with-open [r (io/reader file)] (count (line-seq r))))
(defn parse-licence [s]
  (when-let [[_ file a b] (and (string? s) (re-matches #"(.+):(\d+)(?:-(\d+))?" s))]
    {:file file :start (parse-long a) :end (parse-long (or b a))}))
(defn resolve-licence [files s]
  (if-let [{:keys [file start end] :as parsed} (parse-licence s)]
    (let [matches (suffix-matches files file)]
      (cond (empty? matches) (assoc parsed :status :unresolved)
            (> (count matches) 1) (assoc parsed :status :ambiguous :matches matches)
            (or (> start end) (> end (line-count (first matches))))
            (assoc parsed :status :unresolved)
            :else (assoc parsed :status :resolved :resolved (first matches))))
    {:status :unresolved :file s}))
(defn read-artifact [resolution]
  (when (= :resolved (:status resolution))
    (try (edn/read-string {:default (fn [_ v] v)} (slurp (:resolved resolution)))
         (catch Exception _ nil))))
(defn quantity-evidence? [id artifact]
  (case id
    :policy-set (and (= :wm/cascade-policy-decision-v1 (:schema artifact))
                     (<= 2 (count (:candidates artifact))))
    ;; Future witness rungs must add an explicit quantity-specific recognizer.
    false))
(defn expected-leading [order spec impl]
  (cond (> (order spec) (order impl)) :spec-ahead
        (< (order spec) (order impl)) :impl-ahead
        :else :level))
(defn accepted-run-ids [pin]
  (set (map (comp str :run-id) (:pin/accepted-steps pin))))

(defn check! [ledger equations pin files]
  (let [rows (:rows ledger) row-groups (group-by :id rows)
        eqs (into {} (map (juxt :id identity) (:equations equations)))
        order (zipmap rungs (range)) findings (atom [])
        licences (into {} (for [row rows leg [:spec-leg :impl-leg]]
                            [[(:id row) leg]
                             (resolve-licence files (get-in row [leg :licence]))]))
        accepted (accepted-run-ids pin)]
    (println "run-identity-policy :nested-tree; F6-top-level predicate reported separately")
    (when-not (= schema (:schema ledger))
      (swap! findings conj (finding! :error/not-a-convergence-ledger (:schema ledger))))
    (when-not (seq rows) (swap! findings conj (finding! :error/vacuous :rows)))
    (doseq [[id rs] row-groups :when (> (count rs) 1)]
      (swap! findings conj (finding! :error/duplicate-row-id id (count rs))))
    (let [actual (set (keys row-groups)) expected (set (keys eqs))]
      (when-not (= actual expected)
        (swap! findings conj (finding! :error/row-set-mismatch
                                       :missing (sort (set/difference expected actual))
                                       :extra (sort (set/difference actual expected))))))
    (doseq [row rows :let [eq (eqs (:id row))] :when eq
            field [:defines :node :formal] :when (not= (field row) (field eq))]
      (swap! findings conj (finding! :error/registry-transcription-drift
                                     (:id row) field :ledger (field row) :registry (field eq))))
    (when-not (= rungs (:rung-order ledger))
      (swap! findings conj (finding! :error/rung-order-drift (:rung-order ledger))))
    (doseq [row rows leg [:spec-leg :impl-leg]
            :let [rung (get-in row [leg :rung])] :when (not (contains? order rung))]
      (swap! findings conj (finding! :error/unknown-rung (:id row) leg rung)))
    (doseq [[[id leg] resolution] licences]
      (case (:status resolution)
        :unresolved (swap! findings conj (finding! :error/unresolved-licence id leg (:file resolution)))
        :ambiguous (swap! findings conj (finding! :error/ambiguous-licence id leg (:file resolution)
                                                  (mapv #(.getPath ^java.io.File %) (:matches resolution))))
        nil))
    (doseq [row rows leg [:spec-leg :impl-leg]
            :let [rung (get-in row [leg :rung])]
            :when (and (contains? order rung) (>= (order rung) (order :witnessed)))
            :let [resolution (licences [(:id row) leg]) artifact (read-artifact resolution)
                  nested (nested-run-ids artifact) f6? (f6-machine-record? artifact)
                  evidence? (quantity-evidence? (:id row) artifact)]
            :when (not (and (seq nested) evidence?))]
      (swap! findings conj (finding! :error/unlicensed-witness (:id row) leg
                                     :f6-top-level-run? f6? :nested-run-ids (sort nested)
                                     :quantity-evidence? evidence?)))
    (doseq [row rows
            :let [s (get-in row [:spec-leg :rung]) i (get-in row [:impl-leg :rung])]
            :when (and (contains? order s) (contains? order i)
                       (not= (:leading-leg row) (expected-leading order s i)))]
      (swap! findings conj (finding! :error/leading-leg-mismatch (:id row)
                                     :stored (:leading-leg row) :computed (expected-leading order s i))))
    (doseq [row rows :let [c (:certificate row)
                           artifact-form? (contains? c :artifact)
                           trailing-form? (contains? c :trailing-leg)]]
      (cond
        (and artifact-form? trailing-form?)
        (swap! findings conj (finding! :error/ambiguous-certificate-form (:id row)))
        artifact-form?
        (let [resolution (resolve-licence files (:artifact c)) artifact (read-artifact resolution)
              ids (nested-run-ids artifact) computed-accepted? (boolean (seq (set/intersection ids accepted)))]
          (when (not= computed-accepted? (boolean (:accepted-run? c)))
            (swap! findings conj (finding! :error/accepted-run-mismatch (:id row)
                                           :declared (:accepted-run? c) :computed computed-accepted? :run-ids (sort ids))))
          (when (and (not (and (:green? c) (:accepted-run? c)))
                     (not (and (string? (:reason c)) (seq (:reason c)))))
            (swap! findings conj (finding! :error/certificate-without-reason (:id row))))
          (when (and (:converged? row) (not (and (:green? c) computed-accepted?)))
            (swap! findings conj (finding! :error/false-convergence (:id row)))))
        trailing-form?
        (do (when-not (#{:spec :impl :both} (:trailing-leg c))
              (swap! findings conj (finding! :error/invalid-trailing-leg (:id row) (:trailing-leg c))))
            (when-not (and (string? (:reason c)) (seq (:reason c))
                           (string? (:what-would-license c)) (seq (:what-would-license c)))
              (swap! findings conj (finding! :error/incomplete-trailing-certificate (:id row))))
            (when (:converged? row)
              (swap! findings conj (finding! :error/false-convergence (:id row)))) )
        :else (swap! findings conj (finding! :error/unknown-certificate-form (:id row)))))
    (doseq [needle required-caveats :when (not (some #(str/includes? % needle) (:caveats ledger)))]
      (swap! findings conj (finding! :error/missing-caveat needle)))
    (if (some false? @findings) false
        (let [higher (for [row rows leg [:spec-leg :impl-leg]
                           :let [r (get-in row [leg :rung])] :when (>= (order r) (order :witnessed))]
                       [row leg (read-artifact (licences [(:id row) leg]))])
              f6-count (count (filter #(f6-machine-record? (nth % 2)) higher))
              nested-count (count (filter #(seq (nested-run-ids (nth % 2))) higher))]
          (println "ACCEPT" (count rows) "rows, 54 identity fields, 36 licences," f6-count
                   "F6-top-level run identities," nested-count "nested run identities,"
                   (count (filter :converged? rows)) "converged")
          true))))

(let [script (.getCanonicalFile (io/file *file*)) lab (.getParentFile script)
      futon2 (.getCanonicalFile (io/file lab "../../..")) code (.getParentFile futon2)
      ledger-path (or (first *command-line-args*) (.getPath (io/file lab "CONVERGENCE.edn")))
      equation-path (or (second *command-line-args*) (.getPath (io/file lab "aif-equations.edn")))
      pin-path (or (nth *command-line-args* 2 nil) (.getPath (io/file futon2 "data/wm-step/w1/pin/pin.edn")))]
  (if-not (.isFile (io/file pin-path))
    (do (finding! :error/accepted-run-authority-absent pin-path) (System/exit 1))
    (try (let [ledger (edn/read-string (slurp ledger-path))
               equations (edn/read-string (slurp equation-path))
               pin (edn/read-string (slurp pin-path))
               files (source-files [futon2 (io/file code "p4ng") (io/file code "mathlib4")])]
           (when-not (check! ledger equations pin files) (System/exit 1)))
         (catch Exception e
           (finding! :error/not-a-convergence-ledger ledger-path (.getName (class e)) (.getMessage e))
           (System/exit 1)))))
