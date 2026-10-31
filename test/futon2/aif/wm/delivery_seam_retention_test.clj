(ns futon2.aif.wm.delivery-seam-retention-test
  "The omission behind injury adf04df0 made visible dynamically, not just as
  an index row. Two claims are locked together:

  1. The handoff site index (the one existing registry, reused — no parallel
     infrastructure) declares the open-cascade delivery-check seam with a
     retention schema, and that declaration matches the retention module's
     actual schema constant and passes the one existing audit.
  2. Driving the REAL open-cascade-resume-handler delivery-error branch with
     an adf04df0-shaped answer (abbreviated commit descriptor) yields the
     typed refusal AND a retained, content-addressed record whose exact
     check input survives for replay. Before the retention wiring, this
     test failed on :delivery-evidence missing — the same silent loss the
     injury recorded.

  Response-format validation (handoff-contract decoder; here
  cascade-patch-delivery/check-delivery) and durable replay-input sufficiency
  (delivery-failure-evidence) remain distinct obligations at one seam.

  Explicitly NOT covered here (unsupported capability, documented): the
  meta-self-heal diagnosis producer invoke lives in futon3c
  (futon3c/wm/meta-self-heal-adapters producer-invoke!) while
  handoff-site-index audits :source anchors relative to the futon2 checkout
  root; a cross-repo row would always audit :source-missing. That seam is
  covered by the wrapper fidelity test in meta-self-heal-production-test
  until the index grows a cross-repo convention."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.handoff-contract :as handoff]
            [futon2.aif.handoff-site-index :as site-index]
            [futon2.aif.wm.delivery-failure-evidence :as dfe]
            [futon2.aif.wm.open-cascade-resume :as open-resume]))

(def repo (.getCanonicalPath (io/file ".")))

(def ^:private adf04df0-shaped-answer
  {:requisition-id "6a8fe374958818d6246ee46acc6dbdb851c7cdf66930626395572519879d195c"
   :cascade-digest "fcbc11a5af649403bf4cb1255d537339a31e4077f707fb73ef5d8656b64555d0"
   :by "wm-author"
   :patch {:namespace "futon2.aif.wm.authored.req-6a8fe374"
           :var "decisions"
           :source-digest "c049abef66f39c5eb988514752cfd34d81771ffb02595c297df2ca9aa482dcf3"
           :commit "8561ecdc7"}})

(deftest the-index-declares-the-seam-and-the-declaration-is-real
  (let [sites (:sites (site-index/read-index))
        site (some #(when (= :execution/open-cascade-delivery-check (:id %)) %)
                   sites)]
    (is (some? site) "the delivery-check boundary has an index row")
    (is (= :wm/delivery-failure-evidence-v1 (:retention site)))
    (is (= (:retention site) dfe/schema-version)
        "declared retention schema matches the retention module's constant")
    (is (= :cascade-patch-delivery/check-delivery (:decoder site))
        "format validation stays the decoder's job, distinct from retention")
    (let [audit (site-index/audit repo)]
      (is (= :verified (:status audit)) (pr-str (:problems audit))))))

(deftest the-real-resume-handler-retains-what-it-refuses
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "dfe-seam" (make-array java.nio.file.attribute.FileAttribute 0)))
        retained (atom nil)
        handler (runtime/open-cascade-resume-handler
                 {:author "wm-author" :reviewer "wm-reviewer"
                  :run-id "seam-retention-test"
                  :delivery-failure-evidence-root (str root)})]
    (with-redefs
      [open-resume/recognize (constantly {:status :held})
       open-resume/authoring-requisition
       (fn [_ _ _] {:status :dispatched
                   :requisition-id (:requisition-id adf04df0-shaped-answer)
                   :cascade-digest (:cascade-digest adf04df0-shaped-answer)})
       open-resume/validate-author-binding (constantly {:status :bound})
       handoff/contact-with-format-correction
       (fn [{:keys [request]}]
         (is (= (:requisition-id adf04df0-shaped-answer) (:requisition-id request)))
         {:response {:state "done" :job-id "invoke-seam-test"}
          :answer adf04df0-shaped-answer
          :format-correction nil})]
      (let [result (handler {:kind :held-open-cascade})]
        (testing "the typed format refusal still happens first"
          (is (= :error (:status result)))
          (is (= :abbreviated-commit-descriptor (:reason result))))
        (testing "the exact refused input is retained at the boundary"
          (let [ev (:delivery-evidence result)]
            (is (= :retained (:status ev))
                "before the retention wiring this key was absent entirely")
            (reset! retained ev)
            (is (= "invoke-seam-test" (:author-job result)))))
        (let [pin (:pin @retained)
              back (:record (dfe/read-pin pin))]
          (is (= :present (:status (dfe/read-pin pin))))
          (is (= "8561ecdc7" (get-in back [:check-input :patch :commit]))
              "the abbreviated descriptor survives verbatim")
          (is (= (:cascade-digest adf04df0-shaped-answer)
                 (get-in back [:check-input :cascade-digest])))
          (testing "and the retained record is a usable replay input"
            (let [w (dfe/evidence-readiness
                     {:candidate-pins [(select-keys pin [:path :sha256])]
                      :requisition-id (:requisition-id adf04df0-shaped-answer)
                      :cascade-digest (:cascade-digest adf04df0-shaped-answer)})]
              (is (= :usable (:status (first (:retained-input w))))
                  (pr-str (:retained-input w))))))))))
