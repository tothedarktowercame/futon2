#!/usr/bin/env bb
(require '[clojure.edn :as edn] '[clojure.string :as str])
(def path (str (.getParent (.getAbsoluteFile (java.io.File. *file*))) "/worklist.edn"))
(def w (edn/read-string (slurp path)))
(def items (:items w))
(def by-id (into {} (map (juxt :id identity) items)))
(defn loopable? [i] (and (= :open (:status i)) (not= :J (:class i))
                          (not= :joe (:owner i)) (not (:loop-skip i))))
(def cmd (first *command-line-args*))
(case cmd
  "next-open" (println (or (some-> (first (filter loopable? items)) :id name) "NONE"))
  ;; stall-key hashes the WHOLE row (wm loop e84c114e, 2026-09-08): the
  ;; select-keys form lost the same race three times across the two loops --
  ;; a slice recorded into a key the list did not name and three healthy
  ;; iterations read as a stall. Any row edit is evidence of life.
  "stall-key" (println (or (some-> (first (filter loopable? items))
                                    ((fn [i] (str (name (:id i)) ":" (name (:status i)) ":" (hash i)))))
                           "NONE"))
  "unreviewed" (println (str/join " " (map (comp name :id) (filter #(= :done-unreviewed (:status %)) items))))
  "counts" (println (frequencies (map :status items)))
  "unblock" (doseq [r (filter #(and (= :blocked (:status %)) (seq (:depends-on %))
                                     (every? (fn [d] (= :done (:status (by-id d)))) (:depends-on %))
                                     (not= :joe (:owner %))) items)]
              (let [s (slurp path)
                    old (str "{:id " (:id r) " :class " (:class r) " :status :blocked")
                    new (str "{:id " (:id r) " :class " (:class r) " :status :open :unblocked-by \"library-build-loop: dependencies done\"")]
                (when-not (str/includes? s old) (throw (ex-info "row header not found" {:id (:id r)})))
                (spit path (str/replace-first s old new))
                (println "unblocked" (name (:id r)))))
  (do (println "usage: library_step.bb next-open|unblock|unreviewed|counts|stall-key") (System/exit 2)))
