(ns futon2.aif.target-field-test
  "Clause T step 1: the target field over a temporary code root laid out
  like ~/code (primary checkouts with a .git directory). Texts are read from
  disk, observations are stubbed, and the interpretation store is a temp
  directory, so the test reads nothing live and writes nothing shared."
  (:require [clojure.edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.mission-criteria :as mc]
            [futon2.aif.mission-registry :as mr]
            [futon2.aif.target-field :as tf])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def roots (atom []))
(use-fixtures :each
  (fn [f] (try (f) (finally
                     (doseq [root @roots file (reverse (file-seq root))] (Files/delete (.toPath file)))
                     (reset! roots [])))))

(defn- tmp [prefix]
  (let [d (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0)))]
    (swap! roots conj d) (.getCanonicalPath d)))

(defn- put! [root rel text]
  (let [f (io/file root rel)] (io/make-parents f) (spit f text)))

(def autoclock (slurp "test/fixtures/target-field/M-autoclock-in@futon3c-7466251c.md"))
(def f11 (slurp "test/fixtures/mission-criteria/M-f11-find-production-successor@futon2-22fa0da9.md"))

(def shaped
  (str/join "\n" ["# Mission: M-shaped" "" "**Status:** MAP (2026-09-25)" ""
                  "## IDENTIFY" "" "**Exit criterion:** the gap is named. **Met.**" ""
                  "## MAP" "" "**Exit criterion:** the survey lists every caller. **Not met.**" ""]))

