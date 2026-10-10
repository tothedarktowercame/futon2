(ns futon2.aif.progress-read
  "Read the persisted records for progress-check without consulting a live
  store. A flight envelope supplies click order, target, advanced wants and
  enactment citations; the caller explicitly supplies the click run-record
  directory because the flight does not retain it."
  (:require [futon2.aif.run-record-io :as run-record-io]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.progress-check :as progress-check]))

(defn- malformed! [kind detail]
  (throw (ex-info "progress flight record is unreadable"
                  (merge {:kind kind} detail))))

(defn- read-edn [path]
  (try
    (run-record-io/read-record path)
    (catch Exception _ ::unreadable)))

(defn- produced-for [enactments click-id]
  (if-let [entry (first (filter #(= click-id (:click-id %)) enactments))]
    (if-let [reason (get-in entry [:enactment :absent])]
      {:absent reason}
      (if-let [path (:record-path entry)]
        (let [record (read-edn path)]
          (cond
            (= ::unreadable record) {:absent :enactment-record-unreadable}
            ;; a record without an attempts vector is not evidence of no
            ;; production: check-flight reads #{} as that
            (not (and (map? record) (sequential? (:attempts record))))
            {:absent :enactment-record-no-attempts}
            :else
            (into #{} (comp (filter #(true? (:success %))) (map :produced))
                  (:attempts record))))
        {:absent :no-enactment-record-path}))
    {:absent :no-enactment-entry}))

(defn- candidate-needs [candidate target]
  (let [need-sets (map #(get-in % [:guard :needs] ::missing)
                       (get-in candidate [:id :precedence]))
        valid-set? #(and (set? %) (every? (fn [need]
                                            (and (vector? need)
                                                 (= 2 (count need))
                                                 (= target (first need))))
                                          %))]
    (if (and (sequential? (get-in candidate [:id :precedence]))
             (every? valid-set? need-sets))
      (into #{} (map second) (mapcat identity need-sets))
      {:absent :candidate-guard-needs-malformed})))

(defn- needs-for [run-record-dir click-id target]
  (let [path (io/file run-record-dir (str "tick-run-record-" click-id ".edn"))]
    (if-not (.isFile path)
      {:absent :run-record-missing}
      (let [record (read-edn path)
            chosen (when (map? record) (get-in record [:decision :chosen]))]
        (cond
          (= ::unreadable record) {:absent :run-record-unreadable}
          (or (nil? chosen) (= :absent (:status chosen)))
          {:absent :no-chosen-action}
          (not (contains? chosen :candidate))
          {:absent :no-candidate-on-run-record}
          :else
          (let [candidate-id (:candidate chosen)
                candidate (first
                           (filter #(= candidate-id (get-in % [:id :id]))
                                   (get-in record [:decision :selection-certificate
                                                   :candidates])))]
            (if candidate
              (candidate-needs candidate target)
              {:absent :chosen-candidate-not-in-certificate})))))))

(defn flight-clicks
  "Assemble progress-check's click vector from a parsed flight envelope and an
  explicit run-record directory. Mandatory click metadata throws typed ex-info;
  missing evidence becomes a typed absence, never an empty evidence set."
  [flight-envelope run-record-dir]
  (let [flight (:flight flight-envelope)
        target (:target flight)]
    (when-not (some? target)
      (malformed! :target-not-on-flight-record {}))
    (mapv
     (fn [position click]
       (let [click-id (:click-id click)]
         (when-not (some? click-id)
           (malformed! :click-id-not-on-flight-record {:position position}))
         (when-not (contains? click :advanced)
           (malformed! :advanced-not-on-click-record
                       {:position position :click-id click-id}))
         {:click-id click-id
          :target target
          :advanced (:advanced click)
          :produced (produced-for (:enactments flight) click-id)
          :needs (needs-for run-record-dir click-id target)}))
     (range)
     (:clicks flight))))

(defn check-flight-file
  "Read a persisted flight envelope and return the assembled checker input and
  pure progress-check verdict. RUN-RECORD-DIR defaults to the envelope's own
  :run-record-dir (written by flight-driver since 2026-09-28); an older
  envelope without it needs the directory passed."
  ([flight-path] (check-flight-file flight-path nil))
  ([flight-path run-record-dir]
   (let [envelope (edn/read-string {:default tagged-literal} (slurp flight-path))
         dir (or run-record-dir (:run-record-dir envelope))]
     (when-not dir
       (malformed! :run-record-dir-not-on-flight-record {:path (str flight-path)}))
     (let [input (flight-clicks envelope dir)]
       {:input input :check (progress-check/check-flight input)}))))
