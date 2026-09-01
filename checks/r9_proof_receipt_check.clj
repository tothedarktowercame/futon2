#!/usr/bin/env bb
(ns checks.r9-proof-receipt-check
  (:require [babashka.process :as process]
            [checks.mutable-read-set :as read-set]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def receipt-path "holes/labs/wm-contract/R9-VerdictConsultsChecker-proof-receipt.edn")
(def repo-root "/home/joe/code")

(defn sha256-text [s]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes s "UTF-8"))
    (format "%064x" (java.math.BigInteger. 1 (.digest digest)))))

(defn declaration-text [source declaration]
  (let [lines (vec (str/split-lines source))
        start-pattern (re-pattern
                       (str "^(?:structure|inductive|def)\\s+"
                            (java.util.regex.Pattern/quote declaration)
                            "(?:\\s|$)"))
        boundary? #(boolean
                    (re-find #"^(?:/--|structure\s|inductive\s|def\s|theorem\s|lemma\s|namespace\s|end\s)" %))
        start (first (keep-indexed #(when (re-find start-pattern %2) %1) lines))]
    (when (nil? start)
      (throw (ex-info "declaration absent" {:declaration declaration})))
    (let [end (or (first (keep-indexed
                          #(when (and (> %1 start) (boundary? %2)) %1)
                          lines))
                  (count lines))]
      (str (str/join "\n" (subvec lines start end)) "\n"))))

(defn proof-receipt-shape? [x]
  (and (= :LeanProofReceipt (:receipt/type x))
       (= 2 (:receipt/version x))
       (string? (:recorded-at x))
       (= "DarkTower.WarMachine.Holes.r9VerdictConsultsChecker" (:theorem x))
       (seq (get-in x [:proof-source :declarations]))
       (every? #(and (string? (:name %)) (= 64 (count (:sha256 % ""))))
               (get-in x [:proof-source :declarations]))
       (= ["lake" "env" "lean"] (get-in x [:elaborator :command]))
       (= 0 (get-in x [:result :exit]))
       (vector? (get-in x [:result :axioms]))
       (every? string? (get-in x [:result :axioms]))))

(defn elaborate [receipt]
  (let [theorem (:theorem receipt)
        augmented (str "import DarkTower.WarMachine.Holes\n#print axioms " theorem "\n")
        tmp (java.io.File/createTempFile "r9-proof-receipt-" ".lean")]
    (try
      (spit tmp augmented)
      (let [result (process/shell {:out :string :err :string :continue true
                                   :dir (get-in receipt [:elaborator :cwd])}
                                  "lake" "env" "lean" (.getAbsolutePath tmp))
            axiom-line (str (:out result) (:err result))
            axioms (if-let [[_ body] (re-find #"depends on axioms: \[([^]]*)\]" axiom-line)]
                     (if (str/blank? body) [] (mapv str/trim (str/split body #",")))
                     nil)]
        {:exit (:exit result) :axioms axioms :output axiom-line})
      (finally (.delete tmp)))))

(defn validate
  ([receipt] (validate receipt nil))
  ([receipt source-override]
  (let [proof (:proof-source receipt)
        source-path (str repo-root "/" (:repo proof) "/" (:path proof))
        source (or source-override (slurp source-path))
        declaration-failures
        (when (proof-receipt-shape? receipt)
          (->> (:declarations proof)
               (keep (fn [{:keys [name sha256]}]
                       (try
                         (when-not (= sha256 (sha256-text (declaration-text source name)))
                           {:declaration name :reason :source-drift})
                         (catch Exception _
                           {:declaration name :reason :source-absent}))))
               vec))
        live (when (proof-receipt-shape? receipt) (elaborate receipt))
        failures (cond-> []
                   (not (proof-receipt-shape? receipt)) (conj :shape)
                   (seq declaration-failures) (conj :declaration-source-drift)
                   (and live (not= (:result receipt) (select-keys live [:exit :axioms])))
                   (conj :elaboration-drift))]
    {:pass? (empty? failures) :failures failures
     :declaration-failures declaration-failures
     :theorem (:theorem receipt) :axioms (:axioms live)})))

(defn receipt-file [mode]
  (if (= "absent" mode) (str receipt-path ".absent-control") receipt-path))

(defn -main [& [mode]]
  (let [negative? (#{"absent" "tampered"} mode)
        receipt-result (try
                         (let [first-observation (read-set/observe-files [(receipt-file mode)])
                               first-snapshot (read-set/require-stable! first-observation)
                               receipt (edn/read-string
                                        (:text (read-set/entry-by-path first-snapshot
                                                                       (receipt-file mode))))
                               source-path (str repo-root "/" (get-in receipt [:proof-source :repo]) "/"
                                                (get-in receipt [:proof-source :path]))
                               observation (read-set/observe-files [(receipt-file mode) source-path])
                               snapshot (read-set/require-stable! observation)
                               final-receipt (edn/read-string
                                              (:text (read-set/entry-by-path snapshot
                                                                             (receipt-file mode))))
                               final-source-path (str repo-root "/"
                                                      (get-in final-receipt [:proof-source :repo]) "/"
                                                      (get-in final-receipt [:proof-source :path]))
                               _ (when-not (= source-path final-source-path)
                                   (throw (ex-info "proof receipt source changed during discovery"
                                                   {:before source-path :after final-source-path})))]
                           {:receipt final-receipt
                            :source-text (:text (read-set/entry-by-path snapshot source-path))})
                            (catch Exception e {:error e}))
        receipt (:receipt receipt-result)
        report (if-let [read-error (:error receipt-result)]
                 {:pass? false :failures [:unreadable]
                  :message (.getMessage read-error)}
                 (let [captured-source (:source-text receipt-result)
                       source-override (case mode
                                         "tampered" (str/replace-first captured-source
                                                                       "fun _ _ => false"
                                                                       "fun _ _ => true")
                                         "unrelated" (str captured-source
                                                          "\n-- unrelated C115 control\n")
                                         captured-source)]
                   (try (validate receipt source-override)
                        (catch Exception e {:pass? false :failures [:unreadable]
                                            :message (.getMessage e)}))))]
    (cond
      (and negative? (not (:pass? report)))
      (println "r9-proof-receipt-check: negative-control PASS (binding evidence rejected) exit-convention=0-pass/1-fail/2-mutation-slipped" (pr-str report))
      negative?
      (do (println "r9-proof-receipt-check: FAIL (mutation slipped) exit-convention=0-pass/1-fail/2-mutation-slipped" (pr-str report))
          (System/exit 2))
      (= "unrelated" mode)
      (if (:pass? report)
        (println "r9-proof-receipt-check: unrelated-edit PASS (declaration basis stable) exit-convention=0-pass/1-fail/2-mutation-slipped" (pr-str report))
        (do (println "r9-proof-receipt-check: unrelated-edit FAIL exit-convention=0-pass/1-fail/2-mutation-slipped" (pr-str report))
            (System/exit 1)))
      (:pass? report)
      (println "r9-proof-receipt-check: PASS exit-convention=0-pass/1-fail/2-mutation-slipped" (pr-str report))
      :else
      (do (println "r9-proof-receipt-check: FAIL exit-convention=0-pass/1-fail/2-mutation-slipped" (pr-str report))
          (System/exit 1)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
