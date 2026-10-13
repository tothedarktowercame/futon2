(ns futon2.aif.legacy-three-halves-registry
  "Explicit migration certificates for interpretations published before the
  three-halves receipt field became mandatory.  Entries are target- and
  pattern-specific and bind both source documents; there is no default."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as evidence]))

(def ^:private declarations
  {"M-interim-director-proxy-metric-inventory"
   {:target-source
    {:path "futon7/holes/M-interim-director-proxy-metric-inventory.md"
     :revision "75a8378a9bdc8672242c5f471c2824dc28583532"
     :sha256 "b8cc99cafd574380f0d09033d7bf5a97b477017b8a8df3a8d0092303219ee643"}
    :patterns
    {:orchestration/recorded-handoff
     {:path "futon3/library/orchestration/recorded-handoff.flexiarg"
      :revision "860a5595a58d62866b1013715934ae23934cae49"
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

(defn- revision-bytes [code-root {:keys [path revision]}]
  (let [[repo & relative] (str/split path #"/")
        repo-root (.getCanonicalPath (io/file code-root repo))
        object-path (str/join "/" relative)
        {:keys [exit out]} (shell/sh "git" "-C" repo-root "show"
                                     (str revision ":" object-path)
                                     :out-enc :bytes)]
    (when (zero? exit) out)))

(defn- pinned-revision? [code-root {:keys [revision sha256] :as source}]
  (when (and (string? revision) (re-matches #"[0-9a-f]{40}" revision))
    (some->> (revision-bytes code-root source)
             evidence/sha256
             (= sha256))))

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
               (= (select-keys pattern-source [:path :sha256]) (:source receipt))
               (pinned-revision? code-root pattern-source)
               (pinned-revision? code-root target-source))
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
