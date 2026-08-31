#!/usr/bin/env bb
(ns checks.lean-sorry-category-check
  (:require [babashka.fs :as fs]
            [clojure.string :as str]))

(def source-path "/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean")
(def workspace "/home/joe/code/futon2")

(def repository-roots
  {"futon2" "/home/joe/code/futon2"
   "futon3" "/home/joe/code/futon3"
   "mathlib4" "/home/joe/code/mathlib4"
   "p4ng" "/home/joe/code/p4ng"})

(def labels
  ["DELIBERATE IMPLEMENTATION REFUSAL"
   "PERMANENT EXTERNAL ATTESTATION"
   "WITNESSED-INSTANCE OBLIGATION"])

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

(defn validate-source [source]
  (let [decls (declarations source)
        findings
        (mapcat
         (fn [{:keys [name doc sorry?]}]
           (let [present (filterv #(str/includes? doc %) labels)
                 category (first present)
                 unknown (keep second
                               (re-seq #"([A-Z][A-Z -]+) · contract kind HOLE intentionally" doc))
                 unknown (remove (set labels) unknown)
                 checker-errors
                 (if (= category "PERMANENT EXTERNAL ATTESTATION")
                   (let [paths (vec (checker-paths doc))]
                     (if (empty? paths)
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
              (when (and sorry? (not= 1 (count present)))
                [{:declaration name :reason :sorry-category-count
                  :count (count present) :categories present}])
              (when (> (count present) 1)
                [{:declaration name :reason :double-category :categories present}])
              (when (and (= category "DELIBERATE IMPLEMENTATION REFUSAL") (not sorry?))
                [{:declaration name :reason :refusal-label-on-proved}])
              (when (and (= category "PERMANENT EXTERNAL ATTESTATION") (not sorry?))
                [{:declaration name :reason :attestation-label-on-proved}])
              (when (and (= category "WITNESSED-INSTANCE OBLIGATION") sorry?)
                [{:declaration name :reason :witnessed-obligation-has-sorry}])
              (for [label unknown]
                {:declaration name :reason :unknown-category :category label})
              checker-errors
              fixture-errors)))
         decls)]
    {:pass? (empty? findings)
     :declarations (count decls)
     :sorry-count (count (filter :sorry? decls))
     :sorry-category-counts
     (frequencies
      (keep (fn [{:keys [doc sorry?]}]
              (when sorry? (first (filter #(str/includes? doc %) labels))))
            decls))
     :category-counts (frequencies
                       (mapcat (fn [{:keys [doc]}]
                                 (filter #(str/includes? doc %) labels))
                               decls))
     :findings (vec findings)}))

(defn mutate [source mode]
  (case mode
    "--negative-unlabelled"
    (str/replace-first source "DELIBERATE IMPLEMENTATION REFUSAL" "UNLABELLED HOLE")

    "--negative-double"
    (str/replace-first source
                       "DELIBERATE IMPLEMENTATION REFUSAL · contract kind HOLE intentionally"
                       "DELIBERATE IMPLEMENTATION REFUSAL · PERMANENT EXTERNAL ATTESTATION · contract kind HOLE intentionally")

    "--negative-proved-label"
    (str/replace-first source
                       "CLOSED-BY-RECORD · owner: P-R19-preferences-open §principle · holder: by-record · COUNTEREXAMPLE"
                       "PERMANENT EXTERNAL ATTESTATION · contract kind HOLE intentionally · owner: P-R19-preferences-open §principle · holder: by-record · COUNTEREXAMPLE")

    "--negative-missing-checker"
    (str/replace-first source "checks/preference_stack_binding_check.clj"
                       "checks/does_not_exist.clj")

    "--negative-missing-fixture"
    (str/replace-first source
                       "futon2:holes/labs/wm-contract/ablation-exact-dyadic.edn"
                       "futon2:holes/labs/wm-contract/does-not-exist.edn")

    "--negative-fixture-drift"
    (str/replace-first source
                       "f315b748420540688ef81086101b5789a4ecb2bd2a84c7a2b491f94fe8c56261"
                       (apply str (repeat 64 "0")))

    source))

(defn -main [& args]
  (let [mode (first args)
        negative? (some? mode)
        report (validate-source (mutate (slurp source-path) mode))]
    (println "lean-sorry-category-check:"
             (cond
               (and negative? (not (:pass? report))) "negative-control PASS"
               negative? "mutation slipped"
               (:pass? report) "PASS"
               :else "FAIL")
             (pr-str report)
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit
     (cond
       (and negative? (not (:pass? report))) 0
       negative? 2
       (:pass? report) 0
       :else 1))))

(apply -main *command-line-args*)
