(ns futon2.aif.harm-rules-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.harm-rules :as harm]))

(def rules (delay (harm/load-rules)))

(def world
  {:path-exists? #(contains? #{"/prod/runs/a.edn" "/tmp/scratch" "/code/Old.lean"} %)
   :production-data-roots (constantly ["/prod"])
   :default-branch (fn [_repo _remote] "refs/heads/main")
   :joe-identities (constantly #{"joe" :joe})
   :local-destination? #(contains? #{"/tmp/note" "local-file"} % )})

(defn result [rule action]
  (harm/match @rules {:rule rule :declined-action action} world))

(deftest harm-rule-matched-h1-production-data
  (is (= :matched (:status (result :H1 {:operation :delete
                                         :paths ["/prod/runs/a.edn"]}))))
  (is (= :check-failed (:reason (result :H1 {:operation :delete
                                              :paths ["/tmp/scratch"]})))))

(deftest harm-rule-matched-h2-dangerous-push
  (is (= :matched (:status (result :H2 {:operation :git-push :repo "/code/x"
                                         :remote "origin" :ref "refs/heads/topic"
                                         :force? true}))))
  (is (= :check-failed (:reason (result :H2 {:operation :git-push :repo "/code/x"
                                              :remote "origin" :ref "refs/heads/topic"
                                              :force? false})))))

(deftest harm-rule-matched-h3-shared-service
  (is (= :matched (:status (result :H3 {:operation :service-restart
                                         :service :futon3c-agency}))))
  (is (= :check-failed (:reason (result :H3 {:operation :service-restart
                                              :service :private-repl})))))

(deftest harm-rule-matched-h4-external-publication
  (is (= :matched (:status (result :H4 {:operation :post :actor "joe"
                                         :destination "service://public"}))))
  (is (= :check-failed (:reason (result :H4 {:operation :publish :actor "codex"
                                              :destination "local-file"})))))

(deftest harm-rule-matched-h5-spending
  (is (= :matched (:status (result :H5 {:operation :purchase :vendor "vendor"
                                         :amount 1 :currency :USD}))))
  (is (= :check-failed (:reason (result :H5 {:operation :purchase :vendor "vendor"
                                              :amount 0 :currency :USD})))))

(deftest harm-rule-matched-h6-existing-lean
  (is (= :matched (:status (result :H6 {:operation :edit
                                         :paths ["/code/Old.lean"]}))))
  (is (= :check-failed (:reason (result :H6 {:operation :edit
                                              :paths ["/code/New.lean"]})))))

(deftest harm-rule-matched-rejects-unbound-or-cross-rule-claims
  (is (= :no-declined-action
         (:reason (harm/match @rules {:rule :H1 :reason "dangerous"} world))))
  (is (= :operation-not-in-rule
         (:reason (result :H2 {:operation :delete :paths ["/prod/runs/a.edn"]}))))
  (is (= :unknown-rule
         (:reason (result :H99 {:operation :delete :paths ["/prod/runs/a.edn"]}))))
  (is (= :missing-field
         (:reason (result :H5 {:operation :purchase :vendor "v" :amount 1}))))
  (let [rules-with-unknown-check
        (update-in @rules [:rules 0 :checks] conj :not-implemented)]
    (is (= :unknown-check
           (:reason (harm/match rules-with-unknown-check
                                {:rule :H1
                                 :declined-action {:operation :delete
                                                   :paths ["/prod/runs/a.edn"]}}
                                world))))))

(deftest harm-rule-matched-digest-is-stable-under-map-key-order
  (let [a (result :H5 (array-map :operation :purchase :vendor "v"
                                  :amount 2 :currency :USD))
        b (result :H5 (array-map :currency :USD :amount 2 :vendor "v"
                                  :operation :purchase))]
    (is (= :matched (:status a)))
    (is (= (:declined-action-digest a) (:declined-action-digest b)))))
