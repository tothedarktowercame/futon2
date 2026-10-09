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
            [clojure.set :as set]
            [futon2.aif.enactment-habit :as enactment-habit]
            [futon2.aif.cascade-prior :as prior]
            [futon2.aif.flight :as flight]
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

(defn conditioning-step-from-completed-run
  "Build one admitted observation from a predecessor run's measured D-task
  outcome. Predictions from the current selection are intentionally ignored."
  [record]
  (let [chosen (get-in record [:decision :chosen])
        target (:target chosen)
        precedence (vec (:precedence chosen))
        verification (get-in record [:d-task-enactment :verification])
        present (set (or (:present verification) #{}))
        target-present (set (for [[t token] present :when (= t target)] token))
        measured-a (get-in record [:decision :measured-a])
        run-id (:run/id record)]
    (cond
      (nil? record) {:steps [] :status :absent :reason :no-predecessor-run}
      (or (nil? target) (empty? precedence))
      {:steps [] :status :absent :reason :predecessor-has-no-chosen-policy}
      (nil? measured-a)
      {:steps [] :status :absent :reason :predecessor-has-no-measured-a}
      (empty? target-present)
      {:steps [] :status :absent :reason :predecessor-has-no-present-observation}
      :else
      (let [unknown (set (or (:unknown verification) #{}))
            checked (set (for [[t token] (set/union present unknown)
                               :when (= t target)] token))
            observation {:status :observed :o target-present :checked checked
                         :channel (zipmap checked (repeat :D-task-measured))}
            policy-key (prior/policy-key {:mission target :shown precedence
                                          :semilattice {}})
            step (flight/conditioning-step
                  {:run-record record :target target
                   :flight-id (str "completed-run-" run-id) :click-id run-id
                   :observation observation :policy-key policy-key
                   :precedence precedence :enactments []})]
        (if (= :present (:status step))
          {:steps [{:step (assoc step :observation-source
                                  {:kind :completed-predecessor-run
                                   :run/id run-id
                                   :same-run-prediction-counted? false})
                     :path (str "run-record:" run-id)
                     :sha256 (sha256 (.getBytes (pr-str record) "UTF-8"))}]
           :status :present :source :completed-predecessor-run}
          {:steps [] :status :absent
           :reason (or (:reason step) :predecessor-observation-refused)
           :detail (select-keys step [:status :reason :inputs :tokens])})))))
