#!/usr/bin/env bb
(ns checks.closed-record-pointer-check
  (:require [babashka.fs :as fs]
            [cheshire.core :as json]
            [clojure.string :as str]))

(def contract-path "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json")
(def roots {"futon2" "/home/joe/code/futon2" "futon4" "/home/joe/code/futon4"
            "p4ng" "/home/joe/code/p4ng"})

(defn resolve-owner [owner]
  (or
   (when-let [[_ repo path] (re-find #"record: ([^: ]+):([^ ]+)" owner)]
     {:repo repo :path path :rule :explicit})
   (cond
     (str/includes? owner "P-glossary-mathematics")
     {:repo "p4ng" :path "sec-glossary.tex" :rule :glossary}
     (str/starts-with? owner "delivery-lifecycle")
     {:repo "futon4" :path "holes/delivery-lifecycle.md" :rule :lifecycle}
     (str/starts-with? owner "R19-preference-stack.edn")
     {:repo "futon2" :path "holes/labs/wm-contract/R19-preference-stack.edn" :rule :named-fixture}
     :else
     (when-let [[_ record] (re-find #"^(P-[A-Za-z0-9-]+)" owner)]
       {:repo "futon2" :path (str "holes/problems/" record ".md") :rule :problem-record}))))

(defn validate [contract]
  (let [closed (filter #(= "closed" (:kind %)) (:declarations contract))
        rows (mapv (fn [{:keys [name owner]}]
                     (let [{:keys [repo path] :as resolved} (resolve-owner owner)
                           root (get roots repo)
                           target (when (and root path) (fs/path root path))]
                       {:declaration name :owner owner :resolved resolved
                        :exists? (boolean (and target (fs/regular-file? target)))}))
                   closed)
        failures (filterv (complement :exists?) rows)]
    {:pass? (empty? failures) :closed (count rows)
     :resolved (- (count rows) (count failures))
     :failed (count failures) :failures failures}))

(defn mutate [contract]
  (update contract :declarations
          (fn [decls]
            (mapv #(if (and (= "closed" (:kind %)) (= "Channel" (:name %)))
                     (assoc % :owner "record: futon2:holes/problems/DOES-NOT-EXIST.md · P-R2") %)
                  decls))))

(defn -main [& args]
  (let [negative? (some #{"--negative"} args)
        contract (json/parse-string (slurp contract-path) true)
        report (validate (cond-> contract negative? mutate))]
    (println "closed-record-pointer-check:"
             (cond (and negative? (not (:pass? report))) "negative-control PASS"
                   negative? "mutation slipped" (:pass? report) "PASS" :else "FAIL")
             (pr-str report) "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit (cond (and negative? (not (:pass? report))) 0
                       negative? 2 (:pass? report) 0 :else 1))))

(apply -main *command-line-args*)
