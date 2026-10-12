(ns futon2.aif.legacy-three-halves-registry
  "Explicit migration certificates for interpretations published before the
  three-halves receipt field became mandatory.  Entries are target- and
  pattern-specific and bind both source documents; there is no default."
  (:require [clojure.java.io :as io]
            [futon2.aif.interpretation-evidence :as evidence]))

(def ^:private declarations
  {"M-interim-director-proxy-metric-inventory"
   {:target-source
    {:path "futon7/holes/M-interim-director-proxy-metric-inventory.md"
     :sha256 "19cfda4445117f6356728cee91795675860ab52337c556ff9eb1d242c3cef1e3"}
    :patterns
    {:orchestration/recorded-handoff
     {:path "futon3/library/orchestration/recorded-handoff.flexiarg"
      :sha256 "232ec874e6061ce48f7edc39cc6a7029c7005a934c5769abd26cf4ed93c30620"}
     :ukrns/reader-run-path
     {:path "futon3/library/ukrns/reader-run-path.flexiarg"
      :sha256 "d86ea60823aa9647ff460f510bf5b394490b224e8a911b6ad3677ffc50dcd06b"}
     :war-machine/state-capture
     {:path "futon3/library/war-machine/state-capture.flexiarg"
      :sha256 "b45598fb80725dfb4b7deadf520355feaca633dc9d1238c037c7fad654b6d363"}
     :measurement/warrant-travels-with-the-number
     {:path "futon3/library/measurement/warrant-travels-with-the-number.flexiarg"
      :sha256 "415a41bc2fc385ebc02e7cae2dc20d0b484790afa9b14a7958fc66853652e7e3"}}}})

(defn- current-pin? [code-root {:keys [path sha256]}]
  (let [f (io/file code-root path)]
    (and (.isFile f)
         (= sha256 (evidence/sha256 (java.nio.file.Files/readAllBytes (.toPath f)))))))

(defn- object [text source elem label]
  {:text text :source source
   :theory {:elems #{elem} :names {elem label} :axioms #{}}})

(defn square
  "Return the exact migration square only when the stored receipt and both
  live source bytes agree with the declaration."
  [code-root target pattern receipt]
  (let [{:keys [target-source patterns]} (get declarations target)
        pattern-source (get patterns pattern)]
    (when (and pattern-source
               (= pattern-source (:source receipt))
               (current-pin? code-root pattern-source)
               (current-pin? code-root target-source))
      {:schema :wm/three-halves-square-v1
       :objects
       {:G (object "The target's unresolved criterion" target-source :need "need")
        :I1 (object "The admitted library-pattern interpretation" pattern-source
                    :pattern "pattern")
        :I2 (object "The pinned target criterion" target-source :criterion "criterion")
        :B (object "The interpreted pattern applied to the target criterion"
                   pattern-source :application "application")}
       :maps {:a1 #{[:need :pattern]}
              :a2 #{[:need :criterion]}
              :b1 #{[:pattern :application]}
              :b2 #{[:criterion :application]}}
       :auxiliary {:b1 false :b2 false}
       :migration {:kind :pre-three-halves-interpretation
                   :target target :pattern pattern}})))
