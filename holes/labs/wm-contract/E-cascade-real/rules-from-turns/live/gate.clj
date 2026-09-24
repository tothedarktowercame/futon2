(ns gate
  "P2 gate (kimi-2, live set): structural validation of the turn-rules files.

  Checks, over every *.edn in this directory except NOTES.md:
  1. the file reads as one EDN form with :schema :e-cascade-real/turn-rules-v1;
  2. every rule's :pattern resolves to futon3/library/<id>.flexiarg, its
     first line is @flexiarg <id>, and the receipt's :sha256 matches the
     file's current bytes;
  3. every token cue span is within the source record's source_text and
     slices a non-blank string;
  4. every token used in a guard or :produces is defined in :tokens;
  5. no rule has empty :produces.

  Run from /home/joe/code/futon2:  clojure -M holes/labs/wm-contract/E-cascade-real/rules-from-turns/live/gate.clj"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.data.json :as json])
  (:import [java.nio.file Files]
           [java.security MessageDigest]))

(def dir "/home/joe/code/futon2/holes/labs/wm-contract/E-cascade-real/rules-from-turns/live")
(def lib "/home/joe/code/futon3/library/")

(defn sha256 [f]
  (let [bytes (Files/readAllBytes (.toPath (io/file f)))]
    (apply str (map #(format "%02x" %)
                    (.digest (MessageDigest/getInstance "SHA-256") bytes)))))

(def failures (atom []))
(defn fail [msg] (swap! failures conj msg))

(defn check-file [f]
  (let [packet (try (edn/read-string (slurp f))
                    (catch Throwable e
                      (fail (str (.getName f) ": unreadable EDN: " (.getMessage e)))
                      nil))]
    (when packet
      (when-not (= :e-cascade-real/turn-rules-v1 (:schema packet))
        (fail (str (.getName f) ": wrong schema " (:schema packet))))
      (let [src-text (try (:source_text (json/read-str (slurp (:source-record packet))))
                          (catch Throwable e
                            (fail (str (.getName f) ": source record unreadable: " (.getMessage e)))
                            nil))
            tokens (:tokens packet)]
        (when src-text
          (doseq [[tok {:keys [cue]}] tokens]
            (let [[s e] cue]
              (when-not (and (integer? s) (integer? e) (<= 0 s) (< s e) (<= e (count src-text)))
                (fail (str (.getName f) " " tok ": cue span " cue " out of range")))
              (when (and (integer? s) (integer? e) (<= 0 s) (<= e (count src-text)) (< s e)
                         (str/blank? (subs src-text s e)))
                (fail (str (.getName f) " " tok ": cue span " cue " slices blank text"))))))
        (doseq [rule (:rules packet)]
          (let [pid (:pattern rule)
                pf (io/file lib (str pid ".flexiarg"))]
            (if-not (.exists pf)
              (fail (str (.getName f) " " pid ": pattern file missing"))
              (do
                (let [first-line (str/trim (first (str/split-lines (slurp pf))))]
                  (when-not (and (str/starts-with? first-line "@flexiarg")
                                 (str/includes? first-line pid))
                    (fail (str (.getName f) " " pid ": @flexiarg line mismatch: " first-line))))
                (when-not (= (get-in rule [:receipt :source :sha256]) (sha256 pf))
                  (fail (str (.getName f) " " pid ": receipt sha256 does not match file")))))
            (doseq [tok (concat (get-in rule [:guard :needs])
                                (get-in rule [:guard :forbids])
                                (:produces rule))]
              (when-not (contains? tokens tok)
                (fail (str (.getName f) " " pid ": undefined token " tok))))
            (when (empty? (:produces rule))
              (fail (str (.getName f) " " pid ": empty :produces")))))))))

(let [files (->> (file-seq (io/file dir))
                 (filter #(and (.isFile %) (str/ends-with? (.getName %) ".edn")))
                 (sort-by #(.getName %)))]
  (doseq [f files] (check-file f))
  (println (str "gate: " (count files) " EDN files checked"))
  (if (seq @failures)
    (do (println (str "gate: " (count @failures) " FAILURES"))
        (doseq [m @failures] (println " " m))
        (System/exit 1))
    (println "gate: OK — all checks pass")))
