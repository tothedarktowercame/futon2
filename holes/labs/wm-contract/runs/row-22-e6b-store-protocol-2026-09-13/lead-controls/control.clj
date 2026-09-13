(require '[futon2.aif.machine-slow-feedback-store :as s] '[clojure.edn :as edn])
(import '(java.nio.file Files) '(java.nio.file.attribute FileAttribute))
(def authority {:verifier/source-sha256 (apply str (repeat 64 "a")) :evidence-source-sha256s {}})
(defn fresh [] (let [root (.toFile (Files/createTempDirectory "lead-e6b-" (make-array FileAttribute 0))) owner (s/isolated-store root "lead-test")] (s/initialize! owner {:state {:value 0} :revision "r0" :authority authority :committed-at "2026-09-13T00:00:00Z"}) owner))
(let [owner (fresh)]
 (try
  (let [path (.toFile (:head owner)) original (edn/read-string (slurp path))]
   (spit path (pr-str (assoc original :state/revision "invented" :state-sha256 (apply str (repeat 64 "f")))))
   (let [r (s/recover owner)]
    (assert (= "invented" (get-in r [:head :state/revision])))
    (assert (= "r0" (get-in r [:current :state/revision])))
    (prn {:control :head-transaction-mismatch :recover-accepted true :capture-consistent (:local-chain-consistent? (s/capture owner))})))
  (finally (s/release! owner))))
(let [owner (fresh)]
 (try
  (let [h (:head (s/recover owner)) p {:prior {:revision "r0" :transaction-sha256 (:transaction-sha256 h) :state-sha256 (:state-sha256 h)} :next {:revision "r1" :state {:value 1}} :application {:application/id "a1" :feedback/event-id "e1" :status :committed :opaque (Object.)} :authority authority :committed-at "2026-09-13T00:01:00Z"}]
   (s/compare-and-commit! owner p)
   (let [result (try (s/recover owner) :unexpected-success (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e))))]
    (assert (= :e6b-store/invalid-edn result))
    (prn {:control :unreadable-application-published :commit-returned true :recovery result})))
  (finally (s/release! owner))))
