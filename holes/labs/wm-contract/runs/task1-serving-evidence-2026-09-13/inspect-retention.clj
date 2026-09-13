(require '[clojure.edn :as edn])
(import '(java.nio.file Files Paths) '(java.security MessageDigest) '(java.time Instant))
(let [path (Paths/get "/tmp/futon3c-invoke-jobs.edn" (make-array String 0))
      bs (Files/readAllBytes path)
      data (edn/read-string (String. bs "UTF-8"))
      jobs (:jobs data)
      id "invoke-1789315476118-20734-4279de32"
      job (get jobs id)
      sha (apply str (map #(format "%02x" (bit-and 255 %))
                         (.digest (MessageDigest/getInstance "SHA-256") bs)))]
  (prn {:schema :wm/task1-serving-retention-discovery-v1 :at (str (Instant/now))
        :pid 1942869 :source/path (str path) :source/sha256 sha :source/bytes (alength bs)
        :job-count (count jobs)
        :retained-commission-count (count (filter :request-commission (vals jobs)))
        :fresh-dispatch {:job-id id :present? (some? job) :state (:state job)
                         :commission-present? (some? (:request-commission job))
                         :request-digest-present? (some? (:request-digest job))}
        :claim :read-only-discovery :deployment-verified? false}))
