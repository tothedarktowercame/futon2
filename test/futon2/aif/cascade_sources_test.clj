(ns futon2.aif.cascade-sources-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.cascade-sources :as cs]))

(defn- byte-sha [file]
  (format "%064x" (java.math.BigInteger.
                    1 (.digest (java.security.MessageDigest/getInstance "SHA-256")
                               (java.nio.file.Files/readAllBytes (.toPath (io/file file)))))))

(defn- tmp-dir []
  (doto (.toFile (java.nio.file.Files/createTempDirectory "cascade-sources" (make-array java.nio.file.attribute.FileAttribute 0)))
    (.deleteOnExit)))

(def source
  {:schema :wm/cascade-source-v1
   :target "M-fixture"
   :context :WM
   :beta {:value 1 :status :declared}
   :facts [:present-file :absent-file :unlocatable]
   :want [:goal]
   :locators {:present-file {:class :C3 :repo "futon2" :sha "b81afd97" :path "src/futon2/aif/construction.clj"}
              :absent-file {:class :C3 :repo "futon2" :sha "b81afd97" :path "src/futon2/aif/no_such.clj"}
              :unlocatable {:class :C3 :repo "futon2" :sha "0000000000000000000000000000000000000000" :path "missing"}
              :goal {:class :C3 :repo "futon2" :sha "b81afd97" :path "goal"}}
   :patterns {:p/act {:guard {:needs #{:present-file} :forbids #{}} :produces #{:goal}}}
   :interpretation-receipts {:p/act {:kind :hand-admitted}}
   :candidates [{:precedence [:p/act] :construction-receipt {:kind :hand-admitted}}]})

(deftest facts-are-observed-not-declared
  (let [dir (tmp-dir)]
    (spit (io/file dir "m.edn") (pr-str source))
    (let [s (cs/with-context-fn (cs/load-declared (.getPath dir)))]
      (is (= {:present-file true :absent-file false :unlocatable :unknown}
             (get-in s [:universes "M-fixture"])))
      (is (= 1 (count (:files s))))
      (is (= {:beta 1 :status :declared} (get-in s [:beta-by-context :WM])))
      (let [a (cp/assemble {:targets ["M-fixture"] :sources (assoc s :horizon-steps 2)})]
        (is (= 1 (count (:problems a))) (pr-str (:refusals a)))))))

(deftest default-dir-resolves-to-the-declared-sources
  ;; the tick runs in the serving JVM, whose working directory is futon3c, so
  ;; a working-directory-relative default found nothing and loaded no sources
  ;; at all — and "no sources" is a legitimate state, so nothing complained
  (let [dir (io/file cs/default-dir)]
    (is (.isDirectory dir) (str "default-dir does not resolve: " cs/default-dir))
    (is (seq (filter #(.endsWith (.getName ^java.io.File %) ".edn") (.listFiles dir)))
        "no declared cascade source is where the tick looks for it")))

(deftest empty-or-missing-dir-is-nil-and-malformed-throws
  (is (nil? (cs/load-declared "/nonexistent/cascade-sources")))
  (let [dir (tmp-dir)]
    (spit (io/file dir "bad.edn") (pr-str (dissoc source :beta)))
    (is (thrown? clojure.lang.ExceptionInfo (cs/load-declared (.getPath dir))))))

(def pattern-id :cascade-construction/run-it-on-a-real-case)
(def pattern-path "futon3/library/cascade-construction/run-it-on-a-real-case.flexiarg")

(defn- admission [dir receipt]
  ;; Real declaration/receipt, with no fact observations: this test exercises
  ;; document admission, not git/HTTP observation ports. No source-read stub.
  (let [decl (edn/read-string (slurp (io/resource "wm/cascade-sources/M-wm-08-external-f2.edn")))
        decl (assoc decl :facts [] :interpretation-receipts {pattern-id receipt})]
    (spit (io/file dir "admission.edn") (pr-str decl))
    (get-in (cs/load-declared (str dir))
            [:interpretations (:target decl) :receipts pattern-id])))

(defn- refusal [f]
  (try (f) nil
       (catch clojure.lang.ExceptionInfo e (ex-data e))))

(deftest path-only-real-pattern-admission-gets-current-byte-hash
  (let [dir (tmp-dir)
        decl (edn/read-string (slurp (io/resource "wm/cascade-sources/M-wm-08-external-f2.edn")))
        original (get-in decl [:interpretation-receipts pattern-id])
        expected (byte-sha (io/file "/home/joe/code" pattern-path))]
    (try
      (is (= {:path pattern-path} (:source original)) "The actual defective input.")
      (let [receipt (admission dir original)]
        (is (= expected (get-in receipt [:source :sha256])))
        (is (= original (update receipt :source dissoc :sha256)))
        (println "PATTERN-SOURCE-HASH-RECEIPT" (pr-str {:source (:source receipt)
                                                       :independent-sha256 expected
                                                       :scope :offline-admission})))
      (finally (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))

(deftest missing-and-unreadable-sources-refuse-instead-of-emitting-unhashed-receipts
  (let [dir (tmp-dir)
        unreadable (doto (io/file dir "not-a-readable-file") .mkdirs)]
    (try
      (doseq [path [(str (io/file dir "missing.flexiarg")) (str unreadable)]]
        (let [r (refusal #(admission dir {:kind :hand-admitted :source {:path path}}))]
          (is (= :invalid-cascade-source (:error r)))
          (is (= :interpretation-source-unreadable (:reason r)))
          (is (= path (:path r)))))
      (finally (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))

(deftest hash-is-read-fresh-and-declared-pins-are-not-silently-replaced
  (let [dir (tmp-dir)
        file (io/file dir "pattern.flexiarg")
        receipt {:kind :hand-admitted :source {:path (str file)}}]
    (try
      (spit file "first\r\n")
      (let [first-receipt (admission dir receipt)]
        (is (= (byte-sha file) (get-in first-receipt [:source :sha256])))
        (is (= first-receipt (admission dir first-receipt)))
        ;; Invalid UTF-8 proves the digest is over bytes, not decoded text.
        (with-open [out (io/output-stream file)] (.write out (byte-array [(unchecked-byte 255) 0])))
        (let [second-receipt (admission dir receipt)]
          (is (= (byte-sha file) (get-in second-receipt [:source :sha256])))
          (is (not= (:source first-receipt) (:source second-receipt))))
        (is (= :interpretation-source-hash-mismatch
               (:reason (refusal #(admission dir first-receipt))))))
      (finally (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))

(deftest observation-clock-is-declared-and-independent-of-preferences
  (let [dir (tmp-dir)
        bare (assoc source :facts [] :locators {})
        f (io/file dir "clock.edn")
        load! (fn [d] (spit f (pr-str d)) (cs/load-declared (str dir)))]
    (try
      (is (= {:status :held :reason :observation-placement-not-declared}
             (get-in (load! bare) [:observation-schedules "M-fixture"])))
      (doseq [bad [nil 7 {} {:tau :not-a-step}
                   {:tau {:value -1 :status :declared}}
                   {:tau {:value 1.5 :status :declared}}
                   {:tau {:value 1 :status :defaulted}}]]
        (is (= :invalid-observation-schedule
               (:reason (refusal #(load! (assoc bare :observation-schedule bad)))))))
      (let [clock {:tau {:value 1 :status :declared}}
            d (assoc bare :observation-schedule clock)
            c {:placement {:value :terminal :status :declared}
               :elsewhere {:value :uniform-over-non-ruled-zero :status :declared}}]
        (is (= clock (get-in (load! d) [:observation-schedules "M-fixture"])))
        (is (= clock (get-in (load! (assoc d :c-schedule c))
                            [:observation-schedules "M-fixture"]))))
      (finally (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))

(deftest initialized-context-rates-cannot-overwrite-each-other
  (let [dir (tmp-dir) bare (assoc source :facts [] :locators {})]
    (try
      (spit (io/file dir "one.edn") (pr-str bare))
      (spit (io/file dir "two.edn")
            (pr-str (assoc bare :target "M-other" :beta {:value 2 :status :declared})))
      (is (= :incommensurable-family
             (:reason (refusal #(cs/load-declared (str dir))))))
      (finally (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))

;; PROOF-wm-works 1.3 build 2/3 (2026-09-22): lift source-declared
;; :horizon-steps into the merged sources map (max over declarers; absent
;; when none declare; shape error when non-positive-integer).
(deftest horizon-steps-lift-takes-the-max-and-names-the-declarers
  (let [dir (tmp-dir)]
    (spit (io/file dir "a.edn") (pr-str (assoc source :target "M-a" :horizon-steps 2)))
    (spit (io/file dir "b.edn") (pr-str (assoc source :target "M-b" :horizon-steps 4)))
    (let [s (cs/load-declared (.getPath dir))]
      (is (= 4 (:horizon-steps s)) "the horizon covers the longest declared episode")
      (is (= #{{:source "a.edn" :horizon-steps 2} {:source "b.edn" :horizon-steps 4}}
             (set (:horizon-steps-declarations s)))
          "both declarers are recorded by file name with their values"))))

(deftest horizon-steps-absent-when-no-source-declares
  (let [dir (tmp-dir)]
    (spit (io/file dir "m.edn") (pr-str source))
    (let [s (cs/load-declared (.getPath dir))]
      (is (not (contains? s :horizon-steps)))
      (is (nil? (:horizon-steps-declarations s))
          "the T=2 default path reads :horizon-steps' absence, untouched"))))

(deftest non-positive-integer-horizon-steps-refuses
  (doseq [bad [0 -1 "4" 2.5]]
    (let [dir (tmp-dir)]
      (spit (io/file dir "bad-horizon.edn") (pr-str (assoc source :horizon-steps bad)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"invalid-horizon-steps"
                            (cs/load-declared (.getPath dir)))
          (str "value " (pr-str bad) " must refuse")))))

;; claude-5, 2026-09-23. Every declared interpretation source is pinned by
;; sha256, and load-declared refuses the WHOLE set on the first mismatch —
;; so one prose edit to one flexiarg in futon3 stops every WM dispatch with
;; :interpretation-source-hash-mismatch, and the first sign of it is a click
;; that cannot render an acceptance criterion. That happened: futon3 21a9199
;; ("Repair @why and @how after the 2026-09-05 rationale-backfill lanes")
;; moved five pinned flexiargs at 17:17, and the machine went from green to
;; refusing in under an hour. This test names the drifted path instead, so
;; the next library edit fails HERE rather than inside a spent click.
;;
;; Scope: the :source pins only — the ones read-receipt-source actually
;; enforces. :target-source pins (mission files in futon2) are not checked by
;; the loader and several are already stale; asserting on them would make
;; this fail for a condition that stops nothing, which is how a check gets
;; ignored.
;;
;; A mismatch is NOT automatically re-pinned: read the source diff first. If
;; only rationale metadata moved (@why/@how and the like, which no :reading
;; quotes and no locator observes), re-pin and say so. If the pattern's
;; conclusion, violation signature or declared effect moved, the reading
;; itself has to be re-derived — the pin is what makes that distinction
;; possible, and re-pinning past it would erase it.
(deftest every-pinned-interpretation-source-still-matches-its-file
  (let [repo-root (io/file "/home/joe/code")
        pins (for [f (file-seq (io/file "resources/wm/cascade-sources"))
                   :when (.endsWith (.getName f) ".edn")
                   :let [d (edn/read-string (slurp f))]
                   [pattern receipt] (:interpretation-receipts d)
                   :let [source (:source receipt)]
                   :when (and (map? source) (string? (:path source)) (string? (:sha256 source)))]
               (let [p (:path source)
                     file (first (filter #(.exists %) [(io/file p) (io/file repo-root p)]))]
                 {:declaration (.getName f) :pattern pattern :path p
                  :status (cond (nil? file) :file-missing
                                (= (:sha256 source) (byte-sha file)) :matches
                                :else :drifted)}))]
    (is (seq pins) "the declarations carry :source pins at all")
    (is (= [] (vec (remove #(= :matches (:status %)) pins)))
        (str "a pinned interpretation source moved; load-declared will refuse "
             "every cascade until this is read and re-pinned or reverted"))))
