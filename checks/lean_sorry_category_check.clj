#!/usr/bin/env bb
(ns checks.lean-sorry-category-check
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def source-path "/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean")
(def workspace "/home/joe/code/futon2")
(def lifecycle-path
  "/home/joe/code/futon2/holes/labs/wm-contract/runs/U27-hole-closability/audit.edn")
(def witness-registry-path
  "/home/joe/code/futon2/checks/witness-registry.edn")

(def repository-roots
  {"futon2" "/home/joe/code/futon2"
   "futon3" "/home/joe/code/futon3"
   "mathlib4" "/home/joe/code/mathlib4"
   "p4ng" "/home/joe/code/p4ng"})

(def attestation-labels
  ["DELIBERATE IMPLEMENTATION REFUSAL"
   "PERMANENT EXTERNAL ATTESTATION"
   "WITNESSED-INSTANCE OBLIGATION"])

(def current-categories
  (conj attestation-labels
        "OPEN, RUN-GATED"
        "DEFERRAL UNDER ORGANIZED DISCOVERY"))

(defn first-clause [doc]
  (str/trim (first (str/split doc #"·" 2))))

(defn current-category [doc]
  (let [clause (first-clause doc)
        present (filterv #(str/includes? clause %) current-categories)
        category (when (some #{clause} current-categories) clause)]
    {:clause clause
     :present present
     :category category}))

(defn historical-category-mentions [doc]
  (let [history (second (str/split doc #"·" 2))]
    (frequencies
     (mapcat (fn [label]
               (repeat (count (re-seq (re-pattern (java.util.regex.Pattern/quote label))
                                      (or history "")))
                       label))
             current-categories))))

(defn declarations [source]
  (->> (str/split source #"(?=/--)")
       (keep (fn [chunk]
               (when-let [[_ doc] (re-find #"(?s)^/--(.*?)-/" chunk)]
                 (when-let [[_ name]
                            (re-find
                             #"(?m)^(?:private\s+)?(?:noncomputable\s+)?(?:def|theorem|structure|inductive|abbrev)\s+([A-Za-z0-9_]+)"
                             chunk)]
                   {:name name :doc doc :sorry? (str/includes? chunk ":= sorry")}))))))

(defn checker-paths [doc]
  (map second (re-seq #"`(checks/[^` ]+\.clj)`" doc)))

(defn sha256-file [path]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (with-open [input (java.io.BufferedInputStream. (java.io.FileInputStream. (str path)))]
      (let [buffer (byte-array 8192)]
        (loop []
          (let [n (.read input buffer)]
            (when (pos? n)
              (.update digest buffer 0 n)
              (recur))))))
    (format "%064x" (java.math.BigInteger. 1 (.digest digest)))))

(defn fixture-references [doc]
  (let [fixtures (mapv (fn [[_ repo path]] {:repo repo :path path})
                       (re-seq #"fixture: `([^:` ]+):([^`]+)`" doc))
        pins (mapv second (re-seq #"fixture-sha256: `([0-9a-f]{64})`" doc))]
    {:fixtures fixtures :pins pins}))

(defn obligation-fixture-errors [name doc]
  (let [{:keys [fixtures pins]} (fixture-references doc)]
    (cond
      (not= 1 (count fixtures))
      [{:declaration name :reason :obligation-fixture-count :count (count fixtures)}]

      (not= 1 (count pins))
      [{:declaration name :reason :obligation-fixture-pin-count :count (count pins)}]

      :else
      (let [{:keys [repo path]} (first fixtures)
            root (get repository-roots repo)
            target (when root (fs/path root path))]
        (cond
          (nil? root)
          [{:declaration name :reason :obligation-fixture-unknown-repository :repo repo}]

          (not (fs/regular-file? target))
          [{:declaration name :reason :obligation-fixture-missing
            :repo repo :path path}]

          (not= (first pins) (sha256-file target))
          [{:declaration name :reason :obligation-fixture-pin-mismatch
            :repo repo :path path :recorded (first pins)
            :actual (sha256-file target)}]

          :else [])))))

(defn registry-evidence [registry-by-name name]
  (when-let [row (get registry-by-name name)]
    (let [check-path (get-in row [:check :path])
          report-path (get-in row [:report :path])
          check-target (when check-path (fs/path workspace check-path))
          report-target (when report-path (fs/path workspace report-path))]
      {:source :witness-registry
       :recorded-at (:recorded-at row)
       :result (:result row)
       :check check-path
       :check-present? (boolean (and check-target (fs/regular-file? check-target)))
       :report report-path
       :report-present? (boolean (and report-target (fs/regular-file? report-target)))
       :expected-rejection (:expected-rejection row)
       :control (:control row)})))

(defn validate-source
  ([source] (validate-source source {} {}))
  ([source lifecycle] (validate-source source lifecycle {}))
  ([source lifecycle registry-by-name]
  (let [lifecycle-by-name (or (:by-name lifecycle) lifecycle)
        lifecycle-provenance (select-keys lifecycle [:as-of :authority])
        decls (declarations source)
        findings
        (mapcat
         (fn [{:keys [name doc sorry?]}]
           (let [{:keys [clause present category]} (current-category doc)
                 witness (registry-evidence registry-by-name name)
                 executable-witness? (and witness (:check-present? witness)
                                          (= :passed (:result witness))
                                          (or (:expected-rejection witness)
                                              (:control witness)))
                 checker-errors
                 (if (= category "PERMANENT EXTERNAL ATTESTATION")
                   (let [paths (vec (checker-paths doc))]
                     (if (and (empty? paths) (not executable-witness?))
                       [{:declaration name :reason :attestation-checker-absent}]
                       (for [path paths
                             :when (not (fs/regular-file? (fs/path workspace path)))]
                         {:declaration name :reason :attestation-checker-missing
                          :path path})))
                   [])
                 fixture-errors
                 (when (= category "WITNESSED-INSTANCE OBLIGATION")
                   (obligation-fixture-errors name doc))]
             (concat
              (when (and sorry? (nil? category))
                [{:declaration name :reason :sorry-category-count
                  :count 0 :categories []
                  :current-clause clause}])
              (when (> (count present) 1)
                [{:declaration name :reason :double-category :categories present}])
              (when (and sorry? (nil? category) (<= (count present) 1))
                [{:declaration name :reason :unknown-current-category
                  :current-clause clause}])
              (when (and (= category "DELIBERATE IMPLEMENTATION REFUSAL") (not sorry?))
                [{:declaration name :reason :refusal-label-on-proved}])
              (when (and (= category "PERMANENT EXTERNAL ATTESTATION") (not sorry?))
                [{:declaration name :reason :attestation-label-on-proved}])
              (when (and (= category "WITNESSED-INSTANCE OBLIGATION") sorry?)
                [{:declaration name :reason :witnessed-obligation-has-sorry}])
              checker-errors
              fixture-errors)))
         decls)]
    {:pass? (empty? findings)
     :declarations (count decls)
     :sorry-count (count (filter :sorry? decls))
     :sorry-category-counts
     (frequencies
      (keep (fn [{:keys [doc sorry?]}]
              (when sorry? (:category (current-category doc))))
            decls))
     :current-declaration-category-counts
     (frequencies (keep (comp :category current-category :doc) decls))
     :category-counts
     (frequencies (keep (comp :category current-category :doc) decls))
     :historical-category-mentions
     (apply merge-with + (map (comp historical-category-mentions :doc) decls))
     :lifecycle-provenance lifecycle-provenance
     :sorry-declarations
     (mapv (fn [{:keys [name doc sorry?]}]
             (when sorry?
               (merge {:name name
                       :declaration-category (:category (current-category doc))}
                      (when-let [evidence (registry-evidence registry-by-name name)]
                        {:witness-evidence evidence})
                      (select-keys (get lifecycle-by-name name)
                                   [:closability :readiness]))))
           (filter :sorry? decls))
     :lifecycle-counts
     {:closability (frequencies (keep :closability (vals lifecycle-by-name)))
      :readiness (frequencies (keep :readiness (vals lifecycle-by-name)))}
     :findings (vec findings)})))

(defn read-lifecycle [path]
  (let [doc (edn/read-string (slurp path))]
    {:as-of (:as-of doc)
     :authority (:authority doc)
     :by-name (into {} (keep (fn [row]
                               (when (= :contract-declaration (:row-source row))
                                 [(:name row) row])))
                    (:rows doc))}))

(defn read-registry [path]
  (into {} (map (juxt :witnesses identity)) (edn/read-string (slurp path))))

(defn mutate [source mode]
  (case mode
    "--negative-unlabelled"
    (str/replace-first source "DELIBERATE IMPLEMENTATION REFUSAL" "UNLABELLED HOLE")

    "--negative-double"
    (str/replace-first source
                       "DELIBERATE IMPLEMENTATION REFUSAL · contract kind HOLE intentionally"
                       "DELIBERATE IMPLEMENTATION REFUSAL + PERMANENT EXTERNAL ATTESTATION · contract kind HOLE intentionally")

    "--negative-proved-label"
    (str/replace-first source
                       "CLOSED-BY-RECORD · owner: P-R19-preferences-open §principle · holder: by-record · COUNTEREXAMPLE"
                       "PERMANENT EXTERNAL ATTESTATION · contract kind HOLE intentionally · owner: P-R19-preferences-open §principle · holder: by-record · COUNTEREXAMPLE")

    "--negative-missing-checker"
    (str/replace-first source "checks/preference_stack_binding_check.clj"
                       "checks/does_not_exist.clj")

    "--negative-missing-fixture"
    (str source
         "\n/-- WITNESSED-INSTANCE OBLIGATION · fixture: `futon2:holes/labs/wm-contract/does-not-exist.edn` · fixture-sha256: `"
         (apply str (repeat 64 "0")) "` -/\ndef negativeMissingFixture : Prop := True\n")

    "--negative-fixture-drift"
    (str source
         "\n/-- WITNESSED-INSTANCE OBLIGATION · fixture: `futon2:holes/labs/wm-contract/ablation-exact-dyadic.edn` · fixture-sha256: `"
         (apply str (repeat 64 "0")) "` -/\ndef negativeFixtureDrift : Prop := True\n")

    source))

(def negative-reasons
  {"--negative-unlabelled" :unknown-current-category
   "--negative-double" :double-category
   "--negative-proved-label" :attestation-label-on-proved
   "--negative-missing-checker" :attestation-checker-missing
   "--negative-missing-fixture" :obligation-fixture-missing
   "--negative-fixture-drift" :obligation-fixture-pin-mismatch})

(defn negative-detected? [mode report]
  (some #(= (negative-reasons mode) (:reason %)) (:findings report)))

(defn -main [& args]
  (let [mode (first args)
        negative? (some? mode)
        report (validate-source (mutate (slurp source-path) mode)
                                (read-lifecycle lifecycle-path)
                                (read-registry witness-registry-path))
        detected? (and negative? (negative-detected? mode report))]
    (println "lean-sorry-category-check:"
             (cond
               detected? "negative-control PASS"
               negative? "mutation slipped"
               (:pass? report) "PASS"
               :else "FAIL")
             (pr-str report)
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit
     (cond
       detected? 0
       negative? 2
       (:pass? report) 0
       :else 1))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
