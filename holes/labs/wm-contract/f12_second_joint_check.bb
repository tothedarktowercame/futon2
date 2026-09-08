#!/usr/bin/env bb
;; F12 slice 12 -- compare a second snatch joint with the ruled O3 signature.
(require '[clojure.edn :as edn]
         '[clojure.java.shell :as sh]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def root "/home/joe/code")
(def futon3 (str root "/futon3"))
(def mathlib (str root "/mathlib4"))
(def pin "f8dd164bdc7f20a160b5e6b758d71532be65ca1e")
(def out "runs/F12-organise/26-second-joint.edn")
(def lean-path (str mathlib "/DarkTower/WarMachine/F12SecondSnatchExemplar.lean"))
(def findings (atom []))
(defn fail! [k x] (swap! findings conj (sorted-map :detail x :finding k)))

(defn expression [fixture]
  (str "(load-file \"checks/find_organise.clj\") "
       "(require '[clojure.edn :as edn]) "
       "(let [n (find-ns 'find-organise) r #(deref (ns-resolve n %)) "
       "fx (edn/read-string (slurp \"" fixture "\")) "
       "repo ((r 'read-repository) \"library\" [:snatch])] "
       "(prn ((r 'cascade-diff-table) fx repo)))"))

(defn derive-rows [fixture]
  (let [p (sh/sh "bb" "-cp" "checks" "-e" (expression fixture) :dir futon3)]
    (if (zero? (:exit p)) (edn/read-string (:out p))
        (do (fail! :subprocess {:exit (:exit p) :err (:err p)}) []))))

(defn row-for [rows scenario] (first (filter #(= scenario (:scenario %)) rows)))
(defn ruled-result [row]
  (let [added (:added-by-organise row)
        recorded (set (map vec (:edges row)))
        kept (set (remove (fn [[u v]] (or (contains? added u) (contains? added v))) recorded))]
    (sorted-map :added-by-organise (vec (sort-by pr-str added))
                :added-count (count added)
                :recorded-edge-count (count recorded)
                :recorded-edges (vec (sort-by pr-str recorded))
                :ruled-recorded-edges (vec (sort-by pr-str kept))
                :ruled-recorded-edge-count (count kept)
                :scenario (:scenario row)
                :selected (vec (sort-by pr-str (:selected row))))))

(def rows (derive-rows "checks/snatch-cascade.edn"))
(def first-result (ruled-result (row-for rows [:g4 :snatcher])))
(def second-result (ruled-result (row-for rows [:g2 :snatcher])))
(def lean (slurp lean-path))
(def required-decls ["organiseSnatchG2Conformant" "snatchG2NoRecordedEdgeSurvives"
                     "snatchG2RecordedEdgeCount" "organiseSnatchG2NotEdge39"])
(def declarations-present? (every? #(str/includes? lean (str "theorem " %)) required-decls))

(defn verdict [a b decls?]
  (and (= 10 (:recorded-edge-count a)) (= 1 (:ruled-recorded-edge-count a))
       (= 5 (:recorded-edge-count b)) (zero? (:ruled-recorded-edge-count b))
       (pos? (:added-count b)) decls?))

(def plants
  [(sorted-map :plant :second-added-emptied
               :landed? (pos? (:added-count second-result))
               :verdict-after (verdict first-result (assoc second-result :added-count 0) declarations-present?))
   (sorted-map :plant :second-survivor-invented
               :landed? (zero? (:ruled-recorded-edge-count second-result))
               :verdict-after (verdict first-result (assoc second-result :ruled-recorded-edge-count 1) declarations-present?))
   (sorted-map :plant :lean-declaration-absent
               :landed? declarations-present?
               :verdict-after (verdict first-result second-result false))])

(doseq [p plants]
  (when-not (and (:landed? p) (false? (:verdict-after p)))
    (fail! :plant-did-not-fail p)))
(when-not (= pin (str/trim (:out (sh/sh "git" "-C" futon3 "rev-parse" "HEAD"))))
  (fail! :futon3-pin-moved pin))
(when-not (verdict first-result second-result declarations-present?)
  (fail! :comparison-failed {:first first-result :second second-result}))

(def artifact
  (sorted-map
   :comparison :one-of-ten-is-particular-not-characteristic
   :findings (vec @findings)
   :first-joint first-result
   :futon3-pin pin
   :lean-declarations required-decls
   :mutation-plants plants
   :record :F12-slice-12-second-joint
   :second-joint second-result
   :source-pointers ["futon3:checks/find_organise.clj:603-633"
                     "futon2:holes/labs/wm-contract/runs/F12-organise/23-snatch-joint.edn:111-157"
                     "mathlib4:DarkTower/WarMachine/F12SecondSnatchExemplar.lean:12-67"]
   :verdict (if (empty? @findings) :pass :fail)))

(spit out (with-out-str (pp/pprint artifact)))
(println "f12_second_joint_check:" (name (:verdict artifact)) out)
(when (= :fail (:verdict artifact)) (System/exit 1))
