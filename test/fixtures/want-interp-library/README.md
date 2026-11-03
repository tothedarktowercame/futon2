# Want-interpretation fixture library

The two writing-coherence flexiargs are purpose-built four-part test fixtures.
Their real sources at
`/home/joe/code/futon3/library/writing-coherence/{meet-the-reader-where-they-are,plain-language-thesis}.flexiarg`
still lacked `BECAUSE` at futon3 HEAD
`f0dfb3b0d03fcf93ff4f0f76a45d0935a4116e69`, so they could not be copied
verbatim. Each fixture gives IF, HOWEVER, BECAUSE, and THEN one complete
sentence. The paired three-halves-square fixtures reproduce those sentences
verbatim, as required since futon2 commit `51eb13f19`.

The pattern SHA-256 values are pinned in
`M-futon-seams-interpretations@futon2-78439f58.edn` and in each paired square:

```text
78a273be1e399bd3e728ab67effce58ddb9da437c207f0c60a44df47e8c313d7  futon3/library/writing-coherence/meet-the-reader-where-they-are.flexiarg
ee59b145c4f90f0e983d754a0e923f181228c812d5c65a385f8723f1d13999a3  futon3/library/writing-coherence/plain-language-thesis.flexiarg
01b07046ec443127b44c867beded0ab87170713eaa9bd627f0b8dc8e497e6044  ../three-halves-square/meet-the-reader-where-they-are.edn
563aee013832fec50a842720bac296f2415989c8c3c7475feb6a9b211ea43f18  ../three-halves-square/plain-language-thesis.edn
```
