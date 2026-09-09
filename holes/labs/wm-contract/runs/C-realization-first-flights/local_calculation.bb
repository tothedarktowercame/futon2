(require '[clojure.edn :as edn]
         '[clojure.pprint :as pp])

;; Run from futon2: bb holes/labs/wm-contract/runs/C-realization-first-flights/local_calculation.bb
;; Read-only calculation. stdout is the report; no live services or preferences.
(def pins
  {"../futon3c/holes/specs/flight.witness.live-957a4836.edn"
   "3dba6c9a9e98174a1cb081738adbc0c51d1e26c3084884d59e6d2858de56a359"
   "../futon3c/scripts/flight_spec_verify.clj"
   "065a81b381a436f0ca58f26c19dbf24ec31df3a8417564e545a9562595d30be5"})

(defn sha256 [path]
  (let [bytes (java.nio.file.Files/readAllBytes (.toPath (java.io.File. path)))
        digest (.digest (java.security.MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and (int %) 255)) digest))))

(doseq [[path expected] pins]
  (when-not (= expected (sha256 path))
    (throw (ex-info "Source pin changed; review before recalculating" {:path path}))))

(load-file "../futon3c/scripts/flight_spec_verify.clj")
(def verify-record (resolve 'scripts.flight-spec-verify/verify))
(def window-settled? (resolve 'scripts.flight-spec-verify/settled-window?))
(def witness
  (edn/read-string (slurp "../futon3c/holes/specs/flight.witness.live-957a4836.edn")))

(defn evaluate [record]
  (let [report (verify-record record)
        measurement (get-in record [:organs :measurement :judgment])
        predicted (get-in measurement [:predicted :g])
        realised (get-in measurement [:realised :g])
        window (get-in record [:organs :window :judgment])
        scans (:scans window)]
    {:predicted predicted :realised realised
     :absolute-error (Math/abs (- realised predicted))
     :scan-disagreement (Math/abs (- (get-in (first scans) [:g :g])
                                    (get-in (second scans) [:g :g])))
     :epsilon (:epsilon window)
     :settled-window? (boolean (window-settled? window))
     :measurement-ground-present? (contains? (get-in record [:organs :measurement]) :ground)
     :conforms? (:conforms? report)
     :calibration-admissibility (get-in report [:projection :validity-mask])
     :failed-invariants (vec (sort (for [[k v] (:invariants report) :when (not (:pass v))] k)))}))

(def intact (evaluate witness))
(def without-ground
  (evaluate (update-in witness [:organs :measurement] dissoc :ground)))

(assert (= :in (:calibration-admissibility intact)))
(assert (:conforms? intact))
(assert (= :out (:calibration-admissibility without-ground)))
(assert (false? (:conforms? without-ground)))
(assert (= [:F1-term-or-sorry] (:failed-invariants without-ground)))
(assert (= (select-keys intact [:predicted :realised :absolute-error])
           (select-keys without-ground [:predicted :realised :absolute-error])))

(pp/pprint
 {:schema :c-realization/local-satisfaction-v1
  :source-pins pins :flight-id (:flight/id witness)
  :historical-witness intact
  :counterfactual {:label :measurement-ground-removed :historical? false
                   :result without-ground}
  :established :grounded-record-supports-admissible-calibration-pair
  :not-established [:trained-prior :policy-grade-capability :cluster-A-complete]
  :preference-masses :not-assigned
  :probability-of-future-success :not-estimated})
