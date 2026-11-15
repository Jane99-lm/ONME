// AnimateIcons (MIT) / Lucide (ISC), see design/tools/ICONS-NOTICE.md
const AI_CSS = `.ai *{transform-box:fill-box;transform-origin:center}
@keyframes aik0_0{0%{scale:1}35%{scale:0.8}75%{scale:1.06}100%{scale:1}}
.ai.play .k0{animation:aik0_0 0.7s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik1_0{0%{opacity:0}100%{opacity:1}}
@keyframes aik1_1{0%{stroke-dashoffset:25}100%{stroke-dashoffset:0}}
.ai.play .k1{animation:aik1_0 0.25s cubic-bezier(0.42,0,0.58,1) 0.35s both,aik1_1 0.45s cubic-bezier(0,0,0.58,1) 0.35s both}
@keyframes aik2_0{0%{scale:1 1}50%{scale:0.3 1}100%{scale:1 1}}
.ai.play .k2{animation:aik2_0 0.3s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik3_0{0%{scale:1 1}50%{scale:0.3 1}100%{scale:1 1}}
.ai.play .k3{animation:aik3_0 0.3s cubic-bezier(0.42,0,0.58,1) 0.08s both}
@keyframes aik4_0{0%{scale:1 1}50%{scale:0.3 1}100%{scale:1 1}}
.ai.play .k4{animation:aik4_0 0.3s cubic-bezier(0.42,0,0.58,1) 0.16s both}
@keyframes aik5_0{0%{scale:1 1}50%{scale:0.3 1}100%{scale:1 1}}
.ai.play .k5{animation:aik5_0 0.3s cubic-bezier(0.42,0,0.58,1) 0.24s both}
@keyframes aik6_0{0%{rotate:0deg}25%{rotate:-8deg}50%{rotate:4deg}75%{rotate:-4deg}100%{rotate:0deg}}
@keyframes aik6_1{0%{translate:0px 0}25%{translate:-1.5px 0}50%{translate:1px 0}75%{translate:-1px 0}100%{translate:0px 0}}
@keyframes aik6_2{0%{translate:0 0px}25%{translate:0 1px}50%{translate:0 -0.5px}75%{translate:0 0.8px}100%{translate:0 0px}}
.ai.play .k6{animation:aik6_0 0.8s cubic-bezier(0.42,0,0.58,1) 0s both,aik6_1 0.8s cubic-bezier(0.42,0,0.58,1) 0s both,aik6_2 0.8s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik7_0{0%{translate:0 0px}40%{translate:0 1.6px}75%{translate:0 -0.3px}100%{translate:0 0px}}
.ai.play .k7{animation:aik7_0 0.6s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik8_0{0%{translate:0 0px}40%{translate:0 1.6px}75%{translate:0 -0.3px}100%{translate:0 0px}}
.ai.play .k8{animation:aik8_0 0.6s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik9_0{0%{scale:1 1}40%{scale:1 1}65%{scale:1 0.96}100%{scale:1 1}}
.ai.play .k9{animation:aik9_0 0.6s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik10_0{0%{opacity:1}50%{opacity:0.6}100%{opacity:1}}
.ai.play .k10{animation:aik10_0 0.6s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik11_0{0%{opacity:1}50%{opacity:0.6}100%{opacity:1}}
.ai.play .k11{animation:aik11_0 0.6s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik12_0{0%{opacity:1}50%{opacity:0.6}100%{opacity:1}}
.ai.play .k12{animation:aik12_0 0.6s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik13_0{0%{opacity:1}50%{opacity:0.6}100%{opacity:1}}
.ai.play .k13{animation:aik13_0 0.6s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik14_0{0%{opacity:1}50%{opacity:0.6}100%{opacity:1}}
.ai.play .k14{animation:aik14_0 0.6s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik15_0{0%{opacity:1}50%{opacity:0.6}100%{opacity:1}}
.ai.play .k15{animation:aik15_0 0.6s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik16_0{0%{translate:0px 0}50%{translate:3px 0}100%{translate:0px 0}}
.ai.play .k16{animation:aik16_0 0.7s cubic-bezier(0.42,0,0.58,1) 0.05s both}
@keyframes aik17_0{0%{translate:0px 0}50%{translate:-3px 0}100%{translate:0px 0}}
.ai.play .k17{animation:aik17_0 0.7s cubic-bezier(0.42,0,0.58,1) -0.05s both}
@keyframes aik18_0{0%{translate:0px 0}50%{translate:3px 0}100%{translate:0px 0}}
.ai.play .k18{animation:aik18_0 0.7s cubic-bezier(0.42,0,0.58,1) 0.05s both}
@keyframes aik19_0{0%{translate:0px 0}40%{translate:-2.5px 0}75%{translate:0.625px 0}100%{translate:0px 0}}
.ai.play .k19{animation:aik19_0 0.6s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik20_0{0%{translate:0px 0}40%{translate:2.5px 0}75%{translate:-0.625px 0}100%{translate:0px 0}}
.ai.play .k20{animation:aik20_0 0.6s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik21_0{0%{scale:0.9}60%{scale:1.12}100%{scale:1}}
@keyframes aik21_1{0%{opacity:0}100%{opacity:1}}
@keyframes aik21_2{0%{stroke-dashoffset:20}100%{stroke-dashoffset:0}}
.ai.play .k21{animation:aik21_0 0.55s cubic-bezier(0.42,0,0.58,1) 0s both,aik21_1 0.25s cubic-bezier(0,0,0.58,1) 0s both,aik21_2 0.5s cubic-bezier(0,0,0.58,1) 0s both}
@keyframes aik22_0{0%{scale:0.92}70%{scale:1.06}100%{scale:1}}
@keyframes aik22_1{0%{opacity:0}100%{opacity:1}}
@keyframes aik22_2{0%{stroke-dashoffset:52}100%{stroke-dashoffset:0}}
.ai.play .k22{animation:aik22_0 0.6s cubic-bezier(0.42,0,0.58,1) 0s both,aik22_1 0.25s cubic-bezier(0,0,0.58,1) 0s both,aik22_2 0.6s cubic-bezier(0,0,0.58,1) 0s both}
@keyframes aik23_0{0%{scale:1}50%{scale:1.2}100%{scale:1}}
@keyframes aik23_1{0%{rotate:0deg}100%{rotate:90deg}}
.ai.play .k23{animation:aik23_0 0.5s cubic-bezier(0.42,0,0.58,1) 0s both,aik23_1 0.5s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik24_0{0%{scale:1}50%{scale:0.7}100%{scale:1}}
@keyframes aik24_1{0%{rotate:0deg}100%{rotate:180deg}}
.ai.play .k24{animation:aik24_0 0.5s cubic-bezier(0.42,0,0.58,1) 0s both,aik24_1 0.5s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik25_0{0%{scale:1}50%{scale:1.2}100%{scale:1}}
@keyframes aik25_1{0%{rotate:0deg}100%{rotate:90deg}}
.ai.play .k25{animation:aik25_0 0.5s cubic-bezier(0.42,0,0.58,1) 0s both,aik25_1 0.5s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik26_0{0%{scale:1 0.94}50%{scale:1 1.03}100%{scale:1 1}}
@keyframes aik26_1{0%{opacity:0}100%{opacity:1}}
@keyframes aik26_2{0%{stroke-dashoffset:50}100%{stroke-dashoffset:0}}
.ai.play .k26{animation:aik26_0 0.7s cubic-bezier(0.42,0,0.58,1) 0s both,aik26_1 0.25s cubic-bezier(0.42,0,0.58,1) 0s both,aik26_2 0.7s cubic-bezier(0,0,0.58,1) 0s both}
@keyframes aik27_0{0%{rotate:0deg}25%{rotate:-16deg}50%{rotate:12deg}75%{rotate:-6deg}100%{rotate:0deg}}
.ai.play .k27{animation:aik27_0 0.8s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik28_0{0%{translate:-3px 0}100%{translate:0px 0}}
@keyframes aik28_1{0%{opacity:0}100%{opacity:1}}
@keyframes aik28_2{0%{stroke-dashoffset:-21}100%{stroke-dashoffset:0}}
.ai.play .k28{animation:aik28_0 0.55s cubic-bezier(0,0,0.58,1) 0.24s both,aik28_1 0.25s cubic-bezier(0.42,0,0.58,1) 0.24s both,aik28_2 0.55s cubic-bezier(0,0,0.58,1) 0.24s both}
@keyframes aik29_0{0%{translate:-3px 0}100%{translate:0px 0}}
@keyframes aik29_1{0%{opacity:0}100%{opacity:1}}
@keyframes aik29_2{0%{stroke-dashoffset:-29}100%{stroke-dashoffset:0}}
.ai.play .k29{animation:aik29_0 0.55s cubic-bezier(0,0,0.58,1) 0.12s both,aik29_1 0.25s cubic-bezier(0.42,0,0.58,1) 0.12s both,aik29_2 0.55s cubic-bezier(0,0,0.58,1) 0.12s both}
@keyframes aik30_0{0%{translate:-3px 0}100%{translate:0px 0}}
@keyframes aik30_1{0%{opacity:0}100%{opacity:1}}
@keyframes aik30_2{0%{stroke-dashoffset:-18}100%{stroke-dashoffset:0}}
.ai.play .k30{animation:aik30_0 0.55s cubic-bezier(0,0,0.58,1) 0s both,aik30_1 0.25s cubic-bezier(0.42,0,0.58,1) 0s both,aik30_2 0.55s cubic-bezier(0,0,0.58,1) 0s both}
@keyframes aik31_0{0%{scale:1}50%{scale:0.9}100%{scale:1}}
@keyframes aik31_1{0%{rotate:0deg}100%{rotate:-360deg}}
.ai.play .k31{animation:aik31_0 0.8s cubic-bezier(0.42,0,0.58,1) 0s both,aik31_1 0.8s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik32_0{0%{rotate:0deg}25%{rotate:-9deg}50%{rotate:7deg}75%{rotate:-3deg}100%{rotate:0deg}}
@keyframes aik32_1{0%{translate:0 0px}30%{translate:0 1px}60%{translate:0 0.3px}100%{translate:0 0px}}
.ai.play .k32{animation:aik32_0 0.7s cubic-bezier(0.42,0,0.58,1) 0s both,aik32_1 0.5s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik33_0{0%{translate:0 0px}50%{translate:0 -4px}100%{translate:0 0px}}
@keyframes aik33_1{0%{opacity:1}25%{opacity:0.5}50%{opacity:1}75%{opacity:0.4}100%{opacity:1}}
.ai.play .k33{animation:aik33_0 0.8s cubic-bezier(0.42,0,0.58,1) 0s both,aik33_1 0.8s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik34_0{0%{translate:0 0px}50%{translate:0 -4px}100%{translate:0 0px}}
@keyframes aik34_1{0%{opacity:1}25%{opacity:0.5}50%{opacity:1}75%{opacity:0.4}100%{opacity:1}}
.ai.play .k34{animation:aik34_0 0.8s cubic-bezier(0.42,0,0.58,1) 0s both,aik34_1 0.8s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik35_0{0%{translate:0 0px}50%{translate:0 -4px}100%{translate:0 0px}}
@keyframes aik35_1{0%{opacity:1}25%{opacity:0.5}50%{opacity:1}75%{opacity:0.4}100%{opacity:1}}
.ai.play .k35{animation:aik35_0 0.8s cubic-bezier(0.42,0,0.58,1) 0s both,aik35_1 0.8s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik36_0{0%{translate:0 0px}50%{translate:0 -2px}100%{translate:0 0px}}
.ai.play .k36{animation:aik36_0 0.6s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik37_0{0%{rotate:0deg}25%{rotate:-6deg}50%{rotate:0deg}75%{rotate:5deg}100%{rotate:0deg}}
@keyframes aik37_1{0%{translate:0 0px}25%{translate:0 -2px}50%{translate:0 0px}75%{translate:0 -1.2px}100%{translate:0 0px}}
.ai.play .k37{animation:aik37_0 0.8s cubic-bezier(0.42,0,0.58,1) 0s both,aik37_1 0.8s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik38_0{0%{scale:1}25%{scale:1.2}50%{scale:1}75%{scale:1}100%{scale:1}}
.ai.play .k38{animation:aik38_0 0.8s cubic-bezier(0.42,0,0.58,1) 0s both}
@keyframes aik39_0{0%{scale:1}25%{scale:1}50%{scale:1}75%{scale:1.2}100%{scale:1}}
.ai.play .k39{animation:aik39_0 0.8s cubic-bezier(0.42,0,0.58,1) 0s both}
`;
const AI = {
  home: '<g class="k0" style="transform-box:view-box;transform-origin:12px 21px"><path  d="M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><path class="k1" stroke-dasharray="25" d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"/></g>',
  rec: '<path  d="M13.4 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-7.4"/><path class="k2" style="transform-box:view-box;transform-origin:4px 6px" d="M2 6h4"/><path class="k3" style="transform-box:view-box;transform-origin:4px 10px" d="M2 10h4"/><path class="k4" style="transform-box:view-box;transform-origin:4px 14px" d="M2 14h4"/><path class="k5" style="transform-box:view-box;transform-origin:4px 18px" d="M2 18h4"/><path class="k6" style="transform-box:view-box;transform-origin:13px 11px" d="M21.378 5.626a1 1 0 1 0-3.004-3.004l-5.01 5.012a2 2 0 0 0-.506.854l-.837 2.87a.5.5 0 0 0 .62.62l2.87-.837a2 2 0 0 0 .854-.506z"/>',
  cal: '<path class="k7" d="M8 2v3"/><path class="k8" d="M16 2v3"/><g class="k9" style="transform-box:view-box;transform-origin:12px 21px"><path  d="M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2z"/><path  d="M3 9h18"/></g>',
  set: '<path class="k10" d="M10 5H3"/><path class="k11" d="M21 5h-7"/><path class="k12" d="M12 19H3"/><path class="k13" d="M21 19h-5"/><path class="k14" d="M21 12h-9"/><path class="k15" d="M8 12H3"/><path class="k16" d="M14 3v4"/><path class="k17" d="M16 17v4"/><path class="k18" d="M8 10v4"/>',
  back: '<path class="k19" d="m15 18-6-6 6-6"/>',
  chev: '<path class="k20" d="m9 18 6-6-6-6"/>',
  check: '<path class="k21" style="transform-box:view-box;transform-origin:12px 12px" stroke-dasharray="20" d="M5 13l4 4L19 7"/>',
  drop: '<path class="k22" style="transform-box:view-box;transform-origin:12px 13px" stroke-dasharray="52" d="M12 22a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5s-3.5-4-4-6.5c-.5 2.5-2 4.9-4 6.5C6 11.1 5 13 5 15a7 7 0 0 0 7 7z"/>',
  plus: '<path class="k23" style="transform-box:view-box;transform-origin:12px 12px" d="M5 12h14M12 5v14"/>',
  minus: '<path class="k24" style="transform-box:view-box;transform-origin:12px 12px" d="M5 12h14"/>',
  close: '<g class="k25" style="transform-box:view-box;transform-origin:12px 12px"><path  d="M18 6 6 18"/><path  d="m6 6 12 12"/></g>',
  COMFORT: '<path class="k26" style="transform-box:view-box;transform-origin:12px 20px" stroke-dasharray="50" d="M14 4v10.54a4 4 0 1 1-4 0V4a2 2 0 0 1 4 0Z"/>',
  QUIET: '<path class="k27" style="transform-box:view-box;transform-origin:12px 12px" d="M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401"/>',
  RESET: '<path class="k28" stroke-dasharray="21 21" d="M12.8 19.6A2 2 0 1 0 14 16H2"/><path class="k29" stroke-dasharray="29 29" d="M17.5 8a2.5 2.5 0 1 1 2 4H2"/><path class="k30" stroke-dasharray="18 18" d="M9.8 4.4A2 2 0 1 1 11 8H2"/>',
  REPLAY: '<g class="k31" style="transform-box:view-box;transform-origin:12px 12px"><path  d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path  d="M3 3v5h5"/></g>',
  PLACE: '<g class="k32" style="transform-box:view-box;transform-origin:12px 22px"><path  d="M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0"/><path  d="M9 10a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"/></g>',
  REST: '<path class="k33" d="M6 2v2"/><path class="k34" d="M10 2v2"/><path class="k35" d="M14 2v2"/><path class="k36" d="M16 8a1 1 0 0 1 1 1v8a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V9a1 1 0 0 1 1-1h14a4 4 0 1 1 0 8h-1"/>',
  MUSIC: '<g class="k37" style="transform-box:view-box;transform-origin:12px 12px"><path  d="M9 18V5l12-2v13"/><path class="k38" style="transform-box:view-box;transform-origin:6px 18px" d="M3 18a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"/><path class="k39" style="transform-box:view-box;transform-origin:18px 16px" d="M15 16a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"/></g>',
};
