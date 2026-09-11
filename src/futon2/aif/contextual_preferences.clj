(ns futon2.aif.contextual-preferences
  "Pure fixture binding derivation. Supplied warrants/evidence are fixture
   authority, never authenticated production authority. No events or adoption."
  (:require [clojure.string :as str])
  (:import (java.nio.charset StandardCharsets)
           (java.security MessageDigest)))

(defn- text? [x] (and (string? x) (not (str/blank? x))))
(defn- revision? [x] (and (integer? x) (pos? x)))
(defn- refusal [reason path]
  {:status :refused :reason reason :path path})

(defn- digest [view]
  ;; Hash the exact supplied UTF-8 view bytes, not an EDN printer's encoding.
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (MessageDigest/getInstance "SHA-256")
                           (.getBytes ^String view StandardCharsets/UTF_8)))))

(defn- field-error [binding fields]
  (some (fn [[path valid?]]
          (let [value (get-in binding path)]
            (cond
              (nil? value) (refusal :missing-prerequisite path)
              (not (valid? value)) (refusal :invalid-prerequisite path))))
        fields))

(defn- binding-error [b]
  (or
   (when-not (map? b) (refusal :invalid-binding []))
   (field-error b
                [[[:fixture?] true?]
                 [[:instance-id] text?]
                 [[:task] text?]
                 [[:feedback] text?]
                 [[:revision] revision?]
                 [[:membership :status] #{:established}]
                 [[:membership :revision] revision?]
                 [[:membership :establisher] text?]
                 [[:membership :recipients] vector?]
                 [[:applicability :status] #{:established :not-applicable}]
                 [[:applicability :evidence] text?]
                 [[:applicability :scope] text?]
                 [[:authority :warrant] text?]
                 [[:authority :actor] text?]
                 [[:authority :instance-id] text?]
                 [[:payload :finding] text?]
                 [[:payload :reason] text?]
                 [[:payload :response-route] text?]
                 [[:payload :view] text?]
                 [[:payload :digest] text?]
                 [[:receipt-standard] #{:authorized-inbox}]
                 [[:inbox-adapter :status] #{:verifiable :missing}]
                 [[:claims-timeliness?] boolean?]])
   (when-not (= (:task b) (get-in b [:applicability :scope]))
     (refusal :scope-mismatch [:applicability :scope]))
   (when-not (= (:instance-id b) (get-in b [:authority :instance-id]))
     (refusal :authority-mismatch [:authority :instance-id]))
   (when (= :missing (get-in b [:inbox-adapter :status]))
     (refusal :adapter-gap [:inbox-adapter]))
   (field-error b [[[:inbox-adapter :evidence] text?]])
   (when (:claims-timeliness? b)
     ;; Fixture clock uses integer ticks; no wall clock or invented duration.
     (field-error b [[[:deadline] #(and (integer? %) (<= 0 %))]]))
   (some (fn [[index recipient]]
           (when-let [error (field-error recipient
                                        [[[:id] text?] [[:role] text?]
                                         [[:reason] text?] [[:evidence] text?]])]
             (update error :path #(into [:membership :recipients index] %))))
         (map-indexed vector (get-in b [:membership :recipients])))
   (let [ids (map :id (get-in b [:membership :recipients]))]
     (when-not (= (count ids) (count (distinct ids)))
       (refusal :duplicate-recipient [:membership :recipients])))
   (when-not (= (digest (get-in b [:payload :view]))
                (get-in b [:payload :digest]))
     (refusal :digest-mismatch [:payload :digest]))))

(defn derive-binding
  "Validate a fixture instance and derive its initial obligations.
   Returns :refused with a typed reason/path, :not-applicable, or :derived.
   Membership is the supplied frozen roster, including affected consumers;
   each entry needs role, reason and evidence. Empty coverage is vacuous and
   earns no delivery evidence. Registry presence is not an input to derivation."
  [binding]
  (if-let [error (binding-error binding)]
    error
    (if (= :not-applicable (get-in binding [:applicability :status]))
      {:status :not-applicable :binding binding :obligations {}}
      (let [recipients (get-in binding [:membership :recipients])]
        {:status :derived
         :binding binding
         :obligations (into {} (map (fn [{:keys [id]}]
                                     [id {:state :pending
                                          :revision (:revision binding)
                                          :digest (get-in binding [:payload :digest])
                                          :receipt-standard :authorized-inbox}])
                                   recipients))
         :coverage {:required (count recipients) :received 0}
         :vacuous? (empty? recipients)
         :delivery-evidence []
         :delivery-complete false
         :consideration :unobserved
         :revision-acceptance :unobserved
         :subsequent-use :unobserved}))))
