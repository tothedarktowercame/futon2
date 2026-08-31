#!/usr/bin/env bb
(ns checks.preemptive-repair-lint
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def repos {:futon2 "/home/joe/code/futon2"
            :futon3 "/home/joe/code/futon3"
            :p4ng "/home/joe/code/p4ng"})

(def text-exts #{"clj" "bb" "sh" "py" "md" "edn" "tex"})
(def specimen-region #"(?s)PREEMPTIVE-REPAIR-SPECIMENS-BEGIN.*?PREEMPTIVE-REPAIR-SPECIMENS-END")
(defn mask-specimens [text]
  ;; Preserve newlines so findings after a specimen retain their real line.
  (str/replace text specimen-region
               #(str/replace % #"[^\n]" " ")))
(defn- tracked [root]
  (-> (shell {:out :string :err :string} "git" "-C" root "ls-files") :out
      str/split-lines))
(defn- readable [root rel]
  (let [p (fs/path root rel)]
    (when (and (fs/regular-file? p)
               (contains? text-exts (fs/extension p))
               (< (fs/size p) 2000000))
      (mask-specimens (slurp (str p))))))
(defn- corpus []
  (for [[repo root] repos, rel (tracked root), :let [text (readable root rel)], :when text]
    {:repo repo :root root :path rel :text text}))
(defn- line-no [text offset]
  (inc (count (re-seq #"\n" (subs text 0 offset)))))
(defn- matcher-results [re text]
  (let [m (re-matcher re text)]
    (loop [out []]
      (if (.find m)
        (recur (conj out {:start (.start m) :match (.group m)
                          :groups (mapv #(.group m %) (range 1 (inc (.groupCount m))))}))
        out))))
(defn- matches [row re finding]
  (for [{:keys [start match]} (matcher-results re (:text row))]
    (assoc (select-keys row [:repo :path]) :line (line-no (:text row) start)
           :finding finding :excerpt (subs match 0 (min 180 (count match))))))

(defn acceptance-findings [rows]
  (vec (mapcat #(matches % #"(?is)(?:mismatch(?:es)?|findings?)\s*[:=]?\s*[1-9][0-9]*.{0,180}?exit(?:ed|\s+code)?\s*0|exit(?:ed|\s+code)?\s*0.{0,180}?(?:mismatch(?:es)?|findings?)\s*[:=]?\s*[1-9][0-9]*" :nonzero-finding-zero-exit) rows)))

(def path-token #"`((?:checks|holes|src|test|vetting|empirics)/[^` :]+\.[A-Za-z0-9]+)`")
(defn- tracked? [root rel]
  (zero? (:exit (shell {:out :string :err :string :continue true}
                       "git" "-C" root "ls-files" "--error-unmatch" rel))))
(defn artefact-findings [rows]
  (vec
   (concat
    (mapcat
     (fn [{:keys [repo root path text]}]
       (for [{:keys [start groups]} (matcher-results path-token text)
             :let [rel (first groups)]
             :when (and (fs/exists? (fs/path root rel)) (not (tracked? root rel)))]
         {:repo repo :path path :line (line-no text start)
          :finding :citation-to-untracked-path :target rel})) rows)
    ;; Prose may legitimately describe the staging/publishing distinction.  Only
    ;; executable shell is suspect, and only when its publication assertion is
    ;; in the same local branch as --stage.
    (mapcat (fn [row]
              (remove #(str/includes? (str/lower-case (:excerpt %)) "not published")
                      (matches row #"(?is)(?:--stage|STAGE).{0,240}(?:echo|printf).{0,80}publish(?:ed)?" :stage-described-as-published)))
            (filter #(str/ends-with? (:path %) ".sh") rows))
    (for [[stem svg pdf] [["paper" "aif-control-map-paper.svg" "aif-control-map-paper.pdf"]
                          ["futon" "aif-control-map-futon.svg" "aif-control-map-futon.pdf"]]
          :let [root (:p4ng repos)
                sm (fs/file-time->millis (fs/last-modified-time (fs/path root svg)))
                pm (fs/file-time->millis (fs/last-modified-time (fs/path root pdf)))]
          :when (> sm pm)]
      {:repo :p4ng :path pdf :finding :derived-older-than-source :source svg :pair stem}))))

(defn stale-baseline-findings [rows]
  (vec
   (mapcat #(matches % #"(?mi)(?:\(is\s+\(=\s+[0-9]+\s+\(count\s+(?:live[-_]|[^\n)]*(?:contract-corpus|control[-_]map-corpus))[^\n]*|assert(?:-equals)?\s*\(?\s*[0-9]+[^\n]*(?:live[-_]|contract-corpus|control[-_]map-corpus))" :exact-count-over-live-population)
           (filter #(re-find #"(?:^|/)(?:test|checks)/" (:path %)) rows))))

(defn absence-findings [_rows]
  (let [census (edn/read-string (slurp "/home/joe/code/futon2/holes/labs/wm-contract/C12-absence-census.edn"))
        dispositions (edn/read-string (slurp "/home/joe/code/futon2/checks/absence-coercion-dispositions.edn"))
        by-at (into {} (map (juxt :at identity) (:rows dispositions)))
        unsafe (->> (:sites census)
                    (remove #(contains? #{:explicit-model-initialisation :explicit-configuration-default
                                          :sparse-algebra-identity :model-identity :explicit-adapter-policy}
                                        (:status %)))
                    (filter #(or (= false (:consumer-could-tell? %))
                                 (= false (:consumer-could-tell-before? %)))))]
    (when-not (= (set (map :at unsafe)) (set (keys by-at)))
      (throw (ex-info "absence dispositions do not cover the live C12 population"
                      {:census (set (map :at unsafe)) :dispositions (set (keys by-at))})))
    (->> unsafe
         (filter #(= :blocked (:disposition (by-at (:at %)))))
         (mapv #(hash-map :repo :futon2 :path (:at %) :finding :absence-coerced
                          :disposition :blocked :blocked-on (:blocked-on (by-at (:at %)))
                          :input (:input %) :becomes (:becomes %))))))

(defn era-findings [rows]
  (vec
   (mapcat #(matches % #"(?mi)(?:\(is\s+\(=\s+[0-9]+[^\n]{0,180}(?:timestamp|recorded-at|startedAt)|assert(?:-equals)?\s*\(?\s*[0-9]+[^\n]{0,180}(?:timestamp|recorded-at|startedAt))(?![^\n]*(?:era|schema-version|boundary|as-of))" :timestamped-population-without-era)
           (filter #(re-find #"(?:^|/)(?:test|checks)/" (:path %)) rows))))

(defn record-findings [rows]
  (vec
   (concat
    (mapcat
     (fn [row]
       (when (and (re-find #":ratification" (:text row))
                  (re-find #":disagreements\s*\[(?s:.+?)\]" (:text row))
                  (not (re-find #":disagreements\s*\[\s*\]" (:text row))))
         [(merge (select-keys row [:repo :path]) {:finding :ratified-and-disputed})])) rows)
    ;; Amendment prose is history, not a second current status.  The explicit
    ;; sentinel bounds the authoritative table; C51 established this form.
    (mapcat
     (fn [{:keys [repo path text]}]
       (let [current (first (str/split text #"<!-- CURRENT TABLE END -->" 2))
             entries (for [{:keys [start groups]} (matcher-results #"(?m)^\|\s*\*\*(O[0-9]+[a-z]?)\*\*\s*\|\s*\*\*([^*]+)\*\*" current)]
                       {:id (first groups) :status (second groups) :line (line-no current start)})]
         (for [[id xs] (group-by :id entries)
               :when (> (count (set (map :status xs))) 1)]
           {:repo repo :path path :line (:line (first xs))
            :finding :duplicate-current-status :record id
            :statuses (vec (distinct (map :status xs)))}))) rows))))

(def scanners {:acceptance acceptance-findings :artefact artefact-findings
               :stale-baseline stale-baseline-findings :absence absence-findings
               :era era-findings :record record-findings})

;; PREEMPTIVE-REPAIR-SPECIMENS-BEGIN
(def negative-text
  {:acceptance "findings=3; process exit 0"
   :artefact "#!/bin/sh\nif [ \"$1\" = --stage ]; then echo published; fi"
   :stale-baseline "(is (= 42 (count live-contract)))"
   :era "(is (= 14 (count timestamp-records)))"
   :record ":ratification [:x] :disagreements [:x]"})
;; PREEMPTIVE-REPAIR-SPECIMENS-END

(defn run [kind negative?]
  (let [rows (if negative?
               (if (= kind :absence) [] [{:repo :negative :root "/tmp"
                                          :path (if (= kind :artefact) "test/mutation.sh" "test/mutation.clj")
                                          :text (get negative-text kind)}])
               (corpus))
        findings (if (and negative? (= kind :absence))
                   [{:repo :negative :path "mutation.edn" :finding :absence-coerced
                     :input :missing :becomes 0.0}]
                   ((scanners kind) rows))]
    {:kind kind :negative? negative? :findings findings
     :counts (if negative?
               (into (sorted-map) (frequencies (map :repo findings)))
               (merge (sorted-map :futon2 0 :futon3 0 :p4ng 0)
                      (frequencies (map :repo findings))))}))

(defn main [kind args]
  (let [negative? (some #{"--negative"} args)
        result (run kind negative?)
        n (count (:findings result))]
    (println (pr-str (assoc result :findings (vec (take 30 (:findings result))))))
    (if negative?
      (if (pos? n)
        (do (println (name kind) "lint: PASS negative mutation rejected exit-convention=0-pass/1-fail/2-mutation-slipped") 0)
        (do (println (name kind) "lint: FAIL negative mutation slipped exit-convention=0-pass/1-fail/2-mutation-slipped") 2))
      (do (println (name kind) "lint:" (if (zero? n) "PASS" "FAIL") "findings=" n "counts=" (pr-str (:counts result)) "exit-convention=0-pass/1-fail")
          (if (zero? n) 0 1)))))
