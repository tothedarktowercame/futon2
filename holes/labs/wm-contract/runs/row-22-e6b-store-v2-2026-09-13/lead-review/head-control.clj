(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[futon2.aif.machine-slow-feedback-store-v2 :as store]
         '[futon2.aif.machine-slow-feedback-store-v2-test :as fixture])
(let [[root owner artifact] (#'fixture/setup)]
  (try
    (store/commit! owner (#'fixture/pin artifact))
    (let [path (io/file root "HEAD.edn")
          original (edn/read-string (slurp path))
          forged (assoc original :state/revision "invented-head-revision"
                        :state-sha256 (apply str (repeat 64 "f")))]
      (spit path (pr-str forged))
      (let [recovered (store/recover owner)
            captured (store/capture owner)]
        (prn {:control :head-current-mismatch
              :accepted-head (select-keys (:head recovered) [:state/revision :state-sha256])
              :actual-next (select-keys (get-in recovered [:current :next]) [:revision :state-sha256])
              :capture-generation (:generation captured)
              :restart-authorized? (:restart-authorized? captured)})
        (assert (= "invented-head-revision" (get-in recovered [:head :state/revision])))
        (assert (not= (get-in recovered [:head :state/revision])
                      (get-in recovered [:current :next :revision])))))
    (finally (store/release! owner))))
