(ns run-row16-r17-real-envelope
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.a4a-substrate :as substrate]
            [futon2.aif.bmr :as bmr]
            [futon2.aif.r17-offline :as r17])
  (:import [java.security MessageDigest]
           [java.time Instant]))

(def run-dir "holes/labs/wm-contract/runs/row-16-r17-envelope-2026-09-12")
(def parent-path "holes/labs/wm-contract/r17-parent-model.edn")
(def producer-path "src/futon2/aif/r17_offline.clj")
(def v7-path "holes/labs/wm-contract/sim/R17-carriers.edn")

(defn bytes-sha [bytes]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn file-sha [path]
  (bytes-sha (java.nio.file.Files/readAllBytes (.toPath (io/file path)))))

(defn canonical-sha [x]
  (bytes-sha (.getBytes (str (pr-str x) "\n") "UTF-8")))

(defn refuse! [reason data]
  (throw (ex-info (name reason) (assoc data :refusal reason))))

(defn validate-coordinate-vectors! [{:keys [a A a-prime coordinates]}]
  (when-not (= (count a) (count A) (count a-prime) (count coordinates))
    (refuse! :vector-cardinality-mismatch
             {:counts (mapv count [a A a-prime coordinates])}))
  (when-not (= coordinates (vec (sort-by pr-str coordinates)))
    (refuse! :vector-order-mismatch {:coordinates coordinates}))
  true)

(defn proposal-inputs [envelope]
  (let [{:keys [outcomes concentrations prior]}
        (get-in envelope [:r17/replay :input :corpus])]
    (mapv
     (fn [proposal]
       (let [[left right] (get-in proposal [:reduction :members])
             left-row (get concentrations left)
             right-row (get concentrations right)
             a (vec (repeat (+ (count left-row) (count right-row)) prior))
             A (vec (concat left-row right-row))
             pooled (mapv #(/ (+ %1 %2) 2.0) left-row right-row)
             a-prime (vec (concat pooled pooled))
             coordinates (vec (concat (map #(vector left %) outcomes)
                                      (map #(vector right %) outcomes)))
             row {:proposal (:reduction proposal)
                  :coordinates coordinates :a a :A A :a-prime a-prime
                  :A-prime (:reduced-posterior proposal)
                  :delta-F (get-in proposal [:evidence :value])
                  :threshold (get-in proposal [:evidence :threshold])
                  :decision (:decision proposal)}]
         (validate-coordinate-vectors! row)
         row))
     (:r17/proposals envelope))))

(defn execute! []
  (.mkdirs (io/file run-dir))
  (let [parent (edn/read-string (slurp parent-path))
        _ (when-not (= :wm/r17-parent-model-v1 (:schema parent))
            (refuse! :parent-model-config-invalid {:path parent-path}))
        opts (get-in parent [:corpus :reader-options])
        corpus (substrate/read-corpus opts)
        corpus-sha (canonical-sha corpus)
        expected-sha (get-in parent [:corpus :canonical-corpus-sha256])
        _ (when-not (= expected-sha corpus-sha)
            (refuse! :commissioned-corpus-pin-mismatch
                     {:expected expected-sha :observed corpus-sha}))
        envelope (r17/run {:run-id "row-16-r17-envelope-2026-09-12"
                           :parent-model parent
                           :corpus corpus})
        inputs (proposal-inputs envelope)
        first-capability (first (get-in envelope [:r17/replay :input :corpus
                                                  :capabilities]))
        mutated-input (update-in (get-in envelope [:r17/replay :input])
                                 [:corpus :concentrations first-capability 0]
                                 + 1.0)
        mutated (r17/run mutated-input)
        original-first (first (:r17/proposals envelope))
        mutated-first (first (:r17/proposals mutated))
        mutation-changed? (not= (select-keys original-first
                                             [:reduced-posterior :evidence])
                                (select-keys mutated-first
                                             [:reduced-posterior :evidence]))
        order-control (try
                        (validate-coordinate-vectors!
                         {:a [1.0 1.0] :A [2.0 2.0] :a-prime [1.0 1.0]
                          :coordinates [[:z 0] [:a 0]]})
                        nil
                        (catch clojure.lang.ExceptionInfo e
                          (:refusal (ex-data e))))
        cardinality-control (try
                              (bmr/bayesian-model-reduction
                               [1.0 1.0] [2.0] [1.0 1.0])
                              nil
                              (catch clojure.lang.ExceptionInfo _
                                :vector-cardinality-mismatch))
        v7 (edn/read-string (slurp v7-path))
        v7-results
        (mapv (fn [case]
                (let [actual (bmr/bayesian-model-reduction
                              (:full-prior case) (:full-posterior case)
                              (:reduced-prior case))]
                  {:id (:id case)
                   :expected {:delta-F (:expected-delta-f case)
                              :accept? (:expected-accept? case)}
                   :actual (select-keys actual [:delta-F :accept?])
                   :pass? (= [(:expected-delta-f case) (:expected-accept? case)]
                             [(:delta-F actual) (:accept? actual)])}))
              (:model-reduction v7))
        threshold-case (first (filter #(= :informative-parameter (:id %))
                                      v7-results))
        controls {:replay-mutation
                  {:expected :changed :observed (if mutation-changed? :changed :unchanged)
                   :passed? mutation-changed?}
                  :vector-order {:expected :vector-order-mismatch
                                 :observed order-control
                                 :passed? (= :vector-order-mismatch order-control)}
                  :vector-cardinality {:expected :vector-cardinality-mismatch
                                       :observed cardinality-control
                                       :passed? (= :vector-cardinality-mismatch
                                                   cardinality-control)}
                  :threshold-above-minus-three
                  {:delta-F (get-in threshold-case [:actual :delta-F])
                   :threshold bmr/acceptance-threshold
                   :expected :reject
                   :observed (if (get-in threshold-case [:actual :accept?])
                               :accept :reject)
                   :passed? (and (> (get-in threshold-case [:actual :delta-F])
                                    bmr/acceptance-threshold)
                                 (false? (get-in threshold-case [:actual :accept?])))}
                  :v7-cross-checks v7-results}
        result {:schema :wm/r17-retained-offline-envelope-v1
                :scope :real-offline-substrate-corpus
                :retained-at (str (Instant/now))
                :producer {:entrypoint 'futon2.aif.r17-offline/run
                           :source {:repo "futon2" :path producer-path
                                    :sha256 (file-sha producer-path)}}
                :commissioning-record {:repo "futon2" :path parent-path
                                       :sha256 (file-sha parent-path)}
                :corpus-read {:sha256 corpus-sha
                              :counts {:capabilities (count (:capabilities corpus))
                                       :edges (count (:edges corpus))
                                       :discharges (count (:discharges corpus))}}
                :producer-envelope envelope
                :proposal-replay-inputs inputs
                :controls controls}
        all-controls-pass? (and (every? :passed? (vals (dissoc controls :v7-cross-checks)))
                                (every? :pass? v7-results))]
    (when-not all-controls-pass?
      (refuse! :r17-envelope-control-failed {:controls controls}))
    (spit (str run-dir "/envelope.edn") (str (pr-str result) "\n"))
    (prn {:proposals (count inputs)
          :delta-F-range [(apply min (map :delta-F inputs))
                          (apply max (map :delta-F inputs))]
          :decision (get-in envelope [:r17/decision :outcome])
          :controls-pass? all-controls-pass?})))

(execute!)
