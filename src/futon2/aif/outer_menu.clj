(ns futon2.aif.outer-menu
  "Read-only construction of a task-selection menu from pinned library mappings.
  No eval, storage writes, learned-state migration or serving installation."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.outer-score :as score]
            [futon2.aif.q2-construction :as q2])
  (:import [java.nio.file Files]))

(def schema :wm/task-selection-executable-steps-v1)
(defn- refuse! [reason data]
  (throw (ex-info "Task-selection construction refused" (assoc data :kind reason))))
(defn- digest [x] (identity/sha256 (.getBytes (pr-str x) "UTF-8")))
(defn- sha? [x] (and (string? x) (boolean (re-matches #"[0-9a-f]{64}" x))))
(defn- pinned-file [root path]
  (let [f (io/file root path)]
    (when-not (.isFile f) (refuse! :outer-menu/source-missing {:path (.getPath f)}))
    (let [bytes (Files/readAllBytes (.toPath f))]
      {:sha256 (identity/sha256 bytes) :text (String. bytes "UTF-8")})))

(defn- valid-step? [step]
  (or (contains? #{:ready-gate :by-cone :by-unblocks :by-rank} step)
      (and (map? step) (= #{:status-gate} (set (keys step)))
           (contains? #{:ready :not-ready :undetermined} (:status-gate step)))))

(defn- validate-steps! [cascade]
  (when-not (and (vector? (:steps cascade)) (seq (:steps cascade))
                (every? valid-step? (:steps cascade)))
    (refuse! :outer-menu/invalid-steps {:cascade (:id cascade)}))
  ;; Unknown cone/unblocks must not be smuggled in as zeros. A ready or
  ;; not-ready gate narrows the possible statuses before a metric ordering.
  (reduce (fn [statuses step]
            (when (and (contains? #{:by-cone :by-unblocks} step)
                       (contains? statuses :undetermined))
              (refuse! :outer-menu/unavailable-metric-order
                       {:cascade (:id cascade) :step step :statuses statuses}))
            (cond (= :ready-gate step) (set/intersection statuses #{:ready})
                  (map? step) (set/intersection statuses #{(:status-gate step)})
                  :else statuses))
          #{:ready :not-ready :undetermined} (:steps cascade)))

(defn load-library
  "Pin every pattern plus the mapping and README. A composite pin binds all
  three; :manifest-sha is the existing pattern-only manifest's digest."
  [root]
  (let [manifest (q2/library-manifest root)
        _ (when-not (= :pinned (:status manifest))
            (refuse! :outer-menu/library-unavailable {:library manifest}))
        mapping-file (pinned-file root "executable-steps.edn")
        readme (pinned-file root "README.md")
        mapping (edn/read-string (:text mapping-file))
        entries (:cascades mapping)]
    (when-not (and (= schema (:schema mapping)) (pos-int? (:version mapping))
                  (= (:readme-sha256 mapping) (:sha256 readme))
                  (vector? entries) (seq entries)
                  (= (count entries) (count (set (map :id entries)))))
      (refuse! :outer-menu/mapping-invalid {:mapping-sha (:sha256 mapping-file)}))
    (doseq [c entries]
      (when-not (and (keyword? (:id c)) (pos-int? (:version c))
                    (pos-int? (:readme-cascade c))
                    (vector? (:readme-lines c)) (= 2 (count (:readme-lines c)))
                    (every? pos-int? (:readme-lines c))
                    (<= (first (:readme-lines c)) (second (:readme-lines c))))
        (refuse! :outer-menu/mapping-entry-invalid {:cascade (:id c)}))
      (case (:status c)
        :executable
        (do (when-not (and (vector? (:patterns c)) (seq (:patterns c))
                           (every? #(contains? (:patterns manifest) %) (:patterns c)))
              (refuse! :outer-menu/pattern-outside-manifest {:cascade (:id c)}))
            (validate-steps! c))
        :not-executable
        (when-not (seq (:needs c))
          (refuse! :outer-menu/unsupported-without-reason {:cascade (:id c)}))
        :alias
        (when-not (some #(and (= (:alias-of c) (:id %)) (= :executable (:status %))) entries)
          (refuse! :outer-menu/alias-target-invalid {:cascade (:id c) :alias-of (:alias-of c)}))
        (refuse! :outer-menu/mapping-status-invalid {:cascade (:id c)})))
    {:mapping mapping
     :library (assoc manifest :manifest-sha (:sha256 manifest)
                     :mapping-sha (:sha256 mapping-file) :readme-sha (:sha256 readme)
                     :sha256 (digest [(:sha256 manifest) (:sha256 mapping-file) (:sha256 readme)]))}))

(defn cascade-digest
  "Same ordered carrier as emit-selection-grounding-lean.bb. IDs are the full
  digest here; the Lean emitter takes its first 15 hex digits as a Nat."
  [{:keys [id readme-cascade units edges]}]
  (digest [(name id) readme-cascade
           (mapv (fn [u] [(:pattern u) (:sha256 u) (:index u)]) units) edges]))

(defn- construct-cascade [library c]
  (let [units (mapv (fn [i p] {:pattern (str "futon3/library/task-selection/" p ".flexiarg")
                               :sha256 (get-in library [:patterns p]) :index i})
                    (range) (:patterns c))
        edges (mapv (fn [i] [i (inc i) :precedes]) (range (dec (count units))))
        result (assoc c :units units :edges edges)]
    (assoc result :digest (cascade-digest result)
           :digest-scheme :sha256-pr-str-name-readme-units-edges)))

(defn construct
  "Construct a receipt, never selecting or mutating learned state. Missing
  reading/graph/kernel evidence remains typed absence, not a hash of nil."
  [root makeup observation]
  (let [{:keys [library mapping]} (load-library root)
        cascades (mapv #(construct-cascade library %)
                       (filter #(= :executable (:status %)) (:cascades mapping)))
        duplicates (filter #(> (count %) 1) (vals (group-by (juxt :steps :units) cascades)))
        _ (when (seq duplicates)
            (refuse! :duplicate-policy {:cascades (mapv #(mapv :id %) duplicates)}))
        _ (when (< (count cascades) 2)
            (refuse! :outer-menu/insufficient-cascades {:count (count cascades)}))
        slice (vec (sort (set (mapcat :patterns cascades))))
        pool (vec (sort (set (mapcat (fn [c] (map #(-> (:pattern %)
                                                        (str/replace "futon3/library/task-selection/" "")
                                                        (str/replace #"\.flexiarg$" ""))
                                                 (:units c))) cascades))))
        included (set slice)
        retrieval {:query :readme-evidenced-and-executable
                   :manifest-sha (:manifest-sha library) :mapping-sha (:mapping-sha library)
                   :included slice
                   :excluded (mapv (fn [p] {:pattern p :reason :not-in-executable-readme-cascade})
                                   (remove included (keys (:patterns library))))}
        kernel-input (score/kernel-parameters makeup)
        kernel (if (and (:c-kit makeup) (:gain-kit makeup))
                 (digest kernel-input)
                 {:status :absent :reason :kernel-parameters-unretained})]
    (assert (= (set (keys (:patterns library)))
               (set (concat slice (map :pattern (:excluded retrieval))))))
    (assert (= pool slice))
    {:schema :wm/outer-menu-construction-v1 :status :constructed
     :reading (if (sha? (:giraffe/pass-sha256 observation)) (:giraffe/pass-sha256 observation)
                  {:status :absent :reason :giraffe-pass-pin-unavailable})
     :graph (if (sha? (:giraffe/input-sha256 observation)) (:giraffe/input-sha256 observation)
                {:status :absent :reason :giraffe-input-pin-unavailable})
     :library library :kernel kernel :kernel-input kernel-input
     :slice slice :pool pool :retrieval retrieval
     :cascades cascades :policy-count (count cascades)}))
