(ns wm04.pilot-result
  "WM-04 S-4 pilot result join (claude-4, 2026-09-17).

  Admits each subject from its blinded adjudication and its blinded review
  with futon2.aif.observation-admission/admit. It then joins the admitted
  label (the reference) with the recorded verdict (the observation) from
  p4ng DATA-cascade-outcomes-2026-09-17.edn, and counts errors with
  futon2.aif.observation-rates/rates-by-class.

  The two sampling groups are separate token classes and are never pooled.
  Blinding-uncertain subjects are admitted but kept out of the error counts.
  No prior is applied.

  Run: java -cp \"$(clojure -Spath -M:test)\" clojure.main scripts/wm04/pilot_result.clj [--check-digests]"
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.observation-admission :as oa]
            [futon2.aif.observation-rates :as rates]
            [clojure.pprint]))

(def dir "holes/labs/wm-contract/wm04-pilot")
(def data-path "/home/joe/code/p4ng/wm-walkthroughs/build-loop/closure/DATA-cascade-outcomes-2026-09-17.edn")
(def blinding-uncertain
  "futon2 at the cutoff quotes these subjects' recorded outcomes
  (pattern_reliability.clj:16 and its test :13-14; reported by zai-18)."
  #{"WM-10-C6" "R5-1"})

(defn- read-edn-dir [sub]
  (let [d (io/file dir sub)]
    (if (.exists d)
      (vec (mapcat #(edn/read-string (slurp %))
                   (sort (filter #(.endsWith (.getName %) ".edn") (.listFiles d)))))
      [])))

(defn- recorded-verdicts []
  (into {}
        (for [c (:cascades (edn/read-string (slurp data-path)))
              n (:nodes c)]
          [(:id n) (case (:outcome n) :realised true :contradicted false nil)])))

(defn run [check-digests-only?]
  (let [subjects (->> (json/read-str (slurp (io/file dir "subjects.json")) :key-fn keyword)
                      :subjects
                      (map (juxt :subject-id identity))
                      (into {}))
        adjs (into {} (map (juxt :subject-id identity)) (read-edn-dir "adjudications"))
        digest-mismatch (vec (for [[id a] adjs
                                   :let [s (get subjects id)]
                                   :when (not= (:view-digest a)
                                               (oa/view-digest (oa/observer-view s)))]
                               id))]
    (if check-digests-only?
      {:subjects (count subjects) :adjudications (count adjs)
       :missing-adjudication (vec (remove adjs (keys subjects)))
       :digest-mismatch digest-mismatch}
      (let [reviews (into {} (map (juxt :subject-id identity)) (read-edn-dir "reviews"))
            recorded (recorded-verdicts)
            admitted (into {}
                           (for [[id s] subjects
                                 :let [a (get adjs id) r (get reviews id)]]
                             [id (cond (nil? a) {:status :missing :kind :no-adjudication}
                                       (nil? r) {:status :missing :kind :no-review}
                                       :else (oa/admit s a r))]))
            class-of (fn [s] [:J (keyword (:group s))])
            labels (for [[id s] subjects
                         :when (not (blinding-uncertain id))
                         :let [adm (get admitted id)]]
                     {:subject-id id
                      :token-class (class-of s)
                      :recorded (get recorded id)
                      :admitted (when (= :admitted (:status adm)) (:label adm))})
            subject-counts (frequencies (map class-of (vals subjects)))]
        {:schema :wm04-pilot-result-v1
         :prior :none
         :blinding-uncertain-excluded-from-counts (vec (sort blinding-uncertain))
         :admission (frequencies (map (fn [a] (or (:label a) (:kind a))) (vals admitted)))
         :admission-by-group (into {} (for [[g ids] (group-by #(:group (get subjects %)) (keys subjects))]
                                        [g (frequencies (map #(let [a (get admitted %)] (or (:label a) (:kind a))) ids))]))
         :rates (rates/rates-by-class labels subject-counts)
         :per-subject (into (sorted-map) (for [[id a] admitted]
                                           [id {:group (:group (get subjects id))
                                                :recorded (get recorded id)
                                                :admission (or (:label a) (:kind a))}]))}))))

(defn observer-only
  "Pilot comparison with no review step (Joe, 2026-09-17: a pilot does not need
  a full review cycle). Observer findings are compared with the recorded
  verdicts per group. These are UNREVIEWED pilot findings, not admitted labels,
  so they are not error rates for A."
  []
  (let [subjects (->> (json/read-str (slurp (io/file dir "subjects.json")) :key-fn keyword)
                      :subjects (map (juxt :subject-id identity)) (into {}))
        adjs (into {} (map (juxt :subject-id identity)) (read-edn-dir "adjudications"))
        recorded (recorded-verdicts)
        rows (for [[id s] subjects
                   :let [a (get adjs id)]]
               {:id id :group (:group s) :recorded (get recorded id)
                :finding (:finding a) :blinding-uncertain (boolean (blinding-uncertain id))})]
    {:schema :wm04-pilot-observer-only-v1
     :reviewed false
     :blinding-uncertain (vec (sort blinding-uncertain))
     :by-group (into (sorted-map)
                     (for [[g rs] (group-by :group rows)]
                       [g (frequencies (map (juxt :recorded :finding) (remove :blinding-uncertain rs)))]))
     :rows (vec (sort-by :id rows))}))

(let [observer? (some #{"--observer-only"} *command-line-args*)]
  (when observer?
    (let [r (observer-only)]
      (spit (io/file dir "PILOT-OBSERVER-ONLY.edn") (with-out-str (clojure.pprint/pprint r)))
      (prn (:by-group r))
      (System/exit 0))))

(let [check? (some #{"--check-digests"} *command-line-args*)
      result (run check?)]
  (if check?
    (prn result)
    (do (spit (io/file dir "RESULT.edn") (with-out-str (clojure.pprint/pprint result)))
        (prn (select-keys result [:admission :admission-by-group :rates])))))
