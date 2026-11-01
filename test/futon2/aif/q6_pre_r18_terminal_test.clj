(ns futon2.aif.q6-pre-r18-terminal-test
  "L5-6: terminals sealed before R18 verify under the printer that sealed them, so a
  good seal is not read as a broken one, and a broken one still is."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.previous-run :as previous-run]))

;; Verbatim run-stream lines: three terminals sealed before R18 (refused, refused,
;; already-satisfied) and one after it.
(def fixture "test/fixtures/wm-run-facts/pre-r18-run-stream.edn")
(def now (java.time.Instant/parse "2026-11-02T00:00:00Z"))

(defn- dir-holding [text]
  (let [dir (str (java.nio.file.Files/createTempDirectory
                  "q6-r18-" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (spit (io/file dir previous-run/stream-name) text)
    dir))

(defn- history [text]
  (let [{:keys [status events]} (previous-run/read-stream (dir-holding text))]
    (is (= :present status))
    (previous-run/refusal-history events "probe" now)))

(deftest the-real-pre-r18-seals-verify
  (let [h (history (slurp fixture))]
    (is (= 0 (:unsealed h)))
    (is (= 3 (:sealed-before-r18 h)))
    (is (= [] (:entries h)) "these two refusals carry no input digest, so they forbid nothing")))

(deftest the-current-digest-does-not-verify-them
  ;; the bad case this exists for: under value-digest alone they read as broken
  (let [closed (first (filter #(= :closed (:event %))
                              (:events (previous-run/read-stream (dir-holding (slurp fixture))))))]
    (is (not (previous-run/intact? (:q6 closed))))
    (is (= (:sha256 (:q6 closed))
           (evidence/value-digest-before-r18 (dissoc (:q6 closed) :sha256))))))

(deftest a-rewritten-pre-r18-terminal-is-tampered
  (let [h (history (str/replace (slurp fixture) ":outcome :refused" ":outcome :changed"))]
    (is (pos? (:unsealed h)))
    (is (= 1 (:sealed-before-r18 h)) "only the untouched one still verifies")
    (is (= [] (:entries h)))))

(deftest a-dropped-seal-is-tampered
  (let [text (str/replace-first (slurp fixture) #":sha256 \"[0-9a-f]+\"\}, :event|:sha256 \"[0-9a-f]+\"" ":sha257 \"x\"")
        h (history text)]
    (is (pos? (:unsealed h)))))

(deftest the-old-printer-buys-nothing-after-r18
  (let [at "2026-10-27T10:00:00Z"
        body {:schema previous-run/terminal-schema :run/id "late" :startedAt at :input-digest "d"
              :outcome {:status :present :outcome :refused}
              :choice {:status :present :policy-key [:k #:a{:b 1}] :target "M"}}
        sealed (assoc body :sha256 (evidence/value-digest-before-r18 body))
        h (history (str (pr-str {:event :started :run/id "late" :startedAt at}) "\n"
                        (pr-str {:event :closed :run/id "late" :record-file "x" :q6 sealed}) "\n"))]
    (is (= [1 0] ((juxt :unsealed :sealed-before-r18) h)))))
