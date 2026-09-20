# Coverage read path

construction-wiring-result carries the fold result under :fold-output.
Read [:fold-output :coverage-score-delta] to obtain -1.0 for the witnessed
positive construction. The wrapper has no top-level :coverage-score-delta;
reading that absent key yields nil. No wrapper code change is needed.

The follow-up pins all seven reviewed guard shapes against zero-witness box
formation. It changes tests only; implementation 28a90c73 remains the reviewed
repair commit. Store closure uses actual fetched Agency jobs and computed
evidence; the before observation for artifact binding comes from the retained
precommit receipt and its recorded file modification time, as explicitly
identified in computed-evidence.edn.
