(ns holes.labs.wm-contract.runs.row-16-r8-policy-f-2026-09-12.readback
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.policy-free-energy :as fpi]))

(def dir "holes/labs/wm-contract/runs/row-16-r8-policy-f-2026-09-12")
(def opts {:deterministic-tolerance 0.0 :absent-variance :floor :variance-floor 0.01})

(defn references []
  (into {} (for [line (rest (str/split-lines (slurp (str dir "/symbolic-reference.tsv"))))
                 :let [[candidate decimal binary] (str/split line #"\t")]]
             [(str "rank/" candidate) {:decimal90 decimal :binary64 (Double/parseDouble binary)}])))

(defn channel-references []
  (into {} (for [line (rest (str/split-lines (slurp (str dir "/symbolic-channel-reference.tsv"))))
                 :let [[candidate channel decimal binary] (str/split line #"\t")]]
             [[(str "rank/" candidate) (keyword channel)]
              {:decimal90 decimal :binary64 (Double/parseDouble binary)}])))

(defn prediction [channels]
  {:prediction-mean (into {} (map (juxt :channel :mean) channels))
   :prediction-variance (into {} (map (juxt :channel :raw-variance) channels))
   :variance-status
   (into {} (map (fn [{:keys [channel variance-status]}]
                   [channel {:status variance-status}]) channels))})

(defn outcome [f]
  (try {:status :unexpected-success :value (f)}
       (catch clojure.lang.ExceptionInfo e
         {:status :refused :error (:error (ex-data e)) :data (ex-data e)})))

(defn -main []
  (let [fixture (edn/read-string (slurp (str dir "/fixture.edn"))) refs (references)
        channel-refs (channel-references)
        rows (mapv (fn [{:keys [candidate-id retained-result channels]}]
                     (let [observation (into {} (map (juxt :channel :observation) channels))
                           actual (fpi/f-pi-for-candidate (prediction channels) observation opts)
                           reference (get-in refs [candidate-id :binary64])]
                       {:candidate-id candidate-id :retained retained-result :production actual
                        :symbolic-reference (get refs candidate-id)
                        :production-minus-reference (- actual reference)
                        :retained-minus-production (- (:value retained-result) actual)}))
                   (:rows fixture))
        channel-rows
        (mapv (fn [{:keys [candidate-id channels]}]
                (mapv (fn [d]
                        (let [single [d]
                              actual (fpi/f-pi-for-candidate
                                      (prediction single) {(:channel d) (:observation d)} opts)
                              reference (get channel-refs [candidate-id (:channel d)])]
                          {:candidate-id candidate-id :channel (:channel d)
                           :production actual :symbolic-reference reference
                           :delta (- actual (:binary64 reference))}))
                      channels))
              (:rows fixture))
        flat-channel-rows (vec (mapcat identity channel-rows))
        controls {:negative-variance
                  (outcome #(fpi/f-pi-for-candidate
                             {:prediction-mean {:x 0.0} :prediction-variance {:x -1.0}}
                             {:x 0.0} opts))
                  :deterministic-mismatch
                  (outcome #(fpi/f-pi-for-candidate
                             {:prediction-mean {:x 0.0} :prediction-variance {:x 0.0}}
                             {:x 1.0} opts))
                  :zero-variance-match
                  (fpi/f-pi-for-candidate
                   {:prediction-mean {:x 1.0} :prediction-variance {:x 0.0}}
                   {:x 1.0} opts)
                  :absent-variance-floor
                  (fpi/f-pi-for-candidate
                   {:prediction-mean {:x 0.0} :prediction-variance {:x 0.0}
                    :variance-status {:x {:status :absent}}}
                   {:x 0.25} opts)}
        max-delta (apply max (concat
                              (map #(Math/abs (double (:production-minus-reference %))) rows)
                              (map #(Math/abs (double (:delta %))) flat-channel-rows)))
        report {:schema :wm/row16-r8-policy-f-readback-v1
                :actual-production-function 'futon2.aif.policy-free-energy/f-pi-for-candidate
                :candidate-count (count rows) :coordinate-count (* 14 (count rows))
                :all-retained-replayed? (every? #(zero? (:retained-minus-production %)) rows)
                :maximum-symbolic-reference-delta max-delta
                :declared-measured-bound {:criterion :absolute-delta :bound 1.0e-12
                                          :all-within? (<= max-delta 1.0e-12)}
                :rows rows :channel-rows flat-channel-rows :controls controls
                :real-incomplete-coverage-control (:real-refusal-control fixture)}]
    (spit (str dir "/readback.edn") (with-out-str (pp/pprint report)))
    (prn (select-keys report [:candidate-count :coordinate-count :all-retained-replayed?
                              :maximum-symbolic-reference-delta :declared-measured-bound]))))

(-main)
