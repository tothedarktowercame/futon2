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

(defn sha256-bytes [bytes]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (.update digest bytes)
    (format "%064x" (java.math.BigInteger. 1 (.digest digest)))))

(defn process [{:keys [dir bytes?]} & argv]
  (let [pb (ProcessBuilder. (into-array String argv))
        _ (when dir (.directory pb (io/file dir)))
        p (.start pb)
        out (if bytes?
              (let [stream (.getInputStream p)
                    sink (java.io.ByteArrayOutputStream.)]
                (io/copy stream sink)
                (.toByteArray sink))
              (slurp (.getInputStream p)))
        err (slurp (.getErrorStream p))
        exit (.waitFor p)]
    {:exit exit :out out :err err}))

(defn git-context [path]
  (let [file (.getCanonicalFile (io/file path))
        root-result (process {:dir (.getParent file)} "git" "rev-parse" "--show-toplevel")]
    (when (zero? (:exit root-result))
      (let [root (.trim ^String (:out root-result))
            relative (str (.relativize (.toPath (io/file root)) (.toPath file)))]
        {:root root :relative relative}))))

(defn historical-pin [path wanted-sha]
  (when-let [{:keys [root relative]} (git-context path)]
    (let [history (process {:dir root} "git" "log" "--format=%H" "--" relative)]
      (when (zero? (:exit history))
        (some (fn [commit]
                (let [blob (process {:dir root :bytes? true} "git" "show" (str commit ":" relative))]
                  (when (and (zero? (:exit blob))
                             (= wanted-sha (sha256-bytes (:out blob))))
                    commit)))
              (remove empty? (.split (.trim ^String (:out history)) "\\n")))))))

(defn pin-diagnosis [{:keys [id path sha256]}]
  (cond
    (not (.isFile (io/file path)))
    {:pin id :state :UNAVAILABLE :error :absent :path path}

    (= sha256 (sha256-file path))
    {:pin id :state :CURRENT :path path}

    :else
    (if-let [base (historical-pin path sha256)]
      (let [{:keys [root relative]} (git-context path)
            changes (process {:dir root} "git" "log" "--format=%H %s"
                             (str base "..HEAD") "--" relative)
            commits (if (zero? (:exit changes))
                      (vec (remove empty? (.split (.trim ^String (:out changes)) "\\n")))
                      [])]
        (cond-> {:pin id :state :PIN_BEHIND :error :pin-behind :path path
                 :pinned-commit base :spine-changing-commits commits
                 :distinguishable-cause? false
                 :reason :git-proves-content-change-not-landing-intent}
          (= id :lean-spine)
          (assoc :remedy
                 "Re-verify the Q-facing declarations after the Holes.lean binding, then refresh :lean-spine; do not accept the moved pin automatically.")))
      {:pin id :state :STALE_UNATTRIBUTED :error :stale :path path
       :reason :pinned-content-not-found-in-path-history})))

(defn validate [record]
  (let [defs (into {} (map (juxt :id identity) (:definitions record)))
        ports (into {} (map (juxt :id identity) (:interfaces record)))
        pin-states (mapv pin-diagnosis (:source-pins record))
        pin-errors (filter :error pin-states)
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
     :pin-states pin-states
     :errors errors}))

(defn prior-spine-sha [record]
  (let [{:keys [path]} (some #(when (= :lean-spine (:id %)) %) (:source-pins record))
        {:keys [root relative]} (git-context path)
        history (process {:dir root} "git" "log" "--format=%H" "--" relative)]
    (some (fn [commit]
            (let [blob (process {:dir root :bytes? true} "git" "show" (str commit ":" relative))]
              (when (and (zero? (:exit blob))
                         (not= (sha256-file path) (sha256-bytes (:out blob))))
                (sha256-bytes (:out blob)))))
          (remove empty? (.split (.trim ^String (:out history)) "\\n")))))

(defn -main [& args]
  (let [negative? (some #{"--negative-control" "--negative-pin-behind"} args)
        pin-negative? (some #{"--negative-pin-behind"} args)
        report-stale? (some #{"--allow-stale-report"} args)
        path (or (first (remove #{"--negative-control" "--negative-pin-behind"
                                 "--allow-stale-report"} args)) default-path)
        record (edn/read-string (slurp path))
        record (cond
                 pin-negative?
                 (let [prior (prior-spine-sha record)]
                   (when-not prior
                     (binding [*out* *err*]
                       (println "q-interface-completeness: cannot construct historical spine mutation"))
                     (System/exit 1))
                   (update record :source-pins
                           (fn [pins] (mapv #(if (= :lean-spine (:id %))
                                              (assoc % :sha256 prior) %) pins))))
                 negative?
                 (update record :interfaces
                         (fn [rows]
                           (mapv #(if (= :Q/producer-to-risk (:id %))
                                    (dissoc % :next-action)
                                    %)
                                 rows)))
                 :else record)
        result (validate record)
        expected-pin-behind? (some #(= :PIN_BEHIND (:state %)) (:pin-states result))
        reportable-stale?
        (and report-stale?
             (seq (:errors result))
             (every? #(contains? #{:pin-behind :stale} (:error %))
                     (:errors result)))
        accepted? (if pin-negative?
                    (and (not (:pass? result)) expected-pin-behind?)
                    (if negative?
                      (not (:pass? result))
                      (or (:pass? result) reportable-stale?)))]
    (println "q-interface-completeness:"
             (if accepted? "PASS" "FAIL")
             (pr-str result)
             (if reportable-stale?
               "mode=stale-pin-reportable"
               (if pin-negative?
               "negative-control=historical-spine-change-remains-red"
               (if negative?
                 "negative-control=missing-remediation-rejected"
                 "mode=positive")))
             "exit-convention=0-pass/1-fail/2-mutation-slipped")
    (System/exit (if accepted? 0 (if negative? 2 1)))))

(apply -main *command-line-args*)
