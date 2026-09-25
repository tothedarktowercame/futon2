(ns futon2.aif.grain-gate
  "Pre-enactment grain gate (PROOF-2a hole H-grain; discovery packet
  holes/labs/wm-contract/proof2/packets/H-GRAIN-D.md, futon2 80541ebc).

  grain-gate(candidate, planned-attempt, repo-root) compares the grain a
  cascade CHOSE against the grain a planned attempt DECLARES, before the
  attempt is committed. It is futon3c scripts/grain_check.py's three
  comparisons moved in front of the commit:

    1. the candidate declares :grain {:keyed-by ...};
    2. the planned attempt declares the same, with a resolver as evidence;
    3. that resolver EXISTS, with the recorded argument list, in the file at
       the recorded sha256 under repo-root.

  Refusals are typed and evaluated in this order (first refusal wins):
    :grain-not-declared  candidate or attempt carries no :grain map with a
                         :keyed-by (the :detail names which)
    :grain-mismatch      candidate :keyed-by /= attempt :keyed-by
    :scope-mismatch      both grains declare :scope and they differ
    :resolver-missing    no (defn <fn> ...) in the evidence file
    :arglist-mismatch    recorded :arglist /= the arglist in the file
    :sha-mismatch        evidence file's sha256 /= recorded :sha256

  A grain record (H-GRAIN-D section 2):
    {:keyed-by  keyword          ; the mechanical comparator
     :statement string
     :evidence  {:fn string :path string :arglist string :sha256 string}
     :checked-by string
     :scope     ...}             ; declared span; absent is a typed absence

  Absence is never a substituted value: an attempt without :grain REFUSES
  (:grain-not-declared); a :scope or :sha256 the record does not carry is
  not checked and not invented.

  Pure function; the only IO is reading the evidence file under repo-root."
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.security MessageDigest]))

(defn- sha256-hex
  "Lowercase hex sha256 of a file's bytes."
  [^java.io.File f]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (java.nio.file.Files/readAllBytes (.toPath f)))]
    (apply str (map #(format "%02x" %) digest))))

(defn- resolver-arglist
  "The argument vector (as a string, e.g. \"[role]\") of the defn named
  FN-SHORT in SRC, or nil when no such defn exists. Same shape as
  grain_check.py's arglist_in: first bracketed form after the defn name."
  [src fn-short]
  (when-let [m (re-find (re-pattern (str "(?s)\\(defn-?\\s+" (java.util.regex.Pattern/quote fn-short)
                                         "\\b\\s*"
                                         ;; skip an optional docstring and attr-map, so a
                                         ;; bracket inside the docstring is not read as
                                         ;; the arglist (claude-8 review, 2026-09-25)
                                         "(?:\"(?:[^\"\\\\]|\\\\.)*\"\\s*)?(?:\\{[^}]*\\}\\s*)?"
                                         "(\\[[^\\]]*\\])"))
                        src)]
    (second m)))

(defn- refuse [reason detail]
  {:status :refuse :reason reason :detail detail})

(defn grain-gate
  "Compare CANDIDATE's :grain against PLANNED-ATTEMPT's :grain, with
  evidence files read under REPO-ROOT. Returns {:status :pass} or
  {:status :refuse :reason <typed> :detail string}."
  [candidate planned-attempt repo-root]
  (let [cand-grain (:grain candidate)
        att-grain (:grain planned-attempt)
        cand-key (:keyed-by cand-grain)
        att-key (:keyed-by att-grain)]
    (cond
      ;; comparison 1: the candidate declares a grain. A candidate without one
      ;; is the mission's 4b shape ("does not name a grain"); nothing can be
      ;; compared against it, so it refuses rather than passing by default
      ;; (claude-8 review, 2026-09-25: the bad case passed).
      (nil? cand-key)
      (refuse :grain-not-declared
              "the candidate declares no :grain {:keyed-by ...}")

      (nil? att-key)
      (refuse :grain-not-declared
              "the planned attempt declares no :grain {:keyed-by ...}")

      (and cand-key (not= cand-key att-key))
      (refuse :grain-mismatch
              (str "the candidate chose " cand-key
                   ", the attempt is keyed by " att-key))

      (and (:scope cand-grain) (:scope att-grain)
           (not= (:scope cand-grain) (:scope att-grain)))
      (refuse :scope-mismatch
              (str "the candidate's scope is " (pr-str (:scope cand-grain))
                   ", the attempt's is " (pr-str (:scope att-grain))))

      :else
      (let [ev (:evidence att-grain)
            fn-name (:fn ev)
            path (:path ev)]
        (if-not (and fn-name path)
          (refuse :resolver-missing
                  "the attempt's grain has no {:fn :path} evidence")
          (let [f (io/file repo-root path)
                src (when (.isFile f) (slurp f))
                fn-short (last (str/split fn-name #"/"))
                actual (when src (resolver-arglist src fn-short))]
            (cond
              (nil? actual)
              (refuse :resolver-missing
                      (str "no (defn " fn-short " ...) in " path
                           " — the declared grain answers to no code"))

              (and (:arglist ev) (not= actual (:arglist ev)))
              (refuse :arglist-mismatch
                      (str fn-short " takes " actual
                           ", the attempt recorded " (:arglist ev)))

              (and (:sha256 ev) (not= (sha256-hex f) (:sha256 ev)))
              (refuse :sha-mismatch
                      (str path " has changed since the grain was recorded ("
                           (subs (sha256-hex f) 0 12) "… vs "
                           (subs (:sha256 ev) 0 12) "…)"))

              :else
              {:status :pass})))))))
