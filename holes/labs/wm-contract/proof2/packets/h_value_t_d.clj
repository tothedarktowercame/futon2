;; H-VALUE-T-D §2: the H-C extractor over every FEASIBLE target of the
;; target field's live read (claude-13, 2026-09-25). Read-only: reads the
;; fixture and each target's text at the repo head the fixture recorded
;; (git show <head>:<path>); writes nothing; prints to stdout.
;;
;;   cd /home/joe/code/futon2
;;   bb holes/labs/wm-contract/proof2/packets/h_value_t_d.clj
;;
;; Per target it runs the extractor's own steps in -main's order:
;; headings -> instance-sections -> extract -> consolidate (ids o-N) ->
;; filter-outcomes -> verify -> artefact-links. No --cascades directory
;; exists for these targets, so every instance section would be
;; {:absent :no-cascade}; the link count below counts (instance, outcome)
;; links, which artefact-links computes without wants.

(require '[clojure.edn :as edn]
         '[clojure.java.shell :as sh]
         '[clojure.string :as str])

(def root "/home/joe/code")
(def futon2 (str root "/futon2"))
(def fixture (str futon2 "/test/fixtures/target-field/target-field@futon2-7bd17dfb.edn"))
(def extractor (str futon2 "/scripts/wm/extract-outcomes.clj"))

(load-file extractor)   ; defines extract, consolidate, filter-outcomes, ... in user

(defn text-at [repo head path]
  (let [r (sh/sh "git" "-C" (str root "/" repo) "show" (str head ":" path))]
    (when (zero? (:exit r)) (:out r))))

(defn measure [text]
  (let [lines (count (str/split-lines text))
        hs (headings text)
        isecs (instance-sections hs lines)
        section-of (fn [l] (:instance (first (filter #(<= (first (:lines %)) l (second (:lines %))) isecs))))
        rows (extract text section-of {})
        consolidated (mapv #(assoc %1 :id (keyword (str "o-" (inc %2))))
                           (consolidate rows) (range))
        {admitted :outcomes rejected :rejected facets :facets} (filter-outcomes text consolidated)
        fails (verify text (concat admitted rejected facets))
        links (artefact-links text isecs admitted)]
    (cond-> {:cued-rows (count rows)
             :outcomes (count admitted)
             :rejected (count rejected)
             :facets (count facets)
             :instance-sections (count isecs)
             :links (count links)
             :linked-outcomes (count (distinct (map :outcome links)))
             :rules (frequencies (mapcat :rules (mapcat :cues admitted)))
             :whose (frequencies (map :whose admitted))}
      (seq fails) (assoc :refused :cue-does-not-resolve :failures (count fails))
      (and (empty? fails) (empty? admitted)) (assoc :absent :no-stated-outcome))))

(defn -run []
  (let [f (edn/read-string {:default tagged-literal} (slurp fixture))
        tf (get-in f [:decision :target-field])
        heads (get-in f [:read :heads])
        by-target (into {} (map (juxt :target identity)) (:considered tf))
        rows (vec (for [t (:feasible tf)
                        :let [c (by-target (:target t))
                              head (get heads (:repo c))
                              text (when (and c head) (text-at (:repo c) head (:path c)))]]
                    (merge {:target (:target t) :kind (:kind t) :repo (:repo c)
                            :path (:path c) :next-step (:next-step t)}
                           (if text
                             (try (measure text)
                                  (catch Exception e
                                    {:refused :extractor-threw :message (.getMessage e)}))
                             {:absent :text-not-at-recorded-head}))))
        dist (fn [k xs] (into (sorted-map) (frequencies (map k xs))))]
    (println ";; fixture" fixture)
    (println ";; fixture sha256" (sha256 (slurp fixture)))
    (println ";; extractor sha256" (sha256 (slurp extractor)))
    (println ";; heads" (pr-str (into (sorted-map) heads)))
    (println ";; feasible" (count rows) (pr-str (dist :kind rows)))
    (println)
    (println "== control: M-futon-seams (COMPLETE, not in the field) through the same harness")
    (let [head (str/trim (:out (sh/sh "git" "-C" (str root "/futon3c") "rev-parse" "HEAD")))]
      (prn (assoc (dissoc (measure (text-at "futon3c" head "holes/missions/M-futon-seams.md")) :rules :whose)
                  :futon3c-head head)))
    (println)
    (println "== partition")
    (prn {:with-outcomes (count (filter #(pos? (or (:outcomes %) 0)) rows))
          :no-stated-outcome (count (filter #(= :no-stated-outcome (:absent %)) rows))
          :refused (dist :refused (filter :refused rows))
          :text-absent (count (filter #(= :text-not-at-recorded-head (:absent %)) rows))})
    (println "== by kind: [kind with-outcomes of-n]")
    (doseq [[k xs] (group-by :kind rows)]
      (prn [k (count (filter #(pos? (or (:outcomes %) 0)) xs)) (count xs)]))
    (println "== distribution of admitted outcome counts {count targets}")
    (prn (dist :outcomes (remove :refused rows)))
    (println "== distribution of instance-section counts")
    (prn (dist :instance-sections (remove :refused rows)))
    (println "== distribution of served-by link counts")
    (prn (dist :links (remove :refused rows)))
    (println "== cue rules over admitted outcomes, whole field")
    (prn (into (sorted-map) (apply merge-with + (map :rules rows))))
    (println "== attribution of admitted outcomes, whole field")
    (prn (apply merge-with + (map :whose rows)))
    (println "== targets with >= 1 link")
    (doseq [r (filter #(pos? (or (:links %) 0)) rows)] (prn (select-keys r [:target :outcomes :instance-sections :links])))
    (println "== refused")
    (doseq [r (filter :refused rows)] (prn (select-keys r [:target :refused :failures :message])))
    (println "== top 15 by admitted outcomes")
    (doseq [r (take 15 (sort-by (comp - #(or (:outcomes %) 0)) rows))]
      (prn (select-keys r [:target :kind :outcomes :rejected :facets :links :next-step])))
    (println "== every row")
    (doseq [r rows] (prn (dissoc r :rules :whose :path)))))

(-run)
