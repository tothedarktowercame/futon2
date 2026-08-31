#!/usr/bin/env bb
(ns checks.r17-generator-disposer-check
  (:require [babashka.fs :as fs]
            [clojure.string :as str]))

(def default-a4a "src/futon2/aif/a4a.clj")
(def default-r17 "src/futon2/aif/r17_offline.clj")
(def default-live-paths ["scripts/futon2/report/war_machine.clj"])

(defn- sha256 [path]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes (slurp path) "UTF-8"))
    (format "%064x" (java.math.BigInteger. 1 (.digest digest)))))

(defn- require-read [path]
  (when-not (fs/regular-file? path)
    (throw (ex-info "required R17 guard input is absent" {:path path})))
  {:path path :sha256 (sha256 path) :text (slurp path)})

(defn wiring-verdict
  "A dormant unsafe reducer is recorded but cannot pass once live-reachable.
   A live candidate source must be independently generated and owned by a
   component distinct from the free-energy acceptor."
  [{:keys [live-reachable? candidate-source proposer acceptor]}]
  (cond
    (not live-reachable?) {:pass? true :status :dormant-guarded}
    (= :all-pairs-enumeration candidate-source)
    {:pass? false :status :reject
     :reason :all-pairs-is-not-an-independent-generator}
    (= proposer acceptor)
    {:pass? false :status :reject :reason :proposer-is-acceptor}
    (or (nil? candidate-source) (nil? proposer) (nil? acceptor))
    {:pass? false :status :reject :reason :untyped-wiring}
    :else {:pass? true :status :live-independent}))

(defn- source-facts [{a4a-text :text} {r17-text :text} live-reads]
  (let [all-pairs? (and (str/includes? a4a-text "pairs (for [i")
                        (str/includes? a4a-text "j (range (inc i)")
                        (str/includes? a4a-text "score-pair"))
        r17-reaches? (str/includes? r17-text "(a4a/reduce-concepts input)")
        live-hits (->> live-reads
                       (filter #(re-find #"(?i)\br17\b|reduce-concepts" (:text %)))
                       (mapv :path))]
    (when-not (and all-pairs? r17-reaches?)
      (throw (ex-info "R17 source shape changed; classification must be reviewed"
                      {:all-pairs-detected? all-pairs?
                       :r17-reaches-reducer? r17-reaches?})))
    {:candidate-source :all-pairs-enumeration
     :proposer :count-only-bmr
     :acceptor :count-only-bmr
     :r17-reaches-reducer? r17-reaches?
     :live-reachable? (boolean (seq live-hits))
     :live-reference-paths live-hits}))

(defn run-check [{:keys [a4a r17 live-paths negative?]}]
  (let [a4a-read (require-read (or a4a default-a4a))
        r17-read (require-read (or r17 default-r17))
        live-reads (mapv require-read (or live-paths default-live-paths))
        facts (source-facts a4a-read r17-read live-reads)
        evaluated (wiring-verdict (cond-> facts negative?
                                    (assoc :live-reachable? true)))
        checks (if (:pass? evaluated) []
                   [{:check :generator-disposer-independence
                     :reason (:reason evaluated)}])]
    {:summary {:pass? (:pass? evaluated)
               :status (:status evaluated)
               :failures (count checks)}
     :invariant {:statement "Embedding proposes; free energy disposes."
                 :candidate-source (:candidate-source facts)
                 :proposer (:proposer facts)
                 :acceptor (:acceptor facts)
                 :distinct? (not= (:proposer facts) (:acceptor facts))}
     :reachability (select-keys facts [:r17-reaches-reducer? :live-reachable?
                                      :live-reference-paths])
     :activation {:trigger "Any production scheduler, server, or war-machine entrypoint references R17 or reduce-concepts."
                  :required-action "Run this guard as a required pre-merge check; live all-pairs wiring must remain red until an independent generator is declared."}
     :reads (mapv #(select-keys % [:path :sha256])
                  (concat [a4a-read r17-read] live-reads))
     :checks checks}))

(defn- parse-args [args]
  (loop [xs args out {}]
    (if (empty? xs) out
        (if (= "--negative" (first xs))
          (recur (rest xs) (assoc out :negative? true))
          (do
            (when-not (second xs)
              (throw (ex-info "arguments must be --key value pairs" {})))
            (recur (nnext xs)
                   (assoc out (keyword (str/replace (first xs) #"^--" ""))
                          (second xs))))))))

(defn -main [& args]
  (let [{:keys [negative? report] :as opts} (parse-args args)]
    (when-not report
      (binding [*out* *err*]
        (println "usage: r17_generator_disposer_check.clj [--negative] --report FILE"))
      (System/exit 2))
    (let [result (try (run-check opts)
                      (catch Exception e
                        {:summary {:pass? false :status :read-error :failures 1}
                         :checks [{:check :guard-input :reason :absent-or-unclassified
                                   :message (.getMessage e)
                                   :data (ex-data e)}]}))]
      (when-not negative?
        (when-let [parent (fs/parent report)] (fs/create-dirs parent))
        (spit report (str (pr-str result) "\n")))
      (if negative?
        (if (and (not (get-in result [:summary :pass?]))
                 (= :all-pairs-is-not-an-independent-generator
                    (get-in result [:checks 0 :reason])))
          (do (println "r17-generator-disposer: PASS negative all-pairs live wiring rejected exit-convention=0-pass/1-fail/2-mutation-slipped")
              (System/exit 0))
          (do (println "r17-generator-disposer: FAIL negative all-pairs mutation slipped exit-convention=0-pass/1-fail/2-mutation-slipped")
              (System/exit 2)))
        (do (println (str "r17-generator-disposer: "
                          (if (get-in result [:summary :pass?]) "PASS" "FAIL")
                          " status=" (name (get-in result [:summary :status]))
                          " exit-convention=0-pass/1-fail/2-mutation-slipped"))
            (System/exit (if (get-in result [:summary :pass?]) 0 1)))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
