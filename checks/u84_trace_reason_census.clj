(ns checks.u84-trace-reason-census
  "Read-only census of persisted TRACE-hop reasons, not producer input keys.
  Run from futon2: bb -cp . -m checks.u84-trace-reason-census [trace-directory].
  A threshold is reported, never treated as evidence of live origin or review."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn count-record [record]
  (when-not (map? record)
    (throw (ex-info "Trace record must be a map" {:reason :non-map-record})))
  (let [hops (filter #(= :TRACE (:node %)) (:wm/route record))
        reasons (keep :reason hops)
        valid? #(and (map? %) (keyword? (:kind %)) (keyword? (:rule %))
                     (string? (:question %)) (not (str/blank? (:question %))))
        valid (filter valid? reasons)]
    {:records 1
     :trace-records (if (seq hops) 1 0)
     :reason-bearing-records (if (seq valid) 1 0)
     :producer-reason-records
     (if (some #(not= :trace-route-reason-missing (:rule %)) valid) 1 0)
     :fallback-reason-records
     (if (some #(= :trace-route-reason-missing (:rule %)) valid) 1 0)
     :malformed-reason-records
     (if (some #(not (valid? %)) reasons) 1 0)}))

(def zero-counts (zipmap (keys (count-record {})) (repeat 0)))

(defn count-text [text]
  (with-open [r (java.io.PushbackReader. (java.io.StringReader. text))]
    (loop [counts zero-counts]
      ;; Retain opaque historical #object tags as data; never evaluate them.
      (let [record (edn/read {:eof ::eof :default tagged-literal} r)]
        (if (= ::eof record) counts
            (recur (merge-with + counts (count-record record))))))))

(defn- sha256 [text]
  (format "%064x" (java.math.BigInteger.
                    1 (.digest (java.security.MessageDigest/getInstance "SHA-256")
                               (.getBytes text "UTF-8")))))

(defn census [directory]
  (when-not (.isDirectory (io/file directory))
    (throw (ex-info "Trace directory absent" {:directory directory})))
  (let [files (sort-by str
                      (filter #(and (.isFile %)
                                    (re-matches #"wm-trace-.*\.edn" (.getName %)))
                              (file-seq (io/file directory))))
        rows (mapv (fn [file]
                     (let [text (slurp file :encoding "UTF-8")
                           hash (sha256 text)]
                       (try
                         (let [counts (count-text text)]
                           (when-not (= hash (sha256 (slurp file :encoding "UTF-8")))
                             (throw (ex-info "Source moved during census" {})))
                           {:path (str file) :sha256 hash :counts counts})
                         (catch Exception e
                           {:path (str file) :sha256 hash :error (.getMessage e)})))) files)
        complete? (and (seq files) (not-any? :error rows))
        totals (reduce #(merge-with + %1 %2) zero-counts (keep :counts rows))]
    {:schema :wm/u84-trace-reason-census-v1
     :scope :retained-files-not-authenticated-live-origin
     :complete? (boolean complete?) :files rows :totals totals
     :threshold 20
     :threshold-met? (boolean (and complete? (>= (:reason-bearing-records totals) 20)))
     :qualification-revised? false}))

(defn -main [& [directory]]
  (let [result (census (or directory "data/wm-trace"))]
    (prn result)
    (when-not (:complete? result) (System/exit 2))))
