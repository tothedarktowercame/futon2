(ns futon2.aif.machine-slow-state-carrier-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-slow-feedback-evidence :as evidence]
            [futon2.aif.machine-slow-feedback-evidence-test :as et]
            [futon2.aif.machine-slow-state-carrier :as carrier])
  (:import [java.nio.file Files]
           [java.security MessageDigest]
           [java.util Base64]))

(defn- digest [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bs))))))
(defn- vd [x] (digest (.getBytes (pr-str x) "UTF-8")))
(defn- bundle []
  (let [cfg (#'et/fixture identity)
        proposal (evidence/validate-transition (#'et/prospective-config identity))
        configured (get-in cfg [:sources])
        root (:evidence-root cfg)]
    {:proposal-evidence proposal
     :original-sources
     (into {}
           (for [role carrier/source-roles
                 :let [pin (configured role)
                       bs (Files/readAllBytes (.toPath (io/file root (:relative-path pin))))]]
             [role {:bytes/base64 (.encodeToString (Base64/getEncoder) bs)
                    :source-sha256 (digest bs)
                    :value-sha256 (get-in proposal [:input/digests role])}]))}))
(defn- refusal [x]
  (try (carrier/project-transition x) nil
       (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))
(defn- replace-source [b role record]
  (let [bs (.getBytes (str (pr-str record) "\n") "UTF-8")]
    (-> b
        (assoc-in [:original-sources role]
                  {:bytes/base64 (.encodeToString (Base64/getEncoder) bs)
                   :source-sha256 (digest bs) :value-sha256 (vd record)})
        (assoc-in [:proposal-evidence :source/digests role] (digest bs))
        (assoc-in [:proposal-evidence :input/digests role] (vd record)))))

(deftest projects-both-carriers-with-distinct-digest-roles
  (let [input (bundle) out (carrier/project-transition input)]
    (is (= :structurally-projected (:status out)))
    (is (= :none (:authority/status out)))
    (is (= (:proposal-evidence input) (:proposal-evidence out)))
    (is (= (set carrier/source-roles) (set (keys (:original-sources out)))))
    (is (not= (get-in out [:prior :carrier :slow/intrinsics])
              (get-in out [:next :carrier :slow/intrinsics])))))

(deftest carrier-projection-retains-state-and-fixed-order
  (let [out (carrier/project-transition (bundle))]
    (is (= [:schema :model/id :model/revision :run/id :tick/index
            :state/revision :slow/mode :slow/intrinsics]
           (vec (keys (get-in out [:prior :carrier])))))
    (is (= 4 (get-in out [:prior :carrier :tick/index])))
    (is (= 5 (get-in out [:next :carrier :tick/index])))
    (is (not= (get-in out [:prior :sha256])
              (get-in out [:digest-roles :complete-next-record])))
    (is (= (get-in (bundle) [:original-sources :context :bytes/base64])
           (get-in out [:original-sources :context :bytes/base64])))))

(deftest refuses-schema-coordinate-and-subject-mutations
  (doseq [[label mutate expected]
          [[:missing-prior-key
            (fn [b]
              (let [p (update-in b [:proposal-evidence :prior :state] dissoc :slow/mode)]
                (replace-source p :prior-state (get-in p [:proposal-evidence :prior :state]))))
            :e6b-carrier/proposal-record-mismatch]
           [:extra-prior-key
            (fn [b]
              (let [record (assoc (get-in b [:proposal-evidence :prior :state]) :extra true)
                    b' (assoc-in b [:proposal-evidence :prior :state] record)]
                (-> (replace-source b' :prior-state record)
                    (assoc-in [:proposal-evidence :prior :state-sha256] (vd record)))))
            :e6b-carrier/schema-invalid]
           [:nonfinite-coordinate
            (fn [b]
              (let [record (assoc-in (get-in b [:proposal-evidence :prior :state])
                                     [:slow/intrinsics :advance-capability :alpha] ##Inf)
                    b' (assoc-in b [:proposal-evidence :prior :state] record)]
                (-> (replace-source b' :prior-state record)
                    (assoc-in [:proposal-evidence :prior :state-sha256] (vd record)))))
           :e6b-carrier/coordinate-invalid]
           [:wrong-predecessor
            (fn [b]
              (let [next (assoc-in (get-in b [:proposal-evidence :next :state])
                                   [:predecessor/revision] "other")]
                (-> b (assoc-in [:proposal-evidence :next :state] next)
                    (assoc-in [:proposal-evidence :next :state-sha256] (vd next)))))
            :e6b-carrier/transition-subject-mismatch]
           [:wrong-tick
            (fn [b]
              (let [next (assoc-in (get-in b [:proposal-evidence :next :state])
                                   [:tick/index] 8)]
                (-> b (assoc-in [:proposal-evidence :next :state] next)
                    (assoc-in [:proposal-evidence :next :state-sha256] (vd next)))))
            :e6b-carrier/transition-subject-mismatch]]]
    (testing (name label)
      (is (= expected (refusal (mutate (bundle))))))))

(deftest refuses-bytes-digests-and-candidate-booleans
  (is (= :e6b-carrier/source-digest-mismatch
         (refusal (assoc-in (bundle) [:original-sources :context :source-sha256]
                            (apply str (repeat 64 "0"))))))
  (is (= :e6b-carrier/schema-invalid
         (refusal (assoc (bundle) :verified? true))))
  (is (= :e6b-carrier/schema-invalid
         (refusal (assoc-in (bundle) [:proposal-evidence :verified?] true)))))
