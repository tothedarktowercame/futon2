(ns futon2.aif.standing-authority-test
  "Bounded recovery of the 11 prior operator-approved medic adoption bundles
  (operator-authorized 2026-10-31, policy resource wm/standing-authority).

  Hermetic: hermetic evidence roots are created BEFORE any standing load; the
  live ledger is only ever read (hash-guarded before and after); production
  vars are never redefined outside with-redefs scope that restores them."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.load-identity :as li]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.standing-authority :as standing]))

(def live-path "/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22/ledger.edn")
(def live-sha "f7b5a7aac642b35302f30e1254c707805e95188ffd3c8158c51ae6bde56a0074")
(def recovered-ordinals #{38 39 40 41 42 43 44 46 48 50 71})
(def retained-genuine-ordinals #{116 120 122})
(def deny-failing-ordinals #{38 39 40 41 42 43 44 46 48 50 71 116 118 120 122 125})

(defn- file-sha [path]
  (li/sha256 (java.nio.file.Files/readAllBytes (.toPath (io/file path)))))

(defn- live-ledger []
  (when (.isFile (io/file live-path))
    (edn/read-string (slurp live-path))))

(defn- with-entry [l ordinal f]
  (update l :entries (fn [es] (mapv #(if (and (= :medic-repaired (:observation-kind %))
                                              (= ordinal (:ordinal %)))
                                       (f %) %) es))))

(def ^:private observation-defects #'ledger/observation-defects)
(def ^:private seal-ledger #'ledger/seal-ledger)

(defn- walk-defects
  "Mirror validate-ledger's prefix walk for the selected medic-repaired
  ordinals: each selected observation is checked against its sealed prefix
  under the verifier, exactly as the loader does. Returns {ordinal defects}."
  [l verifier selected]
  (let [base (seal-ledger (assoc (select-keys l [:schema :attempt-limit :retry-limit]) :entries []))]
    (loop [prefix base, remaining (:entries l) out {}]
      (if-let [entry (first remaining)]
        (let [sel? (and (= :medic-repaired (:observation-kind entry))
                        (contains? selected (:ordinal entry)))
              defects (when sel? (observation-defects prefix entry verifier))
              next-prefix (seal-ledger (update prefix :entries conj entry))]
          (recur next-prefix (next remaining)
                 (if sel? (assoc out (:ordinal entry) defects) out)))
        out))))

(defn fresh-context-fixture [f]
  (standing/reset-context!)
  (f)
  (standing/reset-context!))

(use-fixtures :each fresh-context-fixture)

(deftest standing-context-ready-and-bounded
  (let [ctx (standing/standing-context)]
    (is (= :ready (:status ctx)) "policy + evidence + composition all resolve")
    (is (= 11 (count (get-in ctx [:identity :scopes])))
        "exactly the 11 retained bundles, no more")
    (standing/reset-context!)
    (is (= (:identity ctx) (:identity (standing/standing-context)))
        "restart repeat: fresh resolution is identical")))

(deftest live-ledger-validates-under-standing-authority
  (when-let [l (live-ledger)]
    (let [v (standing/standing-verifier)
          checked (ledger/validate-ledger l v)]
      (is (= :valid (:status checked)))
      (is (= 0 (count (:defects checked))))
      (is (= live-sha (file-sha live-path)) "canonical bytes untouched"))))

(deftest original-13-and-retained-scopes
  (when-let [l (live-ledger)]
    (let [v (standing/standing-verifier)
          deny (fn [_] false)]
      (testing "the original 13 failing ordinals now admit through the union"
        (is (every? #(= [] %) (vals (walk-defects l v (into recovered-ordinals #{118 125}))))))
      (testing "retained genuine-repair scopes unchanged"
        (is (every? #(= [] %) (vals (walk-defects l v retained-genuine-ordinals)))))
      (testing "deny still refuses all 16 medic ordinals"
        (is (every? #(seq %) (vals (walk-defects l deny deny-failing-ordinals)))))
      (is (= live-sha (file-sha live-path))))))

(deftest four-loaders-fresh-jvm
  (when (.isFile (io/file live-path))
    (let [v (standing/standing-verifier)]
      (testing "authority-bearing loads succeed with the same shared verifier"
        (is (= :loaded (:status (store/load-durable-ledger live-path v))))
        (is (= :loaded (:status (store/load-ledger live-path v))))
        (is (= 129 (count (:entries (:ledger (store/load-durable-ledger live-path v)))))))
      (testing "default and deny loaders still refuse typed"
        (doseq [loader [#(store/load-durable-ledger live-path)
                        #(store/load-durable-ledger live-path (fn [_] false))
                        #(store/load-ledger live-path)
                        #(store/load-ledger live-path (fn [_] false))]]
          (let [r (loader)]
            (is (= :refused (:status r)))
            (is (= [:entry/transition-invalid] (:defects r)))))))
    (is (= live-sha (file-sha live-path)))))

(defn- hermetic-root [policy-map evidence-files]
  (let [dir (java.nio.file.Files/createTempDirectory
             "standing-auth-test" (make-array java.nio.file.attribute.FileAttribute 0))
        root (.toString dir)]
    (spit (str root "/policy.edn") (pr-str policy-map))
    (.mkdirs (io/file (str root "/evidence")))
    (doseq [[name bytes] evidence-files]
      (spit (str root "/evidence/" name) bytes))
    root))

(defn- with-root [root f]
  (with-redefs [clojure.java.io/resource
                (fn [rel]
                  (let [f (io/file (str root "/"
                                        (str/replace rel #"^wm/standing-authority/" "")))]
                    (when (.isFile f) (.toURL ^java.io.File f))))]
    (standing/reset-context!)
    (f)
    (standing/reset-context!)))

(defn- production-policy []
  (edn/read-string (slurp (io/resource "wm/standing-authority/policy.edn"))))

(defn- production-evidence []
  (into {}
        (keep (fn [^java.io.File f]
                (when (.isFile f) [(.getName f) (slurp f)])))
        (file-seq (io/file (.getFile (io/resource "wm/standing-authority/evidence"))))))

(deftest typed-failures-without-writes
  (let [policy (production-policy)
        evidence (production-evidence)
        live-sha-before (file-sha live-path)]
    (testing "missing :bundles refuses typed"
      (with-root (hermetic-root {:schema (:schema policy)} {})
        #(is (= :policy/malformed (:reason (standing/standing-context))))))
    (testing "corrupt evidence refuses typed :evidence/hash-drift"
      (let [bad-sha (-> policy :bundles first :evidence first :sha256)
            root (hermetic-root policy (update evidence bad-sha str " "))]
        (with-root root
          #(let [ctx (standing/standing-context)]
             (is (= :refused (:status ctx)))
             (is (= :evidence/hash-drift (:reason ctx))
                 (str "got " (pr-str ctx)))))))
    (testing "missing evidence refuses typed :evidence/missing"
      (let [bad-sha (-> policy :bundles first :evidence first :sha256)
            root (hermetic-root policy (dissoc evidence bad-sha))]
        (with-root root
          #(is (= :evidence/missing (:reason (standing/standing-context)))))))
    (testing "overlapping scopes refuse typed as ambiguous"
      (let [root (hermetic-root (update policy :bundles #(conj % (first %))) evidence)]
        (with-root root
          #(is (= :meta-self-heal/medic-authority-ambiguous
                  (:reason (standing/standing-context)))))))
    (is (= live-sha-before (file-sha live-path)) "no writes anywhere")))

(deftest unlisted-and-doctored-scopes-refuse
  (when-let [l (live-ledger)]
    (let [v (standing/standing-verifier)
          live-sha-before (file-sha live-path)]
      (testing "an unlisted future observation (unknown receipt pin) refuses"
        (let [doctored (with-entry l 38 #(assoc-in %
                                                   [:medic-operation :authority-receipt-pin :sha256]
                                                   (apply str (repeat 64 "0"))))
              defects (get (walk-defects doctored v #{38}) 38)]
          (is (contains? (set defects) :observation/medic-admission) (pr-str defects))))
      (testing "wrong-injury candidate refuses"
        (let [doctored (with-entry l 40 #(assoc-in %
                                                   [:medic-operation :request :candidate :injury-id]
                                                   "injury/0000000000000000000000000000000000000000"))]
          (is (contains? (set (get (walk-defects doctored v #{40}) 40))
                         :observation/medic-admission))))
      (testing "swapped operation-id refuses"
        (let [doctored (with-entry l 41 #(assoc-in % [:medic-operation :operation-id]
                                                   (apply str (repeat 64 "1"))))]
          (is (contains? (set (get (walk-defects doctored v #{41}) 41))
                         :observation/medic-admission))))
      (is (= live-sha-before (file-sha live-path)) "no writes"))))
