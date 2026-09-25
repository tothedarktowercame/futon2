;; H-C-REACH-I field row (claude-13, 2026-09-25): the 23 targets H-C-REACH-D
;; listed with ### n. sections, re-read through the CHANGED extractor. Read-only;
;; texts at the heads the target-field fixture recorded; prints to stdout.
;;
;;   cd /home/joe/code/futon2
;;   bb holes/labs/wm-contract/proof2/packets/h_c_reach_i_field.clj
(require '[clojure.edn :as edn] '[clojure.java.shell :as sh] '[clojure.string :as str])
(def root "/home/joe/code")
(def packets (str root "/futon2/holes/labs/wm-contract/proof2/packets"))
(def extractor (str root "/futon2/scripts/wm/extract-outcomes.clj"))
(load-file extractor)
(def fixture (str root "/futon2/test/fixtures/target-field/target-field@futon2-7bd17dfb.edn"))
(def the-23
  ;; the Q1 rows of h-c-reach-d-output.txt, in order
  (->> (str/split-lines (slurp (str packets "/h-c-reach-d-output.txt")))
       (filter #(str/includes? % ":iii-shape"))
       (mapv #(second (re-find #":target \"([^\"]+)\"" %)))))
(def old-units
  (into {} (for [l (str/split-lines (slurp (str packets "/h-c-reach-d-output.txt")))
                 :when (str/includes? l ":iii-shape")]
             [(second (re-find #":target \"([^\"]+)\"" l))
              (parse-long (second (re-find #":sections (\d+)" l)))])))
(let [f (edn/read-string {:default tagged-literal} (slurp fixture))
      tf (get-in f [:decision :target-field])
      heads (get-in f [:read :heads])
      by-target (into {} (map (juxt :target identity)) (:considered tf))
      rows (for [t the-23
                 :let [c (by-target t)
                       text (:out (sh/sh "git" "-C" (str root "/" (:repo c)) "show"
                                         (str (get heads (:repo c)) ":" (:path c))))
                       hs (headings text)
                       isecs (instance-sections hs (count (str/split-lines text)))
                       cs (mapv #(assoc %1 :id (keyword (str "o-" (inc %2))))
                                (consolidate (extract text (constantly nil) {})) (range))
                       {:keys [outcomes rejected]} (filter-outcomes text cs)]]
             {:target t :units-before (old-units t) :units-after (count isecs)
              :anchor (some? (instances-anchor hs))
              :outcomes (count outcomes)
              :shape-rejected (vec (map :reason (filter #(= :shape (:clause %)) rejected)))
              :links (count (artefact-links text isecs outcomes))})]
  (println ";; extractor sha256" (sha256 (slurp extractor)))
  (println ";; targets" (count the-23))
  (doseq [r rows] (prn r))
  (prn {:units-before (reduce + (map :units-before rows)) :units-after (reduce + (map :units-after rows))
        :links (reduce + (map :links rows)) :outcomes (reduce + (map :outcomes rows))
        :shape-rejected (frequencies (mapcat :shape-rejected rows))}))
