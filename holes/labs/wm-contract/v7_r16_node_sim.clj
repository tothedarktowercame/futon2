(ns v7-r16-node-sim
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.policy :as policy]))

(def lab (io/file "holes/labs/wm-contract"))
(def carriers (edn/read-string (slurp (io/file lab "sim/R16-carriers.edn"))))
(def equation-text (slurp (io/file lab "aif-equations.edn")))
(def enact-text (slurp "src/futon2/aif/enact.clj"))

(defn first-argmax [xs]
  (first (reduce (fn [[_ best-v :as best] [i v]]
                   (if (> (double v) (double best-v)) [i v] best))
                 [0 (first xs)] (rest (map-indexed vector xs)))))

(defn reference-action [{:keys [ranked-actions tau f-pi]}]
  ;; Independent transcription of u = argmax Q(pi): exp/normalisation preserves
  ;; order, so maximise ln E - G/tau - F_pi and keep the first maximum.
  (let [scores (mapv (fn [entry fp]
                       (- (double (:habit-prior-bias entry))
                          (/ (double (:controller-score entry)) tau)
                          (double fp)))
                     ranked-actions f-pi)]
    (get-in ranked-actions [(first-argmax scores) :action :type])))

(defn node-action [{:keys [ranked-actions tau f-pi]}]
  (get-in (policy/select-action
           ranked-actions
           {:selection-boundary :strategic-recommendation
            :selection-law :full-score-posterior
            :selection-gain tau
            :temperature-opts {:tau-mode :selection-gain-only}
            :f-pi-opts {:f-pi-policy-posterior? true
                        :f-pi-values f-pi :f-pi-scaling :unscaled}})
          [:action :type]))

(def case-results
  (mapv (fn [c] (assoc (select-keys c [:id])
                       :reference (reference-action c)
                       :node (node-action c)
                       :pass? (= (reference-action c) (node-action c))))
        (:cases carriers)))

(def trace-files
  (->> (.listFiles (io/file "data/wm-trace"))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
       (sort-by #(.getName %))))
(def trace-text (str/join "\n" (map slurp trace-files)))
(def checks
  [{:id :registry-row-present :pass? (str/includes? equation-text ":id :action :defines :u :node :R16")}
   {:id :declared-cases-match :pass? (every? :pass? case-results)}
   {:id :enactor-does-not-read-selected-action
    :pass? (not (re-find #"\(:action\s+judgement\)|\[:action\]" enact-text))}
   {:id :first-passing-gate-is-enacted
    :pass? (and (str/includes? enact-text "(first (filter #(= :pass (:verdict %)) gates))")
                (str/includes? enact-text "(when passed (enact! passed))"))}
   {:id :implementation-declares-no-outward-action
    :pass? (and (str/includes? enact-text "NO substrate write, NO outward action")
                (str/includes? enact-text ":constructed-wiring"))}
   {:id :corpus-has-no-external-witness-field
    :pass? (not (re-find #":external-(?:effect|witness)|:substrate-(?:effect|witness)" trace-text))}])

(def plants
  [{:id :wrong-formula-last-argmax
    :caught? (not= (:reference (second case-results))
                   (get-in (:ranked-actions (second (:cases carriers))) [1 :action :type]))}
   {:id :invent-selected-action-read
    :caught? (boolean (re-find #"\(:action\s+judgement\)" "(:action judgement)"))}
   {:id :invent-external-witness
    :caught? (boolean (re-find #":external-(?:effect|witness)" "{:external-witness true}"))}])

(def receipt
  {:schema :wm/v7-r16-node-sim-v1
   :node :R16
   :equation {:id :action :defines :u :cases case-results}
   :implementation {:selection-read-by-enactor? false
                    :enacts-first-passing-gate? true
                    :declares-no-outward-action? true}
   :corpus {:files (count trace-files)
            :external-witness-field-count
            (count (re-seq #":external-(?:effect|witness)|:substrate-(?:effect|witness)" trace-text))}
   :checks checks
   :plants plants
   :summary {:checks-pass (count (filter :pass? checks))
             :checks-total (count checks)
             :plants-caught (count (filter :caught? plants))
             :plants-total (count plants)}})

(when-not (and (every? :pass? checks) (every? :caught? plants))
  (binding [*out* *err*] (pp/pprint receipt))
  (System/exit 1))
(let [f (io/file lab "runs/V7-R16-node-sim/00-r16.edn")]
  (io/make-parents f)
  (with-open [w (io/writer f)] (binding [*out* w] (pp/pprint receipt))))
(pp/pprint (:summary receipt))