(defn- want-of [id text phase]
  (:token (first (filter #(str/starts-with? (str (:phase %)) phase) (mc/criteria id text)))))

(defn- layout
  "A code root with futon3c (M-autoclock-in, M-shaped, a criteria-less
  ticket), futon2 (M-f11) and a store publishing M-shaped's MAP producer."
  []
  (let [root (tmp "tf-code") store (tmp "tf-store")
        map-want (want-of "M-shaped" shaped "MAP")]
    (doseq [r ["futon3c" "futon2"]] (.mkdirs (io/file root r ".git")))
    (put! root "futon3c/holes/missions/M-autoclock-in.md" autoclock)
    (put! root "futon3c/holes/missions/M-shaped.md" shaped)
    (put! root "futon3c/holes/tickets/T-plain.md" "# T-plain\n\n**Status:** OPEN\n\nSome prose, no criteria.\n")
    (put! root "futon3c/holes/excursions/E-plain.md" "# E-plain\n\nStatus: OPEN\n")
    (put! root "futon3c/holes/excursions/E-done.md" "# E-done\n\nStatus: COMPLETE (2026-09-01)\n")
    (put! root "futon2/holes/missions/M-f11-find-production-successor.md" f11)
    (spit (io/file store "M-shaped.edn")
          (pr-str {:schema :wm/machine-interpretations-v1 :target "M-shaped"
                   :patterns {:survey/list-callers {:guard {:needs #{} :forbids #{}} :produces #{map-want}}}
                   :receipts {:survey/list-callers {:request-id "request-test" :by :test}}}))
    {:root root :store store :map-want map-want}))

(defn- field [{:keys [root store]}]
  (let [loaded {:missions (:missions (mr/load-missions-from-files root))
                :tickets (:tickets (mr/load-tickets root))
                :excursions (:excursions (mr/load-excursions root))}]
    (tf/target-field {:code-root root :store store :sources {}
                      :read-text (fn [code-root repo path] (let [f (io/file code-root repo path)] (when (.isFile f) (slurp f))))
                      ;; a C4 met-decl is observed true exactly when the text carries it
                      :observe (fn [l] (str/includes? (slurp (io/file root (:repo l) (:path l))) (str (:decl l))))}
                     loaded)))

(defn- by-target [xs] (into {} (map (juxt :target identity)) xs))

(deftest the-field-partitions-what-the-enumerators-propose
  (let [l (layout) fld (field l)
        ex (by-target (:exclusions fld))
        ok (by-target (:feasible fld))]
    (is (= #{"M-autoclock-in" "M-shaped" "M-f11-find-production-successor" "T-plain" "E-plain"}
           (set (map :target (:considered fld))))
        "a complete excursion is not proposed")
    (is (= {"E-plain" :excursion "T-plain" :ticket "M-shaped" :mission}
           (select-keys (into {} (map (juxt :target :kind)) (:considered fld)) ["E-plain" "T-plain" "M-shaped"])))
    (is (= [] (tf/check-field fld)))
    (is (not (contains? fld :chosen)) "step 1 chooses nothing")
    (testing "a lifecycle-shaped mission with a published producer for its open exit is feasible"
      (is (= #{"M-shaped"} (set (keys ok))))
      (is (= [(:map-want l)] (get-in ok ["M-shaped" :open-wants])))
      (is (= 1 (get-in ok ["M-shaped" :support]))))
    (testing "M-f11 is excluded as not lifecycle-shaped"
      (is (= :not-lifecycle-shaped (get-in ex ["M-f11-find-production-successor" :reason])))
      (is (= [:phase-exits :verdict-lines]
             (get-in ex ["M-f11-find-production-successor" :what-would-make-feasible :lifecycle-parts-missing]))))
    (testing "M-autoclock-in is considered, its status read"
      (let [c (first (filter #(= "M-autoclock-in" (:target %)) (:considered fld)))]
        (is (str/starts-with? (:status-line c) "INSTANTIATE-1 (first implementation, 2026-06-03)"))
        (is (= :unknown (:status-class c)))
        (is (= :not-lifecycle-shaped (get-in ex ["M-autoclock-in" :reason])))
        (is (= {:status-line true :phase-exits 0 :verdict-lines 0 :phase-headings 15}
               (select-keys (get-in ex ["M-autoclock-in" :details :shape])
                            [:status-line :phase-exits :verdict-lines :phase-headings]))
            "phase headings but no exit criterion in the reader's form")))
    (testing "a ticket with no criteria waits on the read step"
      (is (= :needs-reading (get-in ex ["T-plain" :reason]))))))

(deftest a-missing-producer-is-needs-interpretation-naming-the-want
  ;; the only published pattern produces the IDENTIFY exit, already met, so
  ;; the constructor finds no order producing a new want and names the MAP
  ;; exit as the unproduced need
  (let [l (layout)]
    (spit (io/file (:store l) "M-shaped.edn")
          (pr-str {:schema :wm/machine-interpretations-v1 :target "M-shaped"
                   :patterns {:survey/other {:guard {:needs #{} :forbids #{}}
                                             :produces #{(want-of "M-shaped" shaped "IDENTIFY")}}}
                   :receipts {:survey/other {:request-id "request-test" :by :test}}}))
    (let [ex (by-target (:exclusions (field l)))
          e (get ex "M-shaped")]
      (is (= :needs-interpretation (:reason e)) (pr-str e))
      (is (= :no-supported-order (get-in e [:details :constructor-finding])))
      (is (= [{:want (:map-want l) :line 11
               :criterion "**Exit criterion:** the survey lists every caller."}]
             (get-in e [:what-would-make-feasible :interpretations-for]))))))

(deftest x-t-a-a-feasible-target-removed-fails-the-check
  (let [fld (field (layout))
        dropped (update fld :feasible (fn [fs] (vec (remove #(= "M-shaped" (:target %)) fs))))]
    (is (= [] (tf/check-field fld)))
    (is (= [{:failure :considered-not-partitioned :targets ["M-shaped"]}] (tf/check-field dropped)))))

(deftest x-t-c-an-exclusion-without-what-would-make-feasible-fails-the-check
  (let [fld (field (layout))
        bare (update fld :exclusions (fn [xs] (mapv #(if (= "T-plain" (:target %)) (dissoc % :what-would-make-feasible) %) xs)))]
    (is (= [{:failure :exclusion-without-what-would-make-feasible :targets ["T-plain"]}]
           (tf/check-field bare)))
    (is (seq (tf/check-field (assoc fld :chosen "M-shaped"))) "a step-1 field names no choice")))

(deftest live-field-futon2-7a5f6c0b
  ;; the one live read (futon2 7a5f6c0b, futon3c 7466251c; heads recorded in
  ;; the fixture): 495 considered, none feasible. Every live mission fails the
  ;; stated lifecycle test; the only mission that meets it, M-futon-seams, is
  ;; COMPLETE and so not proposed.
  (let [r (clojure.edn/read-string (slurp "test/fixtures/target-field/target-field@futon2-7a5f6c0b.edn"))
        f (get-in r [:decision :target-field])
        ex (by-target (:exclusions f))]
    (is (= [] (tf/check-field f)))
    (is (= {:considered 495 :feasible 0 :excluded 495
            :considered-by-kind {:mission 217 :ticket 30 :excursion 248}
            :excluded-by-reason {:needs-interpretation 4 :needs-reading 274 :not-lifecycle-shaped 217}}
           (tf/counts f)))
    (is (= :not-lifecycle-shaped (get-in ex ["M-f11-find-production-successor" :reason])))
    (is (= "futon3c" (:repo (first (filter #(= "M-autoclock-in" (:target %)) (:considered f))))))
    (is (= 15 (get-in ex ["M-autoclock-in" :details :shape :phase-headings])))
    (is (not-any? #(= "M-futon-seams" (:target %)) (:considered f)))))
