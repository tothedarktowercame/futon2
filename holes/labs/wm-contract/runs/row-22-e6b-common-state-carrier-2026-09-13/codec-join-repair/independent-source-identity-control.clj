(require '[clojure.edn :as edn]
         '[futon2.aif.machine-slow-state-carrier :as carrier]
         '[futon2.aif.machine-slow-state-carrier-test :as test-fixture])

(let [bundle (#'test-fixture/bundle)
      decode-record (fn [role]
                      (edn/read-string
                       (String. (.decode (java.util.Base64/getDecoder)
                                         (get-in bundle [:original-sources role :bytes/base64]))
                                "UTF-8")))
      e2b-record (assoc (decode-record :e2b-subject) :run/id "borrowed")
      outcome-record (assoc (decode-record :outcome) :run/id "borrowed")
      edited (-> bundle
                 (#'test-fixture/replace-source :e2b-subject e2b-record)
                 (#'test-fixture/replace-source :outcome outcome-record))
      output (carrier/project-transition edited)]
  (prn {:status (:status output)
        :context-run (get-in output [:original-sources :context :record :run/id])
        :e2b-run (get-in output [:original-sources :e2b-subject :record :run/id])
        :outcome-run (get-in output [:original-sources :outcome :record :run/id])}))
