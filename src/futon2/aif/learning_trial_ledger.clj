(ns futon2.aif.learning-trial-ledger
  "Append-only record-only counts. Read solely for deduplication/meaning admission,
   never by a production model. OS lock serializes independent writers."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.load-identity :as load-identity])
  (:import [java.io RandomAccessFile PushbackReader StringReader]))

(load-identity/register! *ns* *file*)
;; Absolute, like the cohort root: the serving JVM runs with futon3c as cwd.
(def default-root "/home/joe/code/futon2/data/wm-learning-trials")
(defonce ^:private mutex (Object.))

(defn- records [text]
  (with-open [reader (PushbackReader. (StringReader. text))]
    (loop [out []]
      (let [x (edn/read {:eof ::eof} reader)]
        (if (= ::eof x) out
            (if (= :wm/attempt-learning-count-v1 (:schema x))
              (recur (conj out x))
              (throw (ex-info "Learning ledger schema invalid" {:learning-ledger/refusal :invalid-record}))))))))

(defn record!
  "Append each newly admitted occurrence/effect once. Held rows never create a
   ledger file. A partial/corrupt ledger refuses; it is never repaired here."
  [root receipt]
  (if-not (some #(= :admitted-at-attempt-grain (:status %)) (:trials receipt))
    receipt
    (locking mutex
      (let [file (io/file root "attempts.edn")]
        (io/make-parents file)
        (with-open [raf (RandomAccessFile. file "rw")
                    lock (.lock (.getChannel raf))]
          (let [_ (assert (.isValid lock))
                bytes (byte-array (.length raf))
                _ (.readFully raf bytes)
                existing (records (String. bytes "UTF-8"))
                by-id (atom (into {} (map (juxt :identity identity)) existing))
                meanings (atom (into {} (map (juxt :family :meaning-sha256)) existing))
                rows (mapv
                      (fn [row]
                        (if-not (= :admitted-at-attempt-grain (:status row)) row
                          (let [id (get-in row [:deduplication :identity])
                                old (get @by-id id)
                                family (:learning-family row)
                                reason (cond
                                         (and (contains? @meanings family)
                                              (not= (get @meanings family) (:meaning-sha256 row))) :revised-meaning
                                         (and old (not= (:observed old) (:after-observation row))) :conflicting-observation
                                         old :duplicate-replay)]
                            (if reason
                              (assoc row :status :held :reason reason :counted? false
                                     :ledger {:path (.getPath file) :identity id :status :not-appended})
                              (let [event {:schema :wm/attempt-learning-count-v1 :mode :record-only
                                           :identity id :family family :meaning-sha256 (:meaning-sha256 row)
                                           :observed (:after-observation row)
                                           :increment (if (:after-observation row) {:success 1 :failure 0} {:success 0 :failure 1})
                                           :contract (:contract receipt) :trial row}
                                    line (str (identity/printed false event) "\n")]
                                (.seek raf (.length raf))
                                (.write raf (.getBytes line "UTF-8"))
                                (.sync (.getFD raf))
                                (swap! by-id assoc id event)
                                (swap! meanings assoc family (:meaning-sha256 row))
                                (assoc row :counted? true
                                       :ledger {:path (.getPath file) :identity id :status :appended}))))))
                      (:trials receipt))]
            (assoc receipt :trials rows)))))))
