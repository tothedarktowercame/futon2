#!/usr/bin/env bb
(ns checks.hyper-edge-domain-range-check
  (:require [clojure.edn :as edn]
            [clojure.set :as set]))

(def default-schema
  "/home/joe/code/p4ng/empirics-futon/hyper-edge-schema.edn")
(def negative-fixture
  "/home/joe/code/futon2/checks/fixtures/hyper_edge_domain_range_4_of_96.edn")

(defn- read-edn [path] (edn/read-string (slurp path)))

(defn- declaration-errors [kind declaration]
  (cond
    (not (map? declaration))
    [{:error :missing-declaration :kind kind}]

    (nil? (:population declaration))
    [{:error :missing-population :kind kind}]

    (not (map? (:enumeration declaration)))
    [{:error :missing-enumeration :kind kind}]

    (not (#{:enumerated :unknown} (get-in declaration [:enumeration :status])))
    [{:error :invalid-enumeration-status
      :kind kind
      :actual (get-in declaration [:enumeration :status])}]

    (and (= :unknown (get-in declaration [:enumeration :status]))
         (nil? (get-in declaration [:enumeration :reason])))
    [{:error :unknown-without-reason :kind kind}]

    (and (= :enumerated (get-in declaration [:enumeration :status]))
         (not (vector? (get-in declaration [:enumeration :members]))))
    [{:error :enumeration-without-members :kind kind}]

    :else []))

(defn- coverage-result [port]
  (let [domain (:domain port)
        claim (:claim domain)
        enum (:enumeration domain)
        coverage (:coverage domain)]
    (cond
      (= :unknown claim)
      {:status :unknown :reason (or (:reason coverage)
                                   "domain coverage not established")}

      (not (map? coverage))
      {:status :fail :error :missing-coverage-enumeration}

      (= :unknown (:status coverage))
      {:status :unknown :reason (:reason coverage)}

      (not= :enumerated (:status coverage))
      {:status :fail :error :invalid-coverage-status :actual (:status coverage)}

      (= :all claim)
      (let [required (set (:members enum))
            covered (set (:members coverage))
            missing (set/difference required covered)
            counts (:event-counts enum)
            total-events (or (:event-count enum)
                             (when counts (reduce + 0 (vals counts))))
            missing-events (when counts
                             (reduce + 0 (map #(get counts % 0) missing)))
            covered-events (when total-events (- total-events missing-events))]
        (if (empty? missing)
          {:status :pass
           :covered-members (count required)
           :required-members (count required)
           :covered-events covered-events
           :required-events total-events}
          {:status :fail
           :error :domain-coverage-gap
           :missing (vec (sort missing))
           :covered-members (- (count required) (count missing))
           :required-members (count required)
           :covered-events covered-events
           :required-events total-events}))

      (= :subset claim)
      (let [required (set (:members enum))
            covered (set (:members coverage))
            outside (set/difference covered required)]
        (if (empty? outside)
          {:status :pass
           :note :declared-subset-does-not-claim-total-coverage
           :covered-members (count covered)
           :required-members (count required)}
          {:status :fail
           :error :coverage-outside-declared-domain
           :outside (vec (sort outside))}))

      :else
      {:status :fail :error :invalid-domain-claim :actual claim})))

(defn validate-port [edge-id port]
  (let [errors (vec (concat (declaration-errors :domain (:domain port))
                            (declaration-errors :range (:range port))))
        coverage (when (empty? errors) (coverage-result port))
        pass? (and (empty? errors) (not= :fail (:status coverage)))]
    {:pass? pass?
     :edge/id edge-id
     :port/id (:port/id port)
     :domain-status (get-in port [:domain :enumeration :status])
     :range-status (get-in port [:range :enumeration :status])
     :coverage coverage
     :errors errors}))

(defn -main [& args]
  (let [negative? (some #{"--negative"} args)
        ports (if negative?
                (let [fixture (read-edn negative-fixture)]
                  [[(:fixture/id fixture) fixture]])
                (for [edge (:instances (read-edn default-schema))
                      port (:ports edge)]
                  [(:edge/id edge) port]))
        results (mapv (fn [[edge-id port]] (validate-port edge-id port)) ports)
        passed (count (filter :pass? results))
        enumerated-domains (count (filter #(= :enumerated (:domain-status %)) results))
        unknown-domains (count (filter #(= :unknown (:domain-status %)) results))
        enumerated-ranges (count (filter #(= :enumerated (:range-status %)) results))
        unknown-ranges (count (filter #(= :unknown (:range-status %)) results))]
    (doseq [result results] (println (pr-str result)))
    (println (str "hyper-edge-domain-range-check: "
                  (if negative?
                    (if (not= passed (count results))
                      "negative-control PASS (coverage gap rejected)"
                      "negative-control FAIL (mutation slipped)"
                      )
                    (if (= passed (count results)) "positive PASS" "positive FAIL"))
                  " ports=" (count results)
                  " conforming=" passed
                  " rejected=" (- (count results) passed)
                  " domain-enumerated=" enumerated-domains
                  " domain-unknown=" unknown-domains
                  " range-enumerated=" enumerated-ranges
                  " range-unknown=" unknown-ranges
                  " exit-convention=0-pass/1-fail/2-mutation-slipped"))
    (if negative?
      (System/exit (if (= passed (count results)) 2 0))
      (System/exit (if (= passed (count results)) 0 1)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
