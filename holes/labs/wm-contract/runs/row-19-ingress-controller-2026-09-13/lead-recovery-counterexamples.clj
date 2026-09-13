(require '[futon3c.agency.invoke-ingress-controller :as ingress])
(import '(java.nio.file Files))
(defn store []
  (let [dir (Files/createTempDirectory "lead-ingress-review-" (make-array java.nio.file.attribute.FileAttribute 0))
        s (ingress/file-deferred-store (.resolve dir "deferred.edn"))]
    (ingress/initialize-file-store! s) s))
(defn closed [s]
  (let [c (ingress/controller {:auth-token "isolated-review" :deferred-store s})]
    (ingress/close-intake! c) c))
(let [s (store) c (closed s)]
  (ingress/defer-resume! c "valid" {:prompt "retained"})
  (let [accepted (ingress/defer-resume! c "unreadable" {:payload (Object.)})
        refusal (try (closed s) nil
                     (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e))))]
    (assert (= "unreadable" accepted))
    (assert (= :ingress/deferred-edn-invalid refusal))
    (prn {:control :accepted-non-edn-payload :returned accepted :recovery-refusal refusal})))
(let [s (store) c1 (closed s) c2 (closed s)]
  (ingress/defer-resume! c1 "accepted-a" {:prompt "a"})
  (ingress/defer-resume! c2 "accepted-b" {:prompt "b"})
  (let [recovered (ingress/reopen! (closed s))]
    (assert (= [["accepted-b" {:prompt "b"}]] recovered))
    (prn {:control :two-controllers-overwrite-accepted-resume
          :recovered recovered :missing-accepted-id "accepted-a"})))
