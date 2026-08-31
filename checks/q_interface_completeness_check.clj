#!/usr/bin/env bb
(ns checks.q-interface-completeness-check
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def default-path "holes/labs/wm-contract/Q-interface-completeness.edn")
(def required-definition-ids
  #{:lean/Q-carrier :lean/Q-machine-construction :lean/Q-risk-consumer
    :lean/Q-eig-consumer :lean/Q-reference-fixture :runtime/Q-action-proxy
    :runtime/Q-policy})
(def required-interface-ids
  #{:Q/in-belief :Q/in-candidates :Q/in-policy-depth :Q/producer-to-risk
    :Q/risk-to-ranking :Q/to-EIG})

(defn sha256-file [path]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream path)]
      (let [buf (byte-array 8192)]
        (loop []
          (let [n (.read in buf)]
            (when (pos? n)
              (.update digest buf 0 n)
              (recur))))))
    (format "%064x" (java.math.BigInteger. 1 (.digest digest)))))

(defn validate [record]
  (let [defs (into {} (map (juxt :id identity) (:definitions record)))
        ports (into {} (map (juxt :id identity) (:interfaces record)))
        pin-errors
        (mapcat (fn [{:keys [id path sha256]}]
                  (cond
                    (not (.isFile (io/file path))) [{:pin id :error :absent :path path}]
                    (not= sha256 (sha256-file path)) [{:pin id :error :stale :path path}]
                    :else []))
                (:source-pins record))
        missing-defs (remove #(contains? defs %) required-definition-ids)
        missing-ports (remove #(contains? ports %) required-interface-ids)
        unowned-gaps
        (for [[id row] ports
              :when (and (#{:underpowered :missing-wire} (:effect row))
                         (or (not (string? (:blocker row)))
                             (not (string? (:next-action row)))))]
          {:interface id :error :gap-without-action})
        machine-q (get defs :lean/Q-machine-construction)
        false-completion
        (when (and (not= :missing (:status machine-q))
                   (nil? (:implementation-witness machine-q)))
          [{:definition :lean/Q-machine-construction
            :error :completion-without-implementation-witness}])
        errors (vec (concat pin-errors
                            (map #(hash-map :definition % :error :missing) missing-defs)
                            (map #(hash-map :interface % :error :missing) missing-ports)
                            unowned-gaps false-completion))]
    {:pass? (empty? errors)
     :definitions (count defs)
     :interfaces (count ports)
     :underpowered (count (filter #(= :underpowered (:effect %)) (vals ports)))
     :missing-wires (count (filter #(= :missing-wire (:effect %)) (vals ports)))
     :errors errors}))

(defn -main [& args]
  (let [negative? (some #{"--negative-control"} args)
        path (or (first (remove #{"--negative-control"} args)) default-path)
        record (edn/read-string (slurp path))
        record (if negative?
                 (update record :interfaces
                         (fn [rows]
                           (mapv #(if (= :Q/producer-to-risk (:id %))
                                    (dissoc % :next-action)
                                    %)
                                 rows)))
                 record)
        result (validate record)
        accepted? (if negative? (not (:pass? result)) (:pass? result))]
    (println "q-interface-completeness:"
             (if accepted? "PASS" "FAIL")
             (pr-str result)
             (if negative?
               "negative-control=missing-remediation-rejected"
               "mode=positive")
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit (if accepted? 0 (if negative? 2 1)))))

(apply -main *command-line-args*)
