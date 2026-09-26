(ns futon2.aif.enactment-fold-source
  "E for the tick's selection, folded from the flights' increment receipts
  (WM-HABIT-FOLD-CALL-I; WM-HABIT-FOLD-WIRE-D, futon2 655bbced).

  The flight's W_c call puts one enactment-habit/increment receipt on its
  flight record, under [:flight :enactments i :increment]; the driver writes
  the record to <machine-interpretations-dir>/flights/<flight-id>.edn. Those
  receipts are the only authority for E, so the tick re-folds all of them at
  each selection (the fold counts a [click candidate] once, so re-folding
  is idempotent) rather than keeping a folded copy.

  This namespace reads the files; enactment-habit stays pure. What it read
  rides on the folded state as :folded-from, which selection's habit-read
  receipt keeps with the :state it consumed (cascade-habit-store/
  attach-state): the directory, each record read with its sha256 and the
  number of receipts on it, and each file that could not be read, with why.
  An unreadable file is noted there, never skipped silently, and never a
  refusal."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.enactment-habit :as enactment-habit]
            [futon2.aif.load-identity :as load-identity])
  (:import [java.security MessageDigest]))

(load-identity/register! *ns* *file*)

(defn- sha256 [^bytes bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %))
                  (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn- flight-files [dir]
  (->> (.listFiles (io/file dir))
       (filter #(and (.isFile ^java.io.File %) (.endsWith (.getName ^java.io.File %) ".edn")))
       (sort-by #(.getName ^java.io.File %))))

(defn increment-receipts
  "The increment receipts on the flight records in DIR:
  {:receipts [...] :read [{:path :sha256 :receipts n}] :unread [{:path :reason}]}."
  [dir]
  (reduce
   (fn [acc ^java.io.File f]
     (let [path (.getCanonicalPath f)
           bytes (java.nio.file.Files/readAllBytes (.toPath f))
           parsed (try {:record (edn/read-string {:default tagged-literal} (String. bytes "UTF-8"))}
                       (catch Exception e {:reason :unparseable :message (ex-message e)}))
           record (:record parsed)
           enactments (when (map? record) (get-in record [:flight :enactments]))]
       (cond
         (:reason parsed)
         (update acc :unread conj {:path path :reason :unparseable :message (:message parsed)})

         (not (map? record))
         (update acc :unread conj {:path path :reason :not-a-flight-record})

         :else
         (let [rs (vec (keep #(let [r (:increment %)] (when (map? r) r)) enactments))]
           (-> acc
               (update :receipts into rs)
               (update :read conj {:path path :sha256 (sha256 bytes) :receipts (count rs)}))))))
   {:receipts [] :read [] :unread []}
   (flight-files dir)))

(defn fold-from-flights
  "The fold of every increment receipt on the flight records in DIR, as
  enactment-habit/fold gives it, with :folded-from naming what was read. A
  missing directory folds nothing and says so."
  [dir]
  (let [present? (.isDirectory (io/file dir))
        {:keys [receipts read unread]} (if present? (increment-receipts dir)
                                           {:receipts [] :read [] :unread []})]
    (assoc (enactment-habit/fold nil receipts)
           :folded-from (cond-> {:dir (str dir) :read read :unread unread}
                          (not present?) (assoc :dir-status {:absent :no-flights-dir})))))

(defn conditioning-steps
  "F1b-admit-I: the conditioning steps (flight/conditioning-step, under
  [:flight :enactments i :step]) on the flight records in DIR, in the files'
  order, each with the record it came from:
  {:steps [{:step s :path p :sha256 h} ...] :read [...] :unread [...]} and
  :dir-status {:absent :no-flights-dir} when DIR is missing. An unreadable
  file is noted, never skipped silently."
  [dir]
  (if-not (.isDirectory (io/file dir))
    {:steps [] :read [] :unread [] :dir (str dir) :dir-status {:absent :no-flights-dir}}
    (assoc
     (reduce
      (fn [acc ^java.io.File f]
        (let [path (.getCanonicalPath f)
              bytes (java.nio.file.Files/readAllBytes (.toPath f))
              h (sha256 bytes)
              record (try (edn/read-string {:default tagged-literal} (String. bytes "UTF-8"))
                          (catch Exception e {::unparseable (ex-message e)}))]
          (cond
            (contains? record ::unparseable)
            (update acc :unread conj {:path path :reason :unparseable :message (::unparseable record)})
            (not (map? record))
            (update acc :unread conj {:path path :reason :not-a-flight-record})
            :else
            (let [steps (vec (keep (fn [enactment-entry]
                                     (let [s (:step enactment-entry)]
                                       (when (map? s) {:step s :path path :sha256 h})))
                                   (get-in record [:flight :enactments])))]
              (-> acc
                  (update :steps into steps)
                  (update :read conj {:path path :sha256 h :steps (count steps)}))))))
      {:steps [] :read [] :unread []}
      (flight-files dir))
     :dir (str dir))))
