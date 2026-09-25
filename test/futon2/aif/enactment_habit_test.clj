(ns futon2.aif.enactment-habit-test
  "Tests for futon2.aif.enactment-habit, live-pinned to the click-001 records
  (H-E-D §4, futon2 69bfd1ab). The enactment, click and constructor-replay
  records are READ from futon3c at test time; every bad case is an edit of the
  live record. The file sha256s are asserted first, so a changed record fails
  here loudly instead of passing against different data.

  W_c verdicts are input data. Where a case needs a failing verdict, the test
  writes the failure string check-c (futon3c exemplar proof2a_check.clj
  L172-192) would return for that edit, in check-c's own format."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-prior :as prior]
            [futon2.aif.enactment-habit :as eh])
  (:import [java.security MessageDigest]))

(def ^:private exemplar-dir
  "/home/joe/code/futon3c/holes/labs/M-futon-seams/exemplar")

(def ^:private pinned-sha256
  "Captured 2026-09-25 at futon3c 7fc18ef0."
  {"click-001.edn" "98aa1cba12cdd1c759474d54447e89de38bcea58aa00fe6cd7cb392776a41409"
   "click-001-enactment.edn" "e51063896e2a42096718d902e0b4dfe0e4652323de0b42848c2c0cf318bf6c89"
   "construct-replay.edn" "5ed3770bcbf7ae383256a89c11fde9c069521fd6eea4c95e1906694d90714789"})

(defn- text [name] (slurp (io/file exemplar-dir name)))

(defn- sha256 [^String s]
  (let [d (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) d))))

(def ^:private click (delay (edn/read-string (text "click-001.edn"))))
(def ^:private enactment (delay (edn/read-string (text "click-001-enactment.edn"))))
(def ^:private replay (delay (edn/read-string (text "construct-replay.edn"))))

(def ^:private cand-a :cand/a-registry-first)
(def ^:private cand-b :cand/b-observe-first)

(defn- key-of [enact] (eh/policy-key-for enact @click @replay))
(defn- key-a [] (key-of @enactment))
(defn- key-b [] (key-of (assoc @enactment :candidate cand-b)))

(defn- successes [enact] (filter :success (:attempts enact)))

(defn- check-c-no-success
  "check-c's failure string for chosen patterns lacking a successful attempt."
  [enact]
  (let [pats (set (nth (key-a) 2))
        missing (remove (set (map :pattern (successes enact))) pats)]
    (str "W_c: chosen patterns with no successful attempt: " (vec (sort-by str missing)))))

