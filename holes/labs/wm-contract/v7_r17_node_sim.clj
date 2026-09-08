(ns v7-r17-node-sim
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.a4a :as a4a]
            [futon2.aif.bmr :as bmr]))

(def lab (io/file "holes/labs/wm-contract"))
(def carriers (edn/read-string (slurp (io/file lab "sim/R17-carriers.edn"))))
(def equation-text (slurp (io/file lab "aif-equations.edn")))
(def source-text
  (str (slurp "src/futon2/aif/a4a.clj") "\n"
       (slurp "src/futon2/aif/r17_offline.clj")))

(defn near? [a b]
  (< (Math/abs (- (double a) (double b))) 1.0e-12))

(defn declared-accumulation [{:keys [previous tick-mass]}]
  (merge-with + previous tick-mass))

(def accumulation (:accumulation carriers))
(def declared-result (declared-accumulation accumulation))
(def machine-result
  (:concentrations (a4a/corpus->concentration (:machine-corpus accumulation))))

(def bmr-results
  (mapv (fn [{:keys [id full-prior full-posterior reduced-prior
                     expected-delta-f expected-accept?]}]
          (let [actual (bmr/bayesian-model-reduction
                        full-prior full-posterior reduced-prior)]
            {:id id
             :expected {:delta-F expected-delta-f :accept? expected-accept?}
             :actual (select-keys actual [:delta-F :accept?])
             :pass? (and (near? expected-delta-f (:delta-F actual))
                         (= expected-accept? (:accept? actual)))}))
        (:model-reduction carriers)))

(def production-files
  (->> [(io/file "src") (io/file "scripts")]
       (mapcat file-seq)
       (filter #(and (.isFile %) (str/ends-with? (.getName %) ".clj")))
       (sort-by #(.getPath %))))
(def external-r17-callers
  (->> production-files
       (remove #(str/ends-with? (.getPath %) "src/futon2/aif/r17_offline.clj"))
       (filter #(re-find #"r17-offline|futon2\.aif\.r17-offline" (slurp %)))
       (mapv #(.getPath %))))

(def trace-files
  (->> (.listFiles (io/file "data/wm-trace"))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
       (sort-by #(.getName %))))
(def trace-r17-files
  (filterv #(str/includes? (slurp %) ":R17") trace-files))

(def checks
  [{:id :two-registry-rows-present
    :pass? (and (str/includes? equation-text ":id :dirichlet-accumulation :defines :a-conc :node :R17")
                (str/includes? equation-text ":id :model-reduction :defines :Delta-F :node :R17"))}
   {:id :declared-soft-accumulation-reference
    :pass? (= (:declared-result accumulation) declared-result)}
   {:id :machine-count-reference
    :pass? (= (:machine-result accumulation) machine-result)}
   {:id :declared-and-machine-grains-differ
    :pass? (and (= 2 (count declared-result))
                (= 1 (count (get machine-result "cap-a"))))}
   {:id :bmr-reference-cases-match
    :pass? (every? :pass? bmr-results)}
   {:id :bmr-is-offline
    :pass? (and (str/includes? source-text "Pure, replayable R17 structure-learning run envelopes")
                (str/includes? source-text "performs no substrate writes"))}
   {:id :no-production-r17-offline-caller
    :pass? (empty? external-r17-callers)}
   {:id :no-r17-trace-attribution
    :pass? (empty? trace-r17-files)}])

(def plants
  [{:id :drop-second-soft-cell
    :caught? (not= (:declared-result accumulation)
                   (dissoc declared-result [:channel-b :state-a]))}
   {:id :replace-bmr-threshold
    :caught? (not= (:expected-accept? (first (:model-reduction carriers)))
                   (<= (:expected-delta-f (first (:model-reduction carriers))) -6.0))}
   {:id :invent-live-caller
    :caught? (not (empty? ["src/futon2/aif/invented_r17_caller.clj"]))}
   {:id :invent-trace-attribution
    :caught? (str/includes? "{:route [{:node :R17}]}" ":R17")}])

(def receipt
  {:schema :wm/v7-r17-node-sim-v1
   :node :R17
   :equations {:dirichlet-accumulation
               {:declared declared-result
                :machine machine-result
                :same-grain? false}
               :model-reduction bmr-results}
   :implementation {:mode :offline
                    :external-r17-offline-callers external-r17-callers}
   :corpus {:files (count trace-files)
            :r17-attributed-files (mapv #(.getName %) trace-r17-files)}
   :checks checks
   :plants plants
   :summary {:checks-pass (count (filter :pass? checks))
             :checks-total (count checks)
             :plants-caught (count (filter :caught? plants))
             :plants-total (count plants)}})

(when-not (and (every? :pass? checks) (every? :caught? plants))
  (binding [*out* *err*] (pp/pprint receipt))
  (System/exit 1))
(let [f (io/file lab "runs/V7-R17-node-sim/00-r17.edn")]
  (io/make-parents f)
  (with-open [w (io/writer f)]
    (binding [*out* w] (pp/pprint receipt))))
(pp/pprint (:summary receipt))
