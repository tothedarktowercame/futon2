(require '[futon2.aif.on-demand-entrypoint :as e] '[futon2.aif.on-demand-entrypoint-test :as t])
(let [r (e/run-on-demand! t/valid-config {:post! (fn [_ _] {:status 200 :body "{\"click-id\":\"ours\"}"}) :get! (fn [_ _] {:status 200 :body "{\"click-id\":\"ours\"}"}) :now-ms (constantly 0) :sleep! (fn [_])})]
 (prn r) (assert (nil? (:terminal r))) (assert (= "row26-isolated-1" (:run/id r))))
