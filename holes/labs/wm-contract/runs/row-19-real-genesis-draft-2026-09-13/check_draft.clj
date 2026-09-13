(require '[clojure.edn :as edn] '[clojure.java.io :as io])

(def dir "holes/labs/wm-contract/runs/row-19-real-genesis-draft-2026-09-13")
(defn rd [name] (edn/read-string (slurp (str dir "/" name))))
(defn sha256 [path]
  (with-open [in (io/input-stream path)]
    (let [md (java.security.MessageDigest/getInstance "SHA-256") b (byte-array 8192)]
      (loop [] (let [n (.read in b)] (when (pos? n) (.update md b 0 n) (recur))))
      (apply str (map #(format "%02x" (bit-and 255 %)) (.digest md))))))

(let [d (rd "genesis-evidence-draft.edn") r (rd "root-resolution.edn")
      j (rd "author-job-snapshot.edn")
      paths {:source "../futon3c/src/futon3c/agency/r9_authority.clj"
             :tests "../futon3c/test/futon3c/agency/r9_genesis_test.clj"
             :review "holes/labs/wm-contract/runs/row-19-genesis-verifier-2026-09-13/review-fixes/byte-hardening/lead-review.md"}
      expected-missing #{:author-request-commission-preimage :independent-reviewer-job
                         :independent-reviewer-request-commission
                         :independent-reviewer-trace-authority :exact-subject-acceptance}
      checks [(= :pending (:status d)) (true? (:not-an-anchor d)) (= :verified (:status r))
              (= (get-in j [:job :job-id]) (get-in j [:trace-authority :trace->job]))
              (= :request-commission-missing (get-in d [:author :commission :refusal]))
              (= expected-missing (set (:missing d)))
              (every? (fn [[role path]] (= (sha256 path) (get-in d [:artifacts role :sha256]))) paths)]
      ok? (every? true? checks)]
  (prn {:checker :row19-real-genesis-draft :checks (count checks)
        :passed (count (filter true? checks))
        :status (if ok? :pending-by-evidence :invalid) :missing (:missing d)})
  (System/exit (if ok? 0 1)))
