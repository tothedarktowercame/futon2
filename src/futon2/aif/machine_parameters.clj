(ns futon2.aif.machine-parameters
  "Finite registered machine-model hypothesis kernels. No Dirichlet proxy and
   no EIG integration."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.machine-model :as machine-model])
  (:import [java.security MessageDigest]))

(def schema :wm/parameter-kernels-v1)
(defn- refusal [kind path] {:ok false :refusal {:kind kind :path path}})

(defn- sha256 [path]
  (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file path)))
        digest (.digest (MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and 255 %)) digest))))

(defn- registration [h]
  (let [{:keys [path] expected-sha :sha256} (:registration h)]
    (cond
      (not (and (string? path) (re-matches #"[0-9a-f]{64}" (or expected-sha ""))))
      (refusal :missing-registration-pin [:hypotheses (:id h) :registration])
      (not (.isFile (io/file path)))
      (refusal :unregistered-hypothesis [:hypotheses (:id h) :registration])
      (not= expected-sha (sha256 path))
      (refusal :registration-pin-mismatch [:hypotheses (:id h) :registration])
      :else
      (let [record (edn/read-string (slurp path))]
        (if (= (select-keys record [:schema :id :revision :likelihood])
               {:schema :wm/parameter-hypothesis-v1 :id (:id h)
                :revision (:revision h) :likelihood (:likelihood h)})
          {:ok true :record record}
          (refusal :unregistered-hypothesis [:hypotheses (:id h)]))))))

(defn parameter-kernels
  "Construct Q(theta|pi), Q(theta|o,pi), evidence, and the shared marginal."
  [model parameter-state policies outcome-support]
  (let [hypotheses (:hypotheses parameter-state)
        ids (mapv :id hypotheses)
        prior (:prior parameter-state)
        states (:state-support model)
        q-state (:state-distribution parameter-state)]
    (cond
      (not= outcome-support (get-in model [:outcome :support]))
      (refusal :support-mismatch [:outcome-support])
      (not= (:model parameter-state) (:model model))
      (refusal :model-revision-mismatch [:parameter-state :model])
      (not (and (vector? hypotheses) (seq hypotheses)))
      (refusal :unregistered-hypothesis [:hypotheses])
      (not= (set ids) (set (keys prior)))
      (refusal :support-mismatch [:prior])
      (nil? (machine-model/row-sum-admission prior))
      (refusal :invalid-mass [:prior])
      (not= (set states) (set (keys q-state)))
      (refusal :support-mismatch [:state-distribution])
      :else
      (if-let [bad (first (remove :ok (map registration hypotheses)))]
        bad
        (let [likelihood
              (into {}
                    (for [p policies h hypotheses]
                      [[(:id p) (:id h)]
                       (into {}
                             (for [o outcome-support]
                               [o (reduce + (for [[s mass] q-state]
                                              (* mass (get-in h [:likelihood :rows s o]))))]))]))
              marginals
              (into {} (for [p policies]
                         [(:id p) (into {} (for [o outcome-support]
                                            [o (reduce + (for [h hypotheses]
                                                           (* (get prior (:id h))
                                                              (get-in likelihood [[(:id p) (:id h)] o]))))]))]))
              posteriors
              (into {}
                    (for [p policies o outcome-support
                          :let [z (get-in marginals [(:id p) o])]]
                      [[(:id p) o]
                       (if (pos? z)
                         {:ok true :evidence z
                          :mass (into {} (for [h hypotheses]
                                          [(:id h) (/ (* (get prior (:id h))
                                                        (get-in likelihood [[(:id p) (:id h)] o])) z)]))}
                         {:ok false :refusal {:kind :zero-evidence-conditioning
                                             :path [:posteriors (:id p) o]}})]))
              recomputed (into {} (for [p policies]
                                    [(:id p) (into {} (for [o outcome-support]
                                                       [o (reduce + (for [h hypotheses]
                                                                      (* (get prior (:id h))
                                                                         (get-in likelihood [[(:id p) (:id h)] o]))))]))]))]
          (if (not= marginals recomputed)
            (refusal :marginal-compatibility-failed [:marginal])
            {:ok true :schema schema :model (:model model) :theta ids
             :authority (:authority parameter-state)
             :prior-kernel (into {} (for [p policies] [(:id p) prior]))
             :likelihood likelihood :posterior-kernel posteriors
             :evidence-normalizers marginals :posterior-predictive marginals
             :likelihood-marginal recomputed}))))))