(defn- remove-attempt [enact pred]
  (update enact :attempts #(vec (remove pred %))))

(defn- close? [x y] (< (Math/abs (- (double x) (double y))) 1e-9))

(defn- masses-ab [state] (eh/masses state [(key-a) (key-b)]))

(deftest live-records-pinned
  (doseq [[name sha] pinned-sha256]
    (is (= sha (sha256 (text name))) (str name " changed since the pin"))))

(deftest key-matches-h-e-d-section-1
  (is (= [:pattern-cascade ":inst/i4"
          [:cascade-construction/choose-the-grain-where-state-lives
           :coordination/assignment-binding
           :cycle-machine/single-producer
           :or3/count-every-card-back
           :gauntlet/placenta-transfer
           :translation/test-by-reproducing-behaviour
           :realtime/mode-gate]
          {}]
         (key-a)))
  (testing "a and b are distinct policies of one menu"
    (is (vector? (key-b)))
    (is (not= (key-a) (key-b))))
  (testing "a missing slot refuses typed"
    (is (= :precedence-absent
           (:reason (eh/policy-key-for @enactment @click {:replays []}))))
    (is (= :candidate-not-in-click
           (:reason (key-of (assoc @enactment :candidate :cand/c-absent)))))))

(deftest pass-counts-once
  (let [r (eh/increment @enactment (key-a) [])
        state (eh/fold nil [r])
        [ma mb] (masses-ab state)]
    (is (= 1 (:delta r)))
    (is (= [(:click @enactment) cand-a] (:record-id r)))
    (is (= (count (:attempts @enactment)) (:attempts r)))
    (is (= (mapv :kind (get-in @enactment [:conformance :deviations])) (:deviations r)))
    (is (= :wm/enactment-habit-v1 (:basis r)))
    (is (= 1 (get-in state [:counts (key-a)])))
    (is (close? ma (/ 2.0 3.0)))
    (is (close? mb (/ 1.0 3.0)))))

(deftest a-delete-the-only-success-of-a-pattern
  (let [bad (remove-attempt @enactment #(and (= 7 (:n %)) (:success %)))
        verdict [(check-c-no-success bad)]
        r (eh/increment bad (key-a) verdict)
        [ma mb] (masses-ab (eh/fold nil [r]))]
    (is (str/includes? (first verdict) ":translation/test-by-reproducing-behaviour"))
    (is (= 0 (:delta r)))
    (is (= verdict (:wc-failures r)))
    (is (close? ma 0.5))
    (is (close? mb 0.5))))

(deftest a-prime-a-pattern-with-two-successes-keeps-its-count
  (let [n4 (first (filter #(= 4 (:n %)) (:attempts @enactment)))
        doubled (update @enactment :attempts conj n4)
        one-removed (update doubled :attempts
                            (fn [atts] (let [i (.indexOf ^java.util.List atts n4)]
                                         (vec (concat (subvec atts 0 i) (subvec atts (inc i)))))))
        r (eh/increment one-removed (key-a) [])]
    (is (= 1 (count (filter #(= n4 %) (:attempts one-removed)))))
    (is (= 1 (:delta r)))))

(deftest b-a-success-without-its-check
  (let [bad (update @enactment :attempts
                    (fn [atts] (mapv #(if (= 5 (:n %)) (dissoc % :check) %) atts)))
        stripped (filter #(and (:success %) (nil? (:check %))) (:attempts bad))
        verdict [(str "W_c: successful attempts with no check: " (vec (map :pattern stripped)))]
        r (eh/increment bad (key-a) verdict)]
    (is (= [:cycle-machine/single-producer] (mapv :pattern stripped)))
    (is (= 0 (:delta r)))
    (is (= verdict (:wc-failures r)))
    (is (nil? (get-in (eh/fold nil [r]) [:counts (key-a)])))))

(deftest c-an-attempt-outside-the-candidate
  (let [outside-pattern :agency/single-routing-authority
        bad (update @enactment :attempts conj
                    {:n 8 :pattern outside-pattern :commit "eafd07b7"
                     :produced :one-routing-path :success true
                     :check {:kind :grep :cmd "true"}})
        r (eh/increment bad (key-a) [])
        state (eh/fold nil [r])]
    (is (some #{outside-pattern} (nth (key-b) 2)) "the pattern is candidate b's")
    (is (= :refused (:status r)))
    (is (= :attempt-outside-candidate (:reason r)))
    (is (= [outside-pattern] (:outside r)))
    (is (= {} (:counts state)))
    (is (zero? (:samples state)))))

(deftest d-the-same-receipt-twice
  (let [r (eh/increment @enactment (key-a) [])
        state (eh/fold nil [r r])]
    (is (= 1 (get-in state [:counts (key-a)])))
    (is (= 1 (:samples state)))
    (is (= [{:record-id (:record-id r) :duplicate-of r}] (:duplicates state)))))

(deftest d-prime-a-whitespace-edited-copy
  (let [edited-text (str/replace (text "click-001-enactment.edn") "\n" "\n\n")
        copy (edn/read-string edited-text)
        r1 (eh/increment @enactment (key-a) [])
        r2 (eh/increment copy (key-of copy) [])
        state (eh/fold (eh/fold nil [r1]) [r2])]
    (is (not= (sha256 edited-text) (get pinned-sha256 "click-001-enactment.edn")))
    (is (= 1 (get-in state [:counts (key-a)])))
    (is (= 1 (count (:duplicates state))))))

(deftest e-the-first-attempt-alone
  (let [first-only (assoc @enactment :attempts [(first (:attempts @enactment))])
        verdict [(check-c-no-success first-only)]
        r (eh/increment first-only (key-a) verdict)]
    (is (= "8e5c431e" (:commit (first (:attempts first-only)))))
    (is (false? (:success (first (:attempts first-only)))))
    (is (= 0 (:delta r)))
    (is (nil? (get-in (eh/fold nil [r]) [:counts (key-a)])))))

(deftest f-verdict-is-data-the-fold-does-not-read-g-c
  (let [grain (-> @enactment :grain)
        with-gc (update @enactment :attempts
                        (fn [atts]
                          (mapv #(if (and (:success %)
                                          (= :cascade-construction/choose-the-grain-where-state-lives
                                             (:pattern %)))
                                   (assoc % :check {:kind :grain-gate :status :pass :grain grain})
                                   %)
                                atts)))
        r (eh/increment with-gc (key-a) [])]
    (is (= 1 (count (filter #(= :grain-gate (get-in % [:check :kind])) (:attempts with-gc)))))
    (is (= 1 (:delta r)))
    (is (= r (eh/increment @enactment (key-a) [])) "same receipt as without the G_c check")))

(deftest missing-verdict-is-not-a-pass
  (let [r (eh/increment @enactment (key-a) nil)]
    (is (= 0 (:delta r)))
    (is (= {:status :absent} (:wc-verdict r)))))

(deftest fold-uses-cascade-prior-state
  (let [state (eh/fold nil [(eh/increment @enactment (key-a) [])])]
    (is (= prior/default-alpha (:alpha state)))
    (is (= state (prior/coerce-state state)))))
