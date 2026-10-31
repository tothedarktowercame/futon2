(ns futon2.aif.wm.cascade-patch-delivery-descriptor-test
  "Descriptor-typing witness for injury adf04df0 (click wm-click-eaa9bc3a,
  ordinal 130). The author supplied the real abbreviated commit \"8561ecdc7\"
  (9 hex chars); the gate's 40-hex regex failed it and the reason branch
  mislabeled the miss :commit-not-on-canonical-head under
  :author-delivery-wrong-repository — a false wrong-repository injury.

  The written author contract (open-cascade-author-prompt) requires the FULL
  40-hex SHA in patch.commit, so abbreviations stay REFUSED — but with an
  accurate typed descriptor reason, before any git call, never folded into
  the wrong-repository label, and never silently replaced by the
  history-derived fallback.

  All tests read the canonical repository /home/joe/code/futon2 read-only
  (git log/merge-base/show); the pinned author artifact is the real one from
  the injury (namespace req-6a8fe374, digest c049abef…dcf3, full sha
  8561ecdc7666…fb8fa2e, reachable from main)."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.wm.cascade-patch-delivery :as gate]))

(def canonical "/home/joe/code/futon2")
(def full-sha "8561ecdc7666c1af3d4de101cf6112206fb8fa2e")
(def real-abbrev "8561ecdc7") ; the exact 9-char string from the retained answer
(def digest "c049abef66f39c5eb988514752cfd34d81771ffb02595c297df2ca9aa482dcf3")

(defn- resource-fn [rel]
  (.toURL (.toURI (io/file canonical "src" rel))))

(defn- answer
  ([commit] (answer "futon2.aif.wm.authored.req-6a8fe374" digest commit))
  ([ns digest commit]
   {:patch (cond-> {:namespace ns :var "decisions" :source-digest digest}
             commit (assoc :commit commit))}))

(deftest real-abbreviated-descriptor-is-typed-not-wrong-repository
  (let [r (gate/check-delivery canonical resource-fn (answer real-abbrev))]
    (testing "the exact live injury shape is refused as a descriptor error"
      (is (= :error (:status r)))
      (is (= :author-delivery-invalid-descriptor (:kind r))
          "a format miss is not a wrong-repository delivery")
      (is (= :abbreviated-commit-descriptor (:reason r)))
      (is (= real-abbrev (:supplied-commit r))
          "the original descriptor is retained, not silently replaced"))
    (testing "no location scan ran and no history fallback hid the miss"
      (is (empty? (:found-locations r)))
      (is (= real-abbrev (:source-commit r))
          "the malformed value must not be substituted by file history"))))

(deftest full-sha-and-un-supplied-both-deliver
  (testing "the real full sha delivers"
    (let [r (gate/check-delivery canonical resource-fn (answer full-sha))]
      (is (= :delivered (:status r)) (pr-str (select-keys r [:kind :reason])))))
  (testing "no supplied commit falls back to file history (legacy path intact)"
    (let [r (gate/check-delivery canonical resource-fn (answer nil))]
      (is (= :delivered (:status r)))
      (is (re-matches #"[a-f0-9]{40}" (str (:source-commit r)))))))

(deftest malformed-unreachable-wrongrepo-and-mismatched-controls
  (testing "non-hex garbage is a typed invalid descriptor"
    (let [r (gate/check-delivery canonical resource-fn (answer "not-a-sha"))]
      (is (= :author-delivery-invalid-descriptor (:kind r)))
      (is (= :invalid-commit-descriptor (:reason r)))))
  (testing "well-formed but unknown 40-hex keeps the reachability reason"
    (let [unknown (apply str (repeat 40 "e"))
          r (gate/check-delivery canonical resource-fn (answer unknown))]
      (is (= :author-delivery-wrong-repository (:kind r)))
      (is (= :commit-not-on-canonical-head (:reason r)))))
  (testing "bytes not present in canonical keep the wrong-repository label"
    (let [r (gate/check-delivery canonical resource-fn
                                 (answer "futon2.aif.wm.authored.req-6a8fe374"
                                         (apply str (repeat 64 "f")) nil))]
      (is (= :author-delivery-wrong-repository (:kind r)))
      (is (= :source-not-in-canonical-repository (:reason r)))))
  (testing "reachable commit whose blob differs from the digest is a mismatch"
    ;; 8561ecdc's first parent predates the authored file: it is an ancestor
    ;; of HEAD but does not contain the exact claimed bytes.
    (let [parent (-> (shell/sh "git" "-C" canonical
                               "rev-parse" (str full-sha "~1"))
                     :out str/trim)
          _ (is (re-matches #"[a-f0-9]{40}" (str parent)))
          r (gate/check-delivery canonical resource-fn (answer parent))]
      (is (= :error (:status r)))
      (is (contains? #{:committed-source-mismatch :commit-not-on-canonical-head}
                     (:reason r))
          (pr-str (select-keys r [:kind :reason]))))))

(deftest author-prompt-states-the-descriptor-contract-accurately
  (let [prompt ((var-get (resolve 'futon2.aif.full-loop-runtime/open-cascade-author-prompt))
                {:routable-capabilities [:wm/pattern-apply]})
        _ (is (string? prompt))]
    (is (re-find #"full commit SHA" prompt))
    (is (re-find #"exactly 40 lowercase hex" prompt))
    (is (re-find #":author-delivery-invalid-descriptor" prompt))))
