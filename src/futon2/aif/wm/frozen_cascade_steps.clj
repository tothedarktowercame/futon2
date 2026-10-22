(ns futon2.aif.wm.frozen-cascade-steps
  "Durable offline section checkpoints. Advance only from recorded success."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.selected-outer-cascade-policy :as handoff]
            [futon2.aif.wm.cascade-enactment-compiler :as compiler]
            [futon2.aif.wm.cascade-capability-dispatch :as dispatch]
            [futon2.aif.wm.cascade-continuation :as continuation]
            [futon2.aif.wm.executable-cascade :as cascade])
  (:import [java.util.zip GZIPInputStream GZIPOutputStream]
           [java.nio.file Files StandardCopyOption]
           [java.time Instant]))

(def stages [:selected :materialized :compiled :constructed :wired :dispatch-prepared])
(defn require! [condition kind detail]
  (when-not condition (throw (ex-info (name kind) (assoc detail :kind kind)))))
(defn read-gzip [path]
  (with-open [in (GZIPInputStream. (io/input-stream path))]
    (edn/read-string (slurp in :encoding "UTF-8"))))
(defn- path [root i]
  (io/file root (format "%02d-%s.edn.gz" i (name (nth stages i)))))
(defn- write-once! [file value]
  (io/make-parents file)
  (if (.exists (io/file file))
    (require! (= value (read-gzip file)) :checkpoint-collision {:path (str file)})
    (let [tmp (Files/createTempFile (.toPath (.getParentFile (io/file file)))
                                    "checkpoint-" ".tmp"
                                    (make-array java.nio.file.attribute.FileAttribute 0))]
      (try
        (with-open [out (GZIPOutputStream. (io/output-stream (.toFile tmp)))]
          (.write out (.getBytes (pr-str value) "UTF-8")))
        (Files/move tmp (.toPath (io/file file))
                    (into-array java.nio.file.CopyOption [StandardCopyOption/ATOMIC_MOVE]))
        (finally (Files/deleteIfExists tmp))))))
(defn- producer []
  (let [revision (shell/sh "git" "rev-parse" "HEAD")
        paths ["src/futon2/aif/full_loop_runner.clj"
               "src/futon2/aif/wm/compiled_cascade_construction.clj"
               "src/futon2/aif/wm/frozen_cascade_steps.clj"
               "src/futon2/aif/wm/cascade_continuation.clj"]]
    (require! (zero? (:exit revision)) :source-revision-unavailable {})
    {:revision (.trim ^String (:out revision))
     :sources (into (sorted-map)
                    (map (fn [p] [p (evidence/sha256 (.getBytes (slurp p) "UTF-8"))])) paths)}))
(defn- checkpoint [stage predecessor payload]
  (let [body {:schema :wm/frozen-cascade-checkpoint-v1
              :stage stage :predecessor predecessor :payload payload
              :producer (producer)}]
    (assoc body :digest (evidence/value-digest body))))
(defn history [root]
  (loop [i 0 prior nil entries []]
    (if (and (< i (count stages)) (.isFile (path root i)))
      (let [record (read-gzip (path root i))]
        (require! (and (= (nth stages i) (:stage record))
                       (= prior (:predecessor record))
                       (= (:digest record) (evidence/value-digest (dissoc record :digest))))
                  :checkpoint-chain-invalid {:path (str (path root i))})
        (recur (inc i) (:digest record) (conj entries record)))
      entries)))
(defn initialize! [selected-path recorded-compiled-path root]
  (let [selected (read-gzip selected-path)
        recorded (read-gzip recorded-compiled-path)
        decision (handoff/decision (:selected-outer-cascade-policy selected))
        _ (require! (= :selected (:status decision)) :selected-boundary-invalid decision)
        c (get-in selected [:selected-outer-cascade-policy :selected-entry :canonical-cascade])
        _ (require! (and (cascade/certified? (cascade/validate-cascade recorded))
                         (= (:structural c) (:structural recorded)))
                    :recorded-authoring-does-not-match-selection {})
        record (checkpoint :selected nil {:selection selected :recorded-compiled recorded})]
    (write-once! (path root 0) record)
    {:status :completed :stage :selected :checkpoint (str (path root 0))
     :digest (:digest record)}))
