(ns futon2.aif.wm-scheduled-run-flight-test
  "H-T-CALLER-I: the loop entry's flight path is opt-in and the default tick is
  unchanged. `scripts/wm_scheduled_run.clj` reads FUTON_WM_FLIGHT; unset it is the
  tick, and the script requires no flight namespace, so the default tick loads no
  flight code (`futon2.aif.outer-loop` is resolved lazily, only by `flight-plan!`)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-loop :as outer-loop]
            [wm-scheduled-run :as run]))

(deftest unset-is-the-tick
  (is (nil? (run/flight-mode nil)))
  (is (nil? (run/flight-mode "")))
  (is (nil? (run/flight-mode "   ")))
  (testing "a value selects the flight path, trimmed"
    (is (= "plan" (run/flight-mode "plan")))
    (is (= "plan" (run/flight-mode " plan\n")))))

(deftest the-script-requires-no-flight-code
  ;; the default tick must not load the flight path: the ns form names none of it
  (let [ns-form (with-open [r (java.io.PushbackReader. (io/reader "scripts/wm_scheduled_run.clj"))]
                  (binding [*read-eval* false] (read r)))
        required (set (map str (flatten (filter #(and (seq? %) (= :require (first %))) ns-form))))]
    (is (contains? required "futon2.report.war-machine") "the tick's own requires are there")
    (is (empty? (filter #(or (str/starts-with? % "futon2.aif.flight")
                             (str/starts-with? % "futon2.aif.outer")
                             (str/starts-with? % "futon2.aif.target-field"))
                        required))
        "no flight, outer-cascade, outer-loop or target-field namespace is required at load")
    (testing "and nothing of them is aliased or referred into the script's namespace"
      (is (empty? (filter #(re-find #"flight|outer|target-field" (str (ns-name (val %))))
                          (ns-aliases 'wm-scheduled-run)))))))

(deftest the-trigger-is-read-as-the-tick-always-read-it
  (is (= :unspecified (run/trigger-from-env (constantly nil))))
  (is (= :wallclock-cron (run/trigger-from-env {"FUTON_WM_TRIGGER" "wallclock-cron"})))
  (is (= :duree-click-on-demand (run/trigger-from-env {"FUTON_WM_TRIGGER" "duree-click-on-demand"}))))

(deftest only-plan-is-wired
  (testing "any other mode is refused, loudly, before anything runs"
    (doseq [m ["run" "fly" "PLAN" "1"]]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"only \"plan\" is wired"
                            (run/flight-plan! m {:trigger :unspecified}))
          m)))
  (testing "plan hands its options to plan-from-field! and prints the result"
    (let [got (atom nil)
          out (with-redefs [outer-loop/plan-from-field!
                            (fn [o] (reset! got o) {:selection {:chosen "M-a"} :plan {:placement {:target-source :chosen}}})]
                (with-out-str (run/flight-plan! "plan" {:trigger :wallclock-cron :seed 9 :seat "kimi-6"})))]
      (is (= {:trigger :wallclock-cron :seed 9 :seat "kimi-6"} @got))
      (is (= {:selection {:chosen "M-a"} :plan {:placement {:target-source :chosen}}} (edn/read-string out))))))
