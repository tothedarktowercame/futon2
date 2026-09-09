#!/usr/bin/env bb

(require '[babashka.fs :as fs]
         '[clojure.edn :as edn]
         '[clojure.pprint :as pp])

(def root (fs/canonicalize (fs/cwd)))
(def source-path (fs/path root "holes/labs/wm-contract/runs/F12-organise/23-snatch-joint.edn"))
(def output-path (fs/path root "holes/labs/wm-contract/runs/U90-snatch-mini-c.edn"))

(def observations
  ;; These three recorded rows expose the three Snatch endings used by the
  ;; miniature.  The outcome labels are a declared coarsening, not a ruling on
  ;; the production twelve-kind C carrier.
  {[:g1 :snatcher] {:outcome :grim-cut :score -5}
   [:g4 :snatcher] {:outcome :remedy :score-before 3 :score-after -5}
   [:g2 :snatcher] {:outcome :abstention :score -10}})

(def support #{:grim-cut :remedy :abstention})
(def declared-c {:grim-cut 1/6 :remedy 2/3 :abstention 1/6})

(defn row-for [rows scenario]
  (or (first (filter #(= scenario (:scenario %)) rows))
      (throw (ex-info "pinned Snatch scenario not found" {:scenario scenario}))))

(defn validate-pin! [row {:keys [score score-before score-after]}]
  (doseq [[k expected] (remove (comp nil? val)
                               {:score-before score-before
                                :score-after score-after})]
    (when-not (= expected (get row k))
      (throw (ex-info "pinned Snatch measurement drifted"
                      {:scenario (:scenario row) :field k
                       :expected expected :actual (get row k)}))))
  ;; Ordinary rows carry a single :score-before/:score-after measurement.
  (when (some? score)
    (when-not (= [score score] [(:score-before row) (:score-after row)])
      (throw (ex-info "pinned Snatch score drifted"
                      {:scenario (:scenario row) :expected score
                       :actual [(:score-before row) (:score-after row)]}))))
  row)

(defn point-mass [outcome]
  (into (sorted-map) (map (fn [d] [d (if (= d outcome) 1 0)]) support)))

(defn kl [q c]
  (reduce-kv (fn [total d qd]
               (if (zero? qd) total
                   (+ total (* (double qd)
                               (Math/log (/ (double qd) (double (get c d))))))))
             0.0 q))

(defn calculate []
  (let [record (edn/read-string (slurp (str source-path)))
        rows (:rows record)
        fitted (into (sorted-map)
                     (for [[scenario spec] observations
                           :let [row (validate-pin! (row-for rows scenario) spec)]]
                       [scenario {:count 1
                                  :P-d-given-o (point-mass (:outcome spec))
                                  :measured {:score-before (:score-before row)
                                             :score-after (:score-after row)}}]))
        policies (into (sorted-map)
                       (for [[policy scenario] {:grim [:g1 :snatcher]
                                                :patterns-with-remedy [:g4 :snatcher]
                                                :preserve-abstention [:g2 :snatcher]}
                             :let [q-o {scenario 1}
                                   q-d (get-in fitted [scenario :P-d-given-o])]]
                         [policy {:Q-o q-o :Q-d q-d
                                  :disposition-risk-nats (kl q-d declared-c)}]))]
    {:record :U90-snatch-mini-c
     :measured-at "2026-09-09"
     :source {:record (:record record)
              :pointer "futon2:holes/labs/wm-contract/runs/F12-organise/23-snatch-joint.edn:39-170"}
     :support support
     :declared-C declared-c
     :kernel fitted
     :policies policies
     :selected-policy (->> policies (apply min-key (comp :disposition-risk-nats val)) key)
     :checks {:C-normalised (= 1 (reduce + (vals declared-c)))
              :all-kernel-rows-normalised (every? #(= 1 (reduce + (vals (:P-d-given-o %)))) (vals fitted))
              :unobserved-outcomes-explicit-zero
              (every? #(= 2 (count (filter zero? (vals (:P-d-given-o %))))) (vals fitted))}}))

(defn render [value]
  (with-out-str (pp/pprint value)))

(let [actual (render (calculate))
      check? (= "--check" (first *command-line-args*))]
  (if check?
    (if (and (fs/exists? output-path) (= actual (slurp (str output-path))))
      (println "snatch-mini-c: CURRENT")
      (do (binding [*out* *err*] (println "snatch-mini-c: STALE")) (System/exit 1)))
    (do (spit (str output-path) actual)
        (println (str "wrote " output-path)))))