(defn- recorded-authoring [record]
  (let [nodes (:executable-nodes record)
        attributions (set (map #(get-in % [:clause :authoring/attribution]) nodes))]
    (require! (= 1 (count attributions)) :ambiguous-recorded-authoring {})
    {:attribution (first attributions)
     :application-order (get-in record [:relations :application-order])
     :guards (get-in record [:relations :guards])
     :clauses (into {} (map (fn [n] [(:occurrence-id n)
                                    (dissoc (:clause n) :authoring/attribution)])) nodes)}))
(defn- run-stage [stage by-stage]
  (let [initial (:selected by-stage)
        selected (:selection initial)
        entry (get-in selected [:selected-outer-cascade-policy :selected-entry])]
    (case stage
      :materialized
      (let [record (:canonical-cascade entry)
            validation (cascade/validate-cascade record)]
        (require! (cascade/certified? validation) :canonical-cascade-invalid validation)
        {:canonical record :validation validation})
      :compiled
      (let [authoring (recorded-authoring (:recorded-compiled initial))
            compiled (compiler/compile-executable
                      (:structural (:canonical (:materialized by-stage))) authoring)
            record (:record compiled)
            _ (require! (and record (seq (:frontier record))
                             (= (:digest (:recorded-compiled initial)) (:digest record)))
                        :recorded-compilation-not-reproduced (select-keys compiled [:refusal :digest]))
            routing (dispatch/route-frontier {:wm/pattern-apply :full-loop-build-review-close} record)]
        (require! (= :routed (:status routing)) :frontier-not-routed routing)
        {:compiled record :authoring authoring :routing routing})
      :constructed
      (let [construction (runner/construct-for-decision
                          (assoc entry :compiled-cascade (get-in by-stage [:compiled :compiled])))]
        (require! (map? construction) :construction-missing {})
        {:construction construction})
      :wired
      (let [result (runner/construction-wiring-result (get-in by-stage [:constructed :construction]))]
        (require! (= :wired (:status result)) :construction-not-wired result)
        result)
      :dispatch-prepared
      (let [compiled (get-in by-stage [:compiled :compiled])
            attribution (get-in by-stage [:compiled :authoring :attribution])
            author (:author attribution) reviewer (:reviewer attribution)
            requests (mapv #(continuation/effect-request (:digest compiled) % author reviewer)
                           (:frontier compiled))]
        (require! (and author reviewer (not= author reviewer) (seq requests))
                  :execution-cast-unavailable {})
        {:requests requests :construction (get-in by-stage [:constructed :construction])
         :scope :prepared-new-work :task-executed false
         :historical-source (:source selected)}))))
(defn advance! [root]
  (let [records (history root)
        n (count records)]
    (require! (pos? n) :initialize-first {})
    (if (= n (count stages))
      {:status :completed :stage (last stages) :task-executed false :reused true :digest (:digest (last records))}
      (let [stage (nth stages n) predecessor (:digest (last records))
            by-stage (into {} (map (juxt :stage :payload)) records)]
        (try
          (let [payload (run-stage stage by-stage)
                record (checkpoint stage predecessor payload)]
            (write-once! (path root n) record)
            {:status :completed :stage stage :checkpoint (str (path root n))
             :digest (:digest record) :resumed-from (:stage (last records))})
          (catch Exception ex
            (let [failure {:schema :wm/frozen-section-attempt-v1 :status :failed
                           :stage stage :predecessor predecessor
                           :at (str (Instant/now)) :error (ex-message ex)
                           :detail (ex-data ex)}
                  file (io/file root "attempts" (str (evidence/value-digest failure) ".edn.gz"))]
              (write-once! file failure)
              {:status :failed :stage stage :last-success (:stage (last records))
               :attempt (str file) :detail (select-keys (ex-data ex) [:kind :failure-kind :findings])})))))))

(defn fork! [source-root stage target-root]
  (let [records (history source-root)
        i (.indexOf stages (keyword stage))]
    (require! (and (<= 0 i) (< i (count records))) :successful-stage-missing {:stage stage})
    (require! (empty? (history target-root)) :fork-target-not-empty {})
    (doseq [n (range (inc i))] (write-once! (path target-root n) (nth records n)))
    {:status :forked :through (keyword stage) :copied (inc i)
     :digest (:digest (nth records i)) :target target-root}))
