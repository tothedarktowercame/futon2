(function (root, factory) {
  const api = factory();
  if (typeof module === "object" && module.exports) module.exports = api;
  if (root) root.PatchboardModel = api;
})(typeof window !== "undefined" ? window : null, function () {
  "use strict";

  const range = n => Array.from({length:n}, (_,i) => i);
  const identity = n => range(n);
  const clampShift = n => range(n).map(k => Math.max(k - 1, 0));

  function assertWiring(wiring) {
    const n = Array.isArray(wiring) ? wiring.length : 0;
    if (!n || wiring.some(j => !Number.isInteger(j) || j < 0 || j >= n)) {
      throw new Error("wiring must be a non-empty total map into its own jack set");
    }
    return wiring;
  }

  function choices(n) {
    let out = [[]];
    for (let i=0; i<n; i++) out = out.flatMap(prefix => [prefix.concat(0), prefix.concat(1)]);
    return out;
  }

  function analyse(wiring, pin=0, derive=true) {
    assertWiring(wiring);
    const n = wiring.length, image = new Set(wiring);
    const free = range(n).filter(j => !image.has(j));
    const unsat = range(n).filter(k => wiring[k] === k);
    let values = Array(n).fill(null); free.forEach(j => values[j] = pin);
    for (let pass=0; pass<n; pass++) {
      for (let k=0; k<n; k++) {
        const j = wiring[k];
        if (values[k] !== null && j !== k && values[j] === null) values[j] = 1 - values[k];
      }
    }
    const chain = range(n).filter(j => values[j] !== null && !free.includes(j) && !unsat.includes(j));
    const attractors = derive ? choices(unsat.length).flatMap(bits => {
      const candidate = values.slice(); unsat.forEach((j,i) => candidate[j] = bits[i]);
      if (candidate.some(x => x === null)) return [];
      const bitstring = candidate.join("");
      return [{bits:bitstring, value:parseInt(bitstring, 2)}];
    }).filter((x,i,a) => a.findIndex(y => y.bits === x.bits) === i).sort((a,b) => a.value-b.value) : [];
    return {size:n, wiring:wiring.slice(), free, chain, unsat, pin, attractors};
  }

  const metacaTerminals = ["000","001","010","100","011","101","110","111"]
    .map((condition,jack) => ({jack, condition}));
  const antTerminals = [
    ["food",.55,null],["pher",.35,.30],["food-trace",null,null],["pher-trace",null,null],
    ["home-prox",.20,.70],["enemy-prox",.10,.10],["h",.40,.40],["ingest",.60,.65],
    ["friendly-home",null,null],["trail-grad",.30,.25],["novelty",null,null],
    ["dist-home",.50,.15],["reserve-home",.60,.65],["cargo",.40,.10]
  ].map(([channel,outbound,homebound],jack) => ({jack,channel,outbound,homebound}));

  function boot() {
    if (typeof document === "undefined") return;
    const state = {
      tick:0, running:true, tickMs:180, order:"ascending", mode:"outbound", pin:0,
      antMap:identity(14), metaMap:clampShift(8), metaBits:[0,1,1,0,1,0,0,1],
      antValues:{outbound:antTerminals.map(x => x.outbound), homebound:antTerminals.map(x => x.homebound)},
      pending:{ant:null, meta:null}, timer:null
    };

    const setText = (id,text) => { document.getElementById(id).textContent=text; };
    const fmtSet = xs => `{${xs.join(", ")}}`;
    const kindFor = (shape,k) => shape.unsat.includes(k) ? "unsat" : shape.chain.includes(k) ? "chain" : "free";
    const shapeHtml = (shape, showAttractors) => {
      let attr = "";
      if (showAttractors) {
        const shown = shape.attractors.length <= 16
          ? shape.attractors.map(x => `${x.value} · ${x.bits}`).join("  |  ")
          : `${shape.attractors.length} states (all UNSAT assignments)`;
        attr = `<div class="attractors"><b>ATTR</b> ${shown || "∅ (unseeded closed cycle)"}</div>`;
      }
      return `<div class="shape"><div class="free-text"><b>FREE</b> ${fmtSet(shape.free)} · scaffold</div><div class="chain-text"><b>CHAIN</b> ${fmtSet(shape.chain)} · propagated structure</div><div class="unsat-text"><b>UNSAT</b> ${fmtSet(shape.unsat)} · motion</div>${attr}</div>`;
    };

    function boardSvg(kind, terminals, mapping, shape) {
      const n=terminals.length, row=34, height=n*row+20, left=154, right=430;
      const values = kind === "ant" ? state.antValues[state.mode] : null;
      const cables = range(n).map(k => {
        const y1=18+k*row, y2=18+mapping[k]*row, cls=kindFor(shape,k);
        return `<path class="cable ${cls}" d="M${left},${y1} C${left+92},${y1} ${right-92},${y2} ${right},${y2}"/>`;
      }).join("");
      const source = terminals.map((t,k) => {
        const y=18+k*row, dead=values && values[k]===null, label=kind==="ant"?t.channel:t.condition;
        return `<g class="jack source ${kindFor(shape,k)} ${dead?"dead":""} ${state.pending[kind]===k?"selected":""}" data-kind="${kind}" data-side="source" data-jack="${k}" transform="translate(${left},${y})"><circle r="8"/><text x="-142">${k} · ${label}</text><text class="sub" x="-142" y="11">→ ${mapping[k]}${dead?" · DEAD":""}</text></g>`;
      }).join("");
      const target = terminals.map((t,j) => {
        const y=18+j*row, dead=values && values[j]===null, incoming=mapping.filter(x=>x===j).length;
        return `<g class="jack target ${shape.free.includes(j)?"free":""} ${dead?"dead":""}" data-kind="${kind}" data-side="target" data-jack="${j}" transform="translate(${right},${y})"><circle r="8"/><text x="14">${j} · in ${incoming}${dead?" · DEAD":""}</text></g>`;
      }).join("");
      return `<svg viewBox="0 0 570 ${height}" role="img" aria-label="${kind} patch board">${cables}${source}${target}</svg>`;
    }

    function antLiveHtml() {
      const values=state.antValues[state.mode];
      return `<div class="live"><h3>LIVE C MEANS · current ${state.mode} state · DEAD writes skipped</h3><div class="bars">${values.map((v,k) => `<div class="bar-wrap ${v===null?"dead":""}"><div class="bar"><i style="height:${v===null?0:Math.round(v*100)}%"></i></div>${k}<br>${v===null?"nil":v.toFixed(2)}</div>`).join("")}</div></div>`;
    }
    function metaLiveHtml() {
      return `<div class="live"><h3>LIVE GENOTYPE TERMINALS · analytic attractor above, operational sweep below</h3><div class="bits">${state.metaBits.map((v,k)=>`<div class="bit ${v?"on":""}">${k}<br>${v}</div>`).join("")}</div></div>`;
    }

    function render() {
      const antShape=analyse(state.antMap,state.pin,false), metaShape=analyse(state.metaMap,state.pin,true);
      document.getElementById("ant-board").innerHTML = `<div class="board-head"><h2>ANT · 14 observation-preference jacks</h2><p>Continuous C means. Four permanent DEAD jacks; :food is additionally DEAD homebound.</p></div><div class="presets"><button data-preset="ant-identity">identity · sham</button><button data-preset="ant-clamp">non-injective cascade</button></div>${shapeHtml(antShape,false)}<div class="patch-area">${boardSvg("ant",antTerminals,state.antMap,antShape)}</div><div class="legend"><span><i class="swatch" style="background:var(--free)"></i>FREE target</span><span><i class="swatch" style="background:var(--unsat)"></i>UNSAT source</span><span><i class="swatch" style="background:var(--dead)"></i>DEAD preference</span></div>${antLiveHtml()}`;
      document.getElementById("metaca-board").innerHTML = `<div class="board-head"><h2>MetaCA · 8 condition→response jacks</h2><p>Binary policy entries in legacy truth-table-3 order. Default is the actual Emacs clamp map.</p></div><div class="presets"><button data-preset="meta-figure8">Figure 8 · k↦max(k−1,0)</button><button data-preset="meta-identity">identity · pure UNSAT</button></div>${shapeHtml(metaShape,true)}<div class="patch-area">${boardSvg("meta",metacaTerminals,state.metaMap,metaShape)}</div><div class="legend"><span><i class="swatch" style="background:var(--free)"></i>FREE target</span><span><i class="swatch" style="background:var(--chain)"></i>CHAIN cable</span><span><i class="swatch" style="background:var(--unsat)"></i>UNSAT self-loop</span></div>${metaLiveHtml()}`;
      setText("tick-readout",`tick ${state.tick}`);
    }

    function renderLive() {
      const antLive=document.querySelector("#ant-board .live");
      const metaLive=document.querySelector("#metaca-board .live");
      if(antLive) antLive.outerHTML=antLiveHtml();
      if(metaLive) metaLive.outerHTML=metaLiveHtml();
      setText("tick-readout",`tick ${state.tick}`);
    }

    function sweep(mapping, values, active) {
      const order=range(mapping.length); if(state.order==="descending") order.reverse();
      for(const k of order){ const j=mapping[k]; if(active(k,j)) values[j]=1-values[k]; }
    }
    function tick(){
      if(!state.running)return;
      sweep(state.metaMap,state.metaBits,()=>true);
      for(const mode of ["outbound","homebound"]){ const values=state.antValues[mode]; sweep(state.antMap,values,(k,j)=>values[k]!==null&&values[j]!==null); }
      state.tick++; renderLive();
    }
    function reschedule(){ clearInterval(state.timer); state.timer=setInterval(tick,state.tickMs); }

    document.addEventListener("click", e => {
      const jack=e.target.closest(".jack");
      if(jack){ const kind=jack.dataset.kind, side=jack.dataset.side, j=Number(jack.dataset.jack);
        if(side==="source") state.pending[kind]=j;
        else if(state.pending[kind]!==null){ const key=kind==="ant"?"antMap":"metaMap"; state[key][state.pending[kind]]=j; state.pending[kind]=null; }
        render(); return; }
      const button=e.target.closest("button[data-preset]"); if(!button)return;
      if(button.dataset.preset==="ant-identity")state.antMap=identity(14);
      if(button.dataset.preset==="ant-clamp")state.antMap=clampShift(14);
      if(button.dataset.preset==="meta-figure8")state.metaMap=clampShift(8);
      if(button.dataset.preset==="meta-identity")state.metaMap=identity(8);
      render();
    });
    document.getElementById("running").addEventListener("change",e=>state.running=e.target.checked);
    document.getElementById("tick-ms").addEventListener("change",e=>{state.tickMs=Math.max(40,Number(e.target.value)||180);e.target.value=state.tickMs;reschedule();});
    document.getElementById("write-order").addEventListener("change",e=>state.order=e.target.value);
    document.getElementById("ant-mode").addEventListener("change",e=>{state.mode=e.target.value;render();});
    document.getElementById("free-pin").addEventListener("change",e=>{state.pin=Number(e.target.value);render();});
    render(); reschedule();
  }

  if (typeof document !== "undefined") document.addEventListener("DOMContentLoaded", boot);
  return {identity, clampShift, analyse, metacaTerminals, antTerminals};
});
