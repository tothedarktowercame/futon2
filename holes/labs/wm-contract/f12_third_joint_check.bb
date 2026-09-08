#!/usr/bin/env bb
;; F12 slice 13 -- drive the next joint row through the ruled O3 signature.
(require '[clojure.edn :as edn]
         '[clojure.java.shell :as sh]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def root "/home/joe/code")
(def futon3 (str root "/futon3"))
(def mathlib (str root "/mathlib4"))
(def pin "f8dd164bdc7f20a160b5e6b758d71532be65ca1e")
(def out "runs/F12-organise/27-third-joint.edn")
(def lean-path (str mathlib "/DarkTower/WarMachine/F12ThirdSnatchExemplar.lean"))
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
(def result (ruled-result (row-for rows [:g1 :cautious])))
(def lean (slurp lean-path))
(def required-decls ["organiseSnatchG1CautiousConformant"
                     "snatchG1CautiousNoRecordedEdgeSurvives"
                     "snatchG1CautiousRecordedEdgeCount"
                     "organiseSnatchG1CautiousNotEdge518"])
(def declarations-present? (every? #(str/includes? lean (str "theorem " %)) required-decls))

(defn verdict [r decls?]
  (and (= [:g1 :cautious] (:scenario r))
       (= 3 (:recorded-edge-count r))
       (zero? (:ruled-recorded-edge-count r))
       (pos? (:added-count r)) decls?))

(def plants
  [(sorted-map :plant :added-emptied :landed? (pos? (:added-count result))
               :verdict-after (verdict (assoc result :added-count 0) declarations-present?))
   (sorted-map :plant :survivor-invented :landed? (zero? (:ruled-recorded-edge-count result))
               :verdict-after (verdict (assoc result :ruled-recorded-edge-count 1) declarations-present?))
   (sorted-map :plant :lean-declaration-absent :landed? declarations-present?
               :verdict-after (verdict result false))])

(doseq [p plants]
  (when-not (and (:landed? p) (false? (:verdict-after p)))
    (fail! :plant-did-not-fail p)))
(when-not (= pin (str/trim (:out (sh/sh "git" "-C" futon3 "rev-parse" "HEAD"))))
  (fail! :futon3-pin-moved pin))
(when-not (verdict result declarations-present?)
  (fail! :comparison-failed result))

(def artifact
  (sorted-map :finding :third-row-also-loses-every-recorded-edge
              :findings (vec @findings)
              :futon3-pin pin
              :lean-declarations required-decls
              :mutation-plants plants
              :record :F12-slice-13-third-joint
              :source-pointers ["futon3:checks/find_organise.clj:603-633"
                                "futon2:holes/labs/wm-contract/runs/F12-organise/23-snatch-joint.edn:111-157"
                                "mathlib4:DarkTower/WarMachine/F12ThirdSnatchExemplar.lean:13-57"]
              :third-joint result
              :verdict (if (empty? @findings) :pass :fail)))

(spit out (with-out-str (pp/pprint artifact)))
(println "f12_third_joint_check:" (name (:verdict artifact)) out)
(when (= :fail (:verdict artifact)) (System/exit 1))
