(ns futon2.report.eig-source-remaining-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-sources :as sources]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.ticket-queue :as queue]
            [futon2.aif.token-belief-predecessor :as predecessor]
            [futon2.report.war-machine :as wm]))

(def target "M-aif-policy-conditioned-eig")
(def source-resource "wm/cascade-sources/M-aif-policy-conditioned-eig.edn")
(def updater :hole/h6378c65a4012)
(def typed-q :hole/h0e270aa090bc)
(def shadow :hole/h42fceb4ad48b)
(def new-patterns #{:contracts/every-entry-has-a-falsifier :aif/two-layer-calibration})
(defn declaration [] (edn/read-string (slurp (io/resource source-resource))))

(defn with-declaration [decl f]
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                     "eig-source-" (make-array java.nio.file.attribute.FileAttribute 0)))
        current (slurp (io/resource "fixtures/eig-source-remaining/mission-after-updater.md"))
        old (slurp (io/resource "fixtures/M-aif-policy-conditioned-eig-pre-aeb352f8.md"))
        calls (atom [])]
    (try
      (spit (io/file dir "source.edn") (pr-str decl))
      ;; Replace only Git IO, not C4's declaration matcher or observation/admission.
      ;; Both historical and current mission contents are frozen resource bytes.
      (with-redefs-fn
        {#'checks/git
         (fn [repo & args]
           (swap! calls conj [repo args])
           (case (first args)
             "rev-parse" {:exit 0 :out (str (apply str (repeat 40 (if (str/starts-with? (last args) "cb2045b8") "b" "a"))) "\n")}
             "show" {:exit 0 :out (if (str/starts-with? (second args) "bbbbbbbb") old current)}
             (throw (ex-info "Unexpected observation IO" {:repo repo :args args}))))
         #'predecessor/production-authority
         (fn [_] {:status :refused :kind :fixture-no-predecessor})}
        #(f {:dir dir :calls calls
             :sources (sources/with-context-fn (sources/load-declared (str dir)))}))
      (finally (doseq [file (reverse (file-seq dir))] (io/delete-file file true))))))

(defn decide [src dir]
  (let [assembled (problems/assemble {:targets [target] :sources (assoc src :horizon-steps 2)})
        result (wm/cascade-decision
                assembled
                {:ticket-queue queue/empty-declaration
                 :cascade-habit-path (str (io/file dir "absent-habit.edn"))
                 :novelty-inputs {}
                 :live-c {:derived {:want #{(keyword "alive" target)}
                                    :weights {(keyword "alive" target) 1}
                                    :lam 1 :entries [] :gaps [] :signature "frozen-eig-source"}}})]
    result))

(deftest remaining-work-reaches-real-joint-selection
  (with-declaration
    (declaration)
    (fn [{:keys [sources dir calls]}]
      (let [result (decide sources dir)
            decision (:decision result)
            actions (keys (get-in decision [:selection-law :posterior]))
            first-patterns (set (map #(get-in % [:precedence 0 :id]) actions))
            produced (set (mapcat #(mapcat :produces (:precedence %)) actions))
            traces (get-in decision [:selection-certificate :node-evaluation-traces])]
        (is (true? (get-in sources [:universes target updater])))
        (is (false? (get-in sources [:universes target typed-q])))
        (is (false? (get-in sources [:universes target shadow])))
        (is (= 2 (count actions)) "Two executable alternatives must survive admission")
        (is (= new-patterns first-patterns))
        (is (= #{[target typed-q] [target shadow]} produced))
        (is (not (contains? produced [target updater])) "No admitted action produces the completed updater again")
        (is (= 2 (count traces)))
        (is (every? #(= 2 (:horizon %)) traces))
        (is (some #(= :no-new-wanted-token (:reason %)) (:dropped-candidates result))
            "The retained updater candidate is correctly declined")
        (is (seq @calls) "The real loader executed mechanical C4 observations")
        (is (not (.exists (io/file dir "absent-habit.edn"))))))))

(deftest each-new-order-alone-progresses-within-the-common-horizon
  (doseq [pattern new-patterns]
    (let [decl (declaration)
          candidate (first (filter #(= [pattern] (:precedence %)) (:candidates decl)))]
      (is (some? candidate))
      (when candidate
        (with-declaration
          (assoc decl :candidates [candidate])
          (fn [{:keys [sources dir]}]
            (let [r (decide sources dir)]
              (is (= 1 (count (get-in r [:decision :selection-law :posterior]))))
              (is (= pattern (get-in r [:decision :action :precedence 0 :id]))))))))))

(deftest tampered-new-pattern-pin-refuses-before-selection
  (doseq [pattern new-patterns]
    (let [bad (assoc-in (declaration) [:interpretation-receipts pattern :source :sha256]
                        (apply str (repeat 64 "0")))
          refusal (try (with-declaration bad (fn [_] :unexpected-admission))
                       (catch clojure.lang.ExceptionInfo e (ex-data e)))]
      (is (= :interpretation-source-hash-mismatch (:reason refusal))))))
