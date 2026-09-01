#!/usr/bin/env bb
(ns checks.obligation-ledger-reconciliation-check
  (:require [babashka.fs :as fs]
            [clojure.string :as str]))

(def default-ledger
  "/home/joe/code/p4ng/vetting/OBLIGATIONS-REVERIFY-2026-08-31.md")
(def table-end "<!-- CURRENT TABLE END -->")
(def claim :content-current)
(def canonical-obligations
  (set (concat (map #(str "O" %) (range 1 23)) ["O1c" "O1d"])))

(defn- sha256 [text]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes text "UTF-8"))
    (format "%064x" (java.math.BigInteger. 1 (.digest digest)))))

(defn- ids [label]
  (->> (re-seq #"O\d+[a-z]?" label)
       (filter canonical-obligations)
       distinct vec))

(defn- current-rows [text]
  (let [marker (.indexOf text table-end)]
    (when (neg? marker)
      (throw (ex-info "current-table sentinel is absent" {:sentinel table-end})))
    (->> (subs text 0 marker)
         str/split-lines
         (keep (fn [line]
                 (when-let [[_ label status evidence]
                            (re-matches #"\|\s*\*\*([^*]+)\*\*\s*\|\s*\*\*([^*]+)\*\*\s*\|\s*(.*)\|" line)]
                   {:ids (ids label) :label label :status status :evidence evidence})))
         (filter (comp seq :ids))
         vec)))

(defn- sections [text]
  (->> (str/split text #"(?m)(?=^## )")
       (keep (fn [section]
               (when-let [[_ title] (re-find #"(?m)^## ([^\n]+)" section)]
                 [title section])))
       (into {})))

(defn check-text [text]
  (let [rows (current-rows text)
        section-map (sections (subs text (+ (.indexOf text table-end)
                                           (count table-end))))
        flattened (mapcat (fn [row] (map #(assoc row :id %) (:ids row))) rows)
        duplicates (->> flattened (group-by :id)
                        (keep (fn [[id xs]] (when (> (count xs) 1) id))) sort vec)
        present (set (map :id flattened))
        row-checks
        (mapcat
         (fn [{:keys [id status evidence]}]
           (let [heading (second (re-find #"See “([^”]+)” below" evidence))
                 section (get section-map heading)]
             (cond-> []
               (not= "closure-verified" status)
               (conj {:check :current-status :obligation id
                      :expected :closure-verified :actual (keyword status)})
               (nil? heading)
               (conj {:check :dated-section-reference :obligation id
                      :reason :missing-reference})
               (and heading (nil? section))
               (conj {:check :dated-section-reference :obligation id
                      :reason :heading-not-found :heading heading})
               (and section
                    (not (and (str/includes? section id)
                              (str/includes? section "closure-verified"))))
               (conj {:check :dated-section-evidence :obligation id
                      :reason :closure-not-recorded-in-referenced-section
                      :heading heading}))))
         flattened)
        checks (vec
                (concat
                 (when (not= canonical-obligations present)
                   [{:check :current-population
                     :missing (vec (sort (remove present canonical-obligations)))
                     :extra (vec (sort (remove canonical-obligations present)))}])
                 (when (seq duplicates)
                   [{:check :duplicate-current-row :obligations duplicates}])
                 row-checks))]
    {:summary {:pass? (empty? checks)
               :current-rows (count rows)
               :obligations (count present)
               :closure-verified (count (filter #(= "closure-verified" (:status %))
                                                flattened))
               :failures (count checks)}
     :consumer {:role :paper-vetting-reviewer
                :use :reject-current-table-history-disagreement}
     :checks checks}))

(defn run-check [{:keys [ledger negative?]}]
  (let [path (or ledger default-ledger)]
    (when-not (fs/regular-file? path)
      (throw (ex-info "obligation ledger is absent" {:path path})))
    (let [original (slurp path)
          text (if negative?
                 (str/replace-first original
                                    "| **closure-verified** |"
                                    "| **still-open** |")
                 original)]
      (assoc (check-text text)
             :claim claim
             :reads [{:path path :sha256 (sha256 original)}]
             :negative-mutation (when negative? :one-current-row-reverted-to-still-open)))))

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
        (println "usage: obligation_ledger_reconciliation_check.clj [--negative] [--ledger FILE] --report FILE"))
      (System/exit 2))
    (let [result (try (run-check opts)
                      (catch Exception e
                        {:summary {:pass? false :failures 1}
                         :checks [{:check :checker :reason :read-or-parse-error
                                   :message (.getMessage e) :data (ex-data e)}]}))]
      (when-not negative?
        (when-let [parent (fs/parent report)] (fs/create-dirs parent))
        (spit report (str (pr-str result) "\n")))
      (if negative?
        (if (and (not (get-in result [:summary :pass?]))
                 (some #(= :current-status (:check %)) (:checks result)))
          (do (println "obligation-ledger-reconciliation: PASS negative stale-status mutation rejected exit-convention=0-pass/1-fail/2-mutation-slipped")
              (System/exit 0))
          (do (println "obligation-ledger-reconciliation: FAIL negative mutation slipped exit-convention=0-pass/1-fail/2-mutation-slipped")
              (System/exit 2)))
        (do (println (str "obligation-ledger-reconciliation: "
                          (if (get-in result [:summary :pass?]) "PASS" "FAIL")
                          " obligations=" (get-in result [:summary :obligations])
                          " exit-convention=0-pass/1-fail/2-mutation-slipped"))
            (System/exit (if (get-in result [:summary :pass?]) 0 1)))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
