#!/usr/bin/env python3
"""Capture a serving debugger boundary without continuing or rerunning it.

Usage: wm_capture_held_section.py ROOT RUN_ID LABEL
The saved EDN must round-trip before capture succeeds. Keywords are encoded
as #wm/keyword [namespace name]; decode with a reader calling keyword on that
pair. Version 2 also records map/set iteration order with wm/map and wm/set
tags; readers reconstruct sorted collections in that order. Both EDN equality
and exact printed form must survive capture.
A result is evidence
of a held computation, not automatically a successful mission effect.
"""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import subprocess
import wm_click_repair_loop as runtime


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("root", type=Path)
    ap.add_argument("run_id")
    ap.add_argument("label")
    args = ap.parse_args()
    if not args.label.replace("-", "").isalnum():
        raise ValueError("Label must contain only letters, numbers and hyphens")
    root = args.root.resolve()
    root.mkdir(parents=True, exist_ok=True)
    data = root / (args.label + ".edn.gz")
    summary = root / (args.label + ".json")
    if summary.exists():
        saved = json.loads(summary.read_text())
        assert saved["run-id"] == args.run_id
        assert hashlib.sha256(gzip.decompress(data.read_bytes())).hexdigest() == saved["sha256"]
        print(json.dumps({"status": "restored", **saved}))
        return
    if data.exists():
        raise ValueError("Unfinished capture exists; inspect it before replacing")
    pins = {name: subprocess.check_output(["git", "-C", str(runtime.CODE / name),
                                          "rev-parse", "HEAD"], text=True).strip()
            for name in ["futon2", "futon3c"]}
    q = json.dumps
    form = f'''(let [rid {q(args.run_id)}
          stop (first (filter #(= rid (:run-id %)) (futon2.aif.wm.debugger/stopped)))
          _ (when-not stop (throw (ex-info "Run is not stopped" {{:run-id rid}})))
          value (try {{:present true :value (futon2.aif.wm.debugger/stop-value rid)}}
                     (catch clojure.lang.ExceptionInfo e
                       {{:present false :reason (:kind (ex-data e))}}))
          record {{:schema :wm/live-held-section-v1 :run-id rid :stop stop
                  :result value :printer {{:namespace-maps *print-namespace-maps*
                                          :meta *print-meta* :length *print-length* :level *print-level*}}
                  :captured-at (str (java.time.Instant/now))
                  :source (select-keys (futon2.aif.full-loop-runner/runner-source-drift)
                           [:runner/source-check :runner/loaded-sha256 :runner/canonical-sha256])}}
          encode (fn encode [x]
                   (cond
                     (keyword? x) (tagged-literal 'wm/keyword [(namespace x) (name x)])
                     (map? x) (tagged-literal 'wm/map (mapv (fn [[k v]] [(encode k) (encode v)]) x))
                     (set? x) (tagged-literal 'wm/set (mapv encode x))
                     (vector? x) (mapv encode x)
                     (sequential? x) (doall (map encode x))
                     :else x))
          order-comparator (fn [xs]
                             (let [positions (zipmap xs (range))]
                               (fn [a b]
                                 (if (= a b) 0
                                   (compare [(get positions a (count positions)) (pr-str a)]
                                            [(get positions b (count positions)) (pr-str b)])))))
          readers {{'wm/keyword (fn [[n k]] (keyword n k))
                    'wm/map (fn [pairs]
                              (into (sorted-map-by (order-comparator (mapv first pairs))) pairs))
                    'wm/set (fn [xs] (into (sorted-set-by (order-comparator xs)) xs))}}
          text (pr-str (encode record))
          decoded (clojure.edn/read-string
                    {{:readers readers}} text)
          _ (when-not (= record decoded)
              (throw (ex-info "Held value is not round-trippable EDN" {{:run-id rid}})))
          _ (when-not (= (pr-str record) (pr-str decoded))
              (throw (ex-info "Held value lost print order" {{:run-id rid}})))
          bytes (.getBytes text "UTF-8")
          result {{:encoding "wm/ordered-edn-v2" :print-roundtrip true :run-id rid :phase (:phase stop) :has-value (:present value)
                  :path {q(str(data))}
                  :sha256 (futon2.aif.interpretation-evidence/sha256 bytes)}}]
      (with-open [out (java.util.zip.GZIPOutputStream.
                       (clojure.java.io/output-stream {q(str(data))}))]
        (.write out bytes))
      (spit {q(str(summary))} (cheshire.core/generate-string result))
      result)'''
    (root / (args.label + "-capture.clj")).write_text(form + "\n")
    response = runtime.jvm_eval(form, timeout=60)
    (root / (args.label + "-capture-response.json")).write_text(
        json.dumps({"response": response, "repository_heads": pins}, indent=2) + "\n")
    if response["rc"] or not summary.exists():
        raise RuntimeError(response)
    saved = json.loads(summary.read_text())
    assert hashlib.sha256(gzip.decompress(data.read_bytes())).hexdigest() == saved["sha256"]
    print(json.dumps({"status": "captured", **saved}))


if __name__ == "__main__":
    main()
