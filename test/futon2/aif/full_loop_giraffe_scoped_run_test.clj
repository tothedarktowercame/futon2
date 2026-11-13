(ns futon2.aif.full-loop-giraffe-scoped-run-test
  "Planted regression for repair wm-click-loop-repair-619d23da.

  Held run 2026-11-13-073aa327 stopped at outer-selection with
  :outer-score/no-admitted-tasks because find-giraffe-run served the newest
  giraffe run, which was a scoped `:only-ids` sample (three T-ladder
  tickets, data/giraffe/20261113T054505.540Z-7d42790e).  Its three pass
  rows matched no open registry task, so every one of the 257 open tasks
  was excluded as :unread-new (reach :pass-row-unavailable) and the scored
  field emptied.  missions-giraffe-manifest? — the only manifest gate
  find-giraffe-run/verified-giraffe-run? apply — must not admit a scoped
  run as the missions pass."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runtime])) ;; loaded so find-var can see its privates

(def ^:private manifest? (var-get (find-var 'futon2.aif.full-loop-runtime/missions-giraffe-manifest?)))

(def ^:private base-manifest
  {:sources {:edges {:path "/home/joe/wm-sandbox/code/futon3c/holes/missions/giraffe-mission-edges.json"}}})

(deftest scoped-only-ids-run-is-not-a-missions-pass
  (testing "a scoped only-ids invocation is rejected"
    (is (false? (manifest? (assoc-in base-manifest [:invocation :only-ids]
                                     ["T-ladder-08-memory-preregistration"
                                      "T-ladder-09-the-mirror"
                                      "T-ladder-10-von-neumann-stop"])))))
  (testing "an explicit empty only-ids invocation is a full run"
    (is (true? (manifest? (assoc-in base-manifest [:invocation :only-ids] [])))))
  (testing "a legacy run with no recorded invocation is still admitted"
    (is (true? (manifest? base-manifest))))
  (testing "a nil only-ids under a recorded invocation is a full run"
    (is (true? (manifest? (assoc-in base-manifest [:invocation :only-ids] nil)))))
  (testing "non-missions edges are still rejected"
    (is (false? (manifest? {:sources {:edges {:path "/x/tickets/edges.json"}}})))))
