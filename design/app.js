(() => {
const $ = s => document.querySelector(s);
const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const svg = (i, s = 30) => `<svg class="ai" width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${i}</svg>`;
const I = AI;
document.head.insertAdjacentHTML('beforeend', `<style>${AI_CSS}</style>`);
const playIcons = el => el.querySelectorAll('svg.ai').forEach(v => { v.classList.remove('play'); v.getBoundingClientRect(); v.classList.add('play'); });
document.addEventListener('pointerover', e => { const t = e.target.closest('.tap'); if (t && !t.contains(e.relatedTarget)) playIcons(t); });
document.addEventListener('pointerdown', e => { const t = e.target.closest('.tap'); if (t) playIcons(t); });
const W = 56, SW = 2.6;
const arc = (top, hh, start, sweep, x0 = .33, x1 = .67) => {
  const rx = W * (x1 - x0) / 2, ry = W * hh / 2, cx = W * (x0 + x1) / 2, cy = W * (top + hh / 2), rad = d => d * Math.PI / 180;
  const p = a => `${cx + rx * Math.cos(rad(a))} ${cy + ry * Math.sin(rad(a))}`;
  return `<path d="M${p(start)} A${rx} ${ry} 0 ${sweep > 180 ? 1 : 0} 1 ${p(start + sweep)}"/>`;
};
const line = (a, b, c, d) => `<path d="M${W * a} ${W * b} L${W * c} ${W * d}"/>`;
const dots = `<circle cx="${W * .36}" cy="${W * .40}" r="${SW * .9}" fill="currentColor" stroke="none"/><circle cx="${W * .64}" cy="${W * .40}" r="${SW * .9}" fill="currentColor" stroke="none"/>`;
const two = (x, y, w, h) => [[x, y, w / 2 - .004, h], [x + w / 2 + .004, y, w / 2 - .004, h]];
const EYE = {
  M01: { ar: 336 / 390, lid: '#6CBE61', e: [[.3393, .2077, .1488, .1897], [.494, .2077, .1429, .1897]] },
  M02: { ar: 339 / 374, lid: '', e: [] },
  M03: { ar: 343 / 377, lid: '#51AC53', e: two(.3615, .2122, .2828, .1989) },
  M04: { ar: 345 / 376, lid: '#70C261', e: two(.3391, .3005, .2812, .1915) },
  M05: { ar: 343 / 376, lid: '#71C261', e: two(.3557, .3378, .2886, .1915) },
  M06: { ar: 350 / 375, lid: '#67BE5B', e: two(.3686, .2987, .28, .1867) },
};
const moodImg = id => `<img src="moods/${id}.png" alt="" draggable="false">` + EYE[id].e.map(([x, y, w, h]) =>
  `<i class="lid" style="left:${x * 100}%;top:${y * 100}%;width:${w * 100}%;height:${h * 100}%;--lid:${EYE[id].lid}"></i>`).join('');
const MOODS = [
  ['M01', '좋아요', dots + arc(.46, .22, 15, 150)],
  ['M02', '평온해요', arc(.36, .08, 0, 180, .30, .42) + arc(.36, .08, 0, 180, .58, .70) + arc(.52, .12, 20, 140, .38, .62)],
  ['M03', '보통이에요', dots + line(.38, .64, .62, .64)],
  ['M04', '지쳤어요', line(.30, .42, .42, .42) + line(.58, .42, .70, .42) + arc(.60, .10, 200, 140, .42, .58)],
  ['M05', '울적해요', dots + arc(.60, .18, 200, 140)],
  ['M06', '예민해요', line(.30, .30, .43, .36) + line(.70, .30, .57, .36) + dots + `<path d="M${W*.34} ${W*.66} L${W*.42} ${W*.60} L${W*.50} ${W*.66} L${W*.58} ${W*.60} L${W*.66} ${W*.66}"/>`],
];
const face = (g, s) => `<svg width="${s}" height="${s}" viewBox="0 0 56 56" fill="none" stroke="currentColor" stroke-width="${SW}" stroke-linecap="round">${g}</svg>`;
const CARES = ['COMFORT', 'QUIET', 'RESET', 'REPLAY', 'PLACE', 'REST', 'MUSIC'];
const M = { M01: [85, 65, 55, 85, 80, 10, 90], M02: [65, 80, 60, 95, 55, 20, 85], M03: [75, 65, 50, 90, 75, 30, 85],
            M04: [65, 45, 70, 35, 35, 100, 20], M05: [70, 70, 95, 35, 80, 55, 85], M06: [93, 95, 90, 40, 55, 50, 80] };
const G = { G01: [85, 100, 95, 55, 35, 85, 85], G02: [50, 25, 80, 50, 100, 40, 95], G03: [80, 75, 45, 100, 55, 50, 90] };
const DEFAULT_GOAL = { M01: 'G03', M02: 'G03', M03: 'G02', M04: 'G01', M05: 'G02', M06: 'G01' };
const ALLOWED = { M01: ['G03'], M02: ['G02', 'G03'], M06: ['G01'] };
const GOAL = { G01: '차분해지기', G02: '기분 전환', G03: '기분 유지' };
const MAIN = { COMFORT: '따뜻한 시트를 준비했어요', QUIET: '오늘은 조용한 차량 환경을 추천해요', RESET: '잠시 멈춘 지금, 3분만 쉬어가요',
  REPLAY: '지난번 편안했던 설정을 다시 준비했어요', PLACE: '근처에 잠시 들를 만한 곳이 있어요', REST: '안전한 곳에 정차해 쉬어가세요', MUSIC: '지금 기분에 어울리는 플레이리스트가 있어요' };
const NAME = { COMFORT: '편안한 시트·공조', QUIET: '조용한 환경', RESET: '잠깐 휴식', REPLAY: '지난 설정 다시', PLACE: '들를 곳', REST: '쉬어가기', MUSIC: '플레이리스트' };
const BTN = { COMFORT: ['적용', '취소'], QUIET: ['적용', '취소'], RESET: ['시작', '닫기'], REPLAY: ['다시 적용', '취소'], PLACE: ['경유지 추가', '나중에'], REST: ['확인', ''], MUSIC: ['열기', '닫기'] };
const TAGS = { M01: ['잔잔한 어쿠스틱', '밝은 팝·인디팝', '좋아하는 활기찬 음악'], M02: ['로파이·연주곡', '산뜻한 인디팝', '부드러운 어쿠스틱'],
  M03: ['편안한 인스트루멘털', '경쾌한 팝', '평소 선호 음악'], M05: ['따뜻한 어쿠스틱', '밝은 기분 전환 음악', '익숙하고 편안한 음악'], M06: ['자극 적은 연주곡', '부드러운 리듬의 인디팝', '익숙한 저자극 음악'] };
const PLACE_KINDS = { G01: ['공원', '조용한 카페'], G02: ['카페', '전망 좋은 곳'], G03: ['자주 간 장소'] };
const FATIGUE = '피로가 느껴질 때는 안전한 곳에 정차해 쉬는 것이 먼저예요. 케어가 휴식을 대신할 수는 없어요.';
const WHY = {
  health: '기록한 주기와 증상을 케어 추천에만 써요. 끄면 일반 추천만 보여드려요.',
  learn: '좋았던 케어를 다음 추천에 더 자주 보여드려요.',
  location: '가까운 화장실·편의점과 들를 곳을 찾을 때만 쓰고, 저장하지 않아요.',
  vehicle: '버튼을 누른 뒤에만 시트·공조·조명을 바꿔요.',
  music: '기분에 맞는 플레이리스트를 열 때만 써요.',
  cards: '출발 전이나 오래 운전할 때 짧은 카드를 보여드려요.',
  fp: '가족계획 중일 때만 켜세요. 피임 용도로 쓸 수 없어요.',
};
const LEVEL = ['', '약하게', '보통', '강하게'];
const VEHICLE = ['COMFORT', 'QUIET', 'REPLAY'];
const PARK_FIRST = '안전하게 정차한 뒤 이용할 수 있어요';
const SYMPTOMS = ['복부 불편', '허리 불편', '더위·답답함', '예민함', '두통', '감각 불편', '피로', '졸림', '울적함', '생리량 많음', '화장실 걱정'];
const PARKED_ONLY = ['onboarding', 'reset', 'feedback', 'calendar', 'record', 'settings'];
const day = s => new Date(s + 'T00:00');
const addD = (d, n) => new Date(d.getFullYear(), d.getMonth(), d.getDate() + n);
const md = d => `${d.getMonth() + 1}/${d.getDate()}`;
const same = (a, b) => a.getTime() === b.getTime();
const PROFILES = [
  { role: '나', color: '#BFE8D6', cycle: true, comfort: ['시트 열선 2단계', '실내 온도 23도'], replay: ['시트 열선 2단계', '실내 온도 23도', '저장한 시트 위치'],
    ratings: { COMFORT: ['+', '+', '+'], REPLAY: ['+'] }, cold: true, cset: { heat: 2, temp: 23 }, specialist: true, last: day('2026-11-02'), mu: 30, w: 2, period: 5, stage: '개인화', onboarded: true },
  { role: '남편', color: '#C9D9F0', cycle: false, comfort: ['시트 통풍 1단계', '실내 온도 21도'], replay: null, ratings: {}, cold: false, cset: { vent: 1, temp: 21 } },
  { role: '가족', color: '#F5D6C8', cycle: true, comfort: ['시트 열선 2단계'], replay: null, ratings: {}, cold: true, cset: { heat: 2 }, long: true,
    last: day('2026-11-05'), mu: 29, w: 4, period: 5, stage: '시작', onboarded: true },
];
const SOS = {
  TOILET: [
    { nm: '서울시청 공중화장실', drive: 3, walk: 1, open: '개방시간상 이용 가능', park: '근처 주차 공간 있음', acc: '누구나 이용', rel: '여러 출처 일치' },
    { nm: '서울도서관 1층 화장실', drive: 4, walk: 2, open: '개방시간상 이용 가능', park: '전용 주차 공간 있음', acc: '조건부 이용', rel: '공식 출처' },
    { nm: '덕수궁 돌담길 공중화장실', drive: 6, walk: 2, open: '운영시간 미확인', park: '주차 정보 미확인', acc: '누구나 이용', rel: '공식 출처' },
  ],
  STORE: [
    { nm: 'CU 시청광장점', drive: 3, walk: 1, open: '개방시간상 이용 가능', park: '근처 주차 공간 있음', rel: '최근 현장 확인' },
    { nm: 'GS25 을지로입구점', drive: 5, walk: 1, open: '개방시간상 이용 가능', park: '전용 주차 공간 있음', rel: '여러 출처 일치' },
    { nm: '세븐일레븐 무교점', drive: 6, walk: 2, open: '운영시간 미확인', park: '주차 정보 미확인', rel: '정보 확인 필요' },
  ],
};
const S = { p: null, screen: 'home', driving: false, mood: null, goal: null, rec: null, care: null, applied: false, feedback: null,
  sosCat: null, sosLoading: false, sosOpen: -1, timer: 0, timerMin: 3, timerRest: false, periodOn: false, month: 0,
  delAsk: false, ob: 0, obA: {}, consents: { health: true, learn: true, location: true, vehicle: true, music: true, cards: true, fp: false },
  caps: { seat: true, fan: true, nav: true, music: true }, demo: false,
  today: day('2026-11-05'), drive: 'PARKED', driveMin: 0, cabin: { heat: 0, vent: 0, temp: 22, fan: 3, dim: false }, prevCabin: null,
  card: null, snooze: {}, cardsThisDrive: 0, tr1Done: false, tr2Done: false, tr3Day: 0, ask: null, sosMode: 'ok' };
const CARDS = {
  TR1: { text: '오늘의 케어를 준비할까요?', ok: '적용' },
  TR2: { text: '쉬어갈 곳을 경로에 추가할까요?', ok: '적용' },
  TR3: { text: '준비물을 확인해 보세요', sub: '생리용품, 여벌 속옷, 물티슈, 따뜻한 물', ok: '확인' },
};
const SOS_MSG = { gps: '위치를 확인할 수 없어요. 위치 권한과 GPS 상태를 확인해 주세요', net: '장소 정보를 불러오지 못했어요. 네트워크를 확인해 주세요' };
let tick = null, toastT = null;
function pScore(care) {
  const base = care === 'COMFORT' && S.p.cold ? 60 : 50;
  const r = (S.p.ratings[care] || []).slice(-5);
  if (!S.consents.learn || !r.length) return base;
  const L = r.filter(x => x === '+').length, N = r.filter(x => x === '-').length;
  return Math.max(0, Math.min(100, 50 + 10 * L - 15 * N));
}
function feasible(c) {
  const v = S.consents.vehicle;
  return { COMFORT: v && S.caps.seat, QUIET: v && S.caps.fan, RESET: !S.driving, REPLAY: v && !!S.p.replay && (S.p.ratings.REPLAY || []).includes('+'),
           PLACE: S.consents.location && (!S.driving || S.caps.nav), REST: true, MUSIC: S.consents.music && S.caps.music && S.mood !== 'M04' }[c];
}
function details(c) {
  return {
    COMFORT: S.p.comfort, REPLAY: S.p.replay || ['저장한 설정'], QUIET: ['바람 한 단계 낮춤', '실내 조명 낮춤', '이번 주행 동안 알림 끄기'],
    RESET: ['호흡 안내와 함께 3분 쉬어요'], REST: [S.driving ? '안전한 곳에서 잠시 쉬어가세요' : '10분 정도 쉬거나 가볍게 걸어보세요'],
    PLACE: PLACE_KINDS[S.goal].map(k => `근처 ${k}`), MUSIC: [TAGS[S.mood]?.[Object.keys(GOAL).indexOf(S.goal)] || '플레이리스트'],
  }[c];
}
function recommend() {
  const ranked = CARES.filter(feasible).map(c => { const i = CARES.indexOf(c);
    return { c, s: (3 * M[S.mood][i] + 3 * G[S.goal][i] + 2 * pScore(c) + 50 + 100) / 10, o: i }; })
    .sort((a, b) => b.s - a.s || a.o - b.o);
  if (S.mood === 'M04') return { kind: 'safety', main: 'REST', alts: ranked.filter(x => !['REST', 'MUSIC', 'PLACE'].includes(x.c)).slice(0, 2).map(x => x.c) };
  if (!ranked.length || ranked[0].s < 70) return { kind: 'general', list: ranked.map(x => x.c) };
  return { kind: 'main', main: ranked[0].c, alts: ranked.slice(1, 3).filter(x => ranked[0].s - x.s < 5).map(x => x.c), rest: ranked.slice(1).map(x => x.c) };
}
function forecast(p = S.p) {
  if (!p.cycle) return null;
  const E = addD(p.last, p.mu);
  return { E, rs: addD(E, -p.w), re: addD(E, p.w) };
}
function phase() {
  const f = forecast(); if (!f) return null;
  const t = S.today;
  return t < addD(S.p.last, S.p.period) ? 'P1' : t >= f.E ? 'OVERDUE' : t >= addD(f.E, -6) ? 'P5' : 'MID';
}
function checkCards() {
  if (!S.p || S.card || !S.consents.cards) return;
  const free = id => S.snooze[id] !== S.today.getTime();
  const f = forecast();
  if (!S.driving && f && same(S.today, addD(f.rs, -2)) && S.tr3Day !== S.today.getTime() && free('TR3')) { S.tr3Day = S.today.getTime(); S.card = 'TR3'; return; }
  if (S.cardsThisDrive >= 2) return;
  if (!S.driving && !S.tr1Done && ['P1', 'P5'].includes(phase()) && free('TR1')) { S.tr1Done = true; S.cardsThisDrive++; S.card = 'TR1'; return; }
  if (S.driving && !S.tr2Done && S.driveMin >= (S.p.long ? 60 : 90) && free('TR2')) { S.tr2Done = true; S.cardsThisDrive++; S.card = 'TR2'; }
  if (S.card) say(CARDS[S.card].text);
}
function go(screen) {
  if (S.driving && PARKED_ONLY.includes(screen)) return toast(PARK_FIRST);
  clearInterval(tick); S.screen = screen; S.delAsk = false; render();
}
function setDriving(d) { setDrive(d ? 'DRIVING' : 'PARKED'); }
function setDrive(state) {
  const was = S.driving;
  S.drive = state; S.driving = state !== 'PARKED';
  $('#home').classList.toggle('driving', S.driving);
  window.onmeMotion?.(!S.driving);
  if (S.driving && !was) S.tr2Done = false;
  if (!S.driving) { S.driveMin = 0; S.quietDrive = false; }
  if (!S.p) return;
  const d = S.driving;
  if (d && PARKED_ONLY.includes(S.screen)) { clearInterval(tick); S.screen = 'home'; toast('주행 중이라 홈으로 돌아왔어요'); }
  if (S.screen === 'result') S.rec = recommend();
  render();
}
let voiceOn = true;
function say(text) {
  if (!voiceOn || S.quietDrive || !('speechSynthesis' in window)) return;
  speechSynthesis.cancel();
  const u = new SpeechSynthesisUtterance(text);
  u.lang = 'ko-KR';
  u.voice = speechSynthesis.getVoices().find(v => v.lang.startsWith('ko')) || null;
  speechSynthesis.speak(u);
}
function toast(msg) {
  say(msg);
  const t = $('#toast'); t.innerHTML = svg(I.check, 22) + esc(msg); t.classList.add('show');
  clearTimeout(toastT); toastT = setTimeout(() => t.classList.remove('show'), 2400);
}
const head = (title, right = '') => `<div class="head"><div class="back g tap" data-a="go" data-v="home" aria-label="홈">${svg(I.back, 26)}</div><h2>${title}</h2><div class="right">${right}</div></div>`;
const careOrb = (c, s = 52) => `<div class="care">${svg(I[c], s)}</div>`;
const pbtn = (label, a, v = '') => `<div class="btn p tap" data-a="${a}" data-v="${v}">${label}<span class="chev">${svg(I.chev, 19)}</span></div>`;
const qbtn = (label, a, v = '', cls = 'q') => `<div class="btn ${cls} tap" data-a="${a}" data-v="${v}">${label}</div>`;
const greeting = () => { const h = new Date().getHours(); return h >= 5 && h <= 10 ? '좋은 아침이에요' : h >= 11 && h <= 16 ? '좋은 오후예요' : h >= 17 && h <= 21 ? '좋은 저녁이에요' : '편안한 밤이에요'; };

const V = {
  home() {
    const slot = S.drive === 'UNKNOWN' ? `<div class="slot plain">차량 상태를 확인할 수 없어 주행 중과 같이 보여드려요</div>`
      : S.driving ? `<div class="slot plain">주행 중에는 기분 고르기와 급할 때 찾기만 쓸 수 있어요</div>`
      : S.feedback ? `<div class="slot g"><p><b>방금 케어는 어땠나요?</b></p>${pbtn('평가하기', 'go', 'feedback')}</div>`
      : S.p.cycle && !S.p.onboarded ? `<div class="slot g"><p>몇 가지만 알려주시면 케어가 더 잘 맞아요</p>${pbtn('초기 질문 시작하기', 'go', 'onboarding')}</div>`
      : `<div class="slot plain">기분을 고르면 지금 할 수 있는 케어를 찾아 드려요</div>`;
    return `<div class="date">${S.today.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'long' })}</div>
      <div class="hero"><p>${greeting()}, ${esc(S.p.role)}</p><h1>지금 기분은 어떤가요?</h1></div>
      <div class="pond">${(S.compact ? [MOODS.slice(0, 3), MOODS.slice(3)] : [MOODS]).map((row, r) => `<div class="lake">${row.map(([id, label, g], i) => `<div class="drop tap" style="--i:${r * 3 + i}" data-a="mood" data-v="${id}"><div class="orb char"><div class="bob" style="--ar:${(EYE[id] || EYE.M02).ar}">${moodImg(id)}</div></div><span>${label}</span></div>`).join('')}</div>`).join('')}</div>
      ${slot}`;
  },
  result() {
    const [, label, g] = MOODS.find(m => m[0] === S.mood), r = S.rec;
    const seg = `<div class="seg g">${Object.entries(GOAL).filter(([id]) => !ALLOWED[S.mood] || ALLOWED[S.mood].includes(id)).map(([id, n]) => `<div class="tap ${id === S.goal ? 'on' : ''}" data-a="goal" data-v="${id}">${n}</div>`).join('')}</div>`;
    const top = r.kind === 'general' ? r.list[0] : r.main;
    const others = r.kind === 'general' ? r.list.slice(1, 3)
      : r.kind === 'safety' ? r.alts : [...r.alts, ...r.rest.filter(c => !r.alts.includes(c))].slice(0, 2);
    const tags = c => [`${label}일 때`, `${GOAL[S.goal]}에 좋아요`, ...((S.p.ratings[c] || []).includes('+') ? ['지난번 좋았어요'] : []), ...(c === 'RESET' ? ['정차 중에만'] : [])];
    const main = top ? `<div class="pick g ${r.kind === 'safety' ? 'safe' : ''}">
        <div class="pk-m">
          <div class="pk-top">${careOrb(top)}<span class="badge">${r.kind === 'safety' ? '먼저 쉬어가요' : '가장 추천'}</span></div>
          <h3>${MAIN[top]}</h3>
          <div class="tags">${(r.kind === 'safety' ? ['안전 우선'] : tags(top)).map(t => `<i>${t}</i>`).join('')}</div>
        </div>
        <div class="pk-plan"><span class="k">${r.kind === 'safety' ? '안내' : '적용하면'}</span>
          ${(r.kind === 'safety' ? [FATIGUE] : details(top)).map(t => `<div class="pl">${svg(I.check, 14)}<span>${t}</span></div>`).join('')}
          ${pbtn(BTN[top][0], 'care', top)}</div></div>` : ''
    const row = (c, i) => `<div class="orow g tap" data-a="care" data-v="${c}"><span class="ic">${svg(I[c], 30)}</span>
        <div><b>${NAME[c]}</b><span>${details(c)[0]}</span></div>${r.alts && i < r.alts.length ? '<em>이것도 괜찮아요</em>' : ''}<span class="chev">${svg(I.chev, 22)}</span></div>`;
    return head(`<span class="mini"><span class="mchar">${moodImg(S.mood)}</span>${label}</span>`, S.compact ? '' : seg) + (S.compact ? `<div style="margin-bottom:24px">${seg}</div>` : '')
      + `<div class="rows3 ${top ? '' : 'flat'}">${main}${others.map(row).join('')}</div>`;
  },
  confirm() {
    const k = S.care, [yes, no] = BTN[k], c = S.cabin, vehicle = VEHICLE.includes(k);
    const items = k === 'COMFORT' ? S.p.comfort : k === 'REPLAY' ? S.p.replay : ['바람 한 단계 낮춤', '실내 조명 낮춤', '이번 주행 동안 온미 알림 끄기'];
    const mini = (icon, text, a, v) => `<div class="mini-row act tap" data-a="${a}" data-v="${v}">${svg(I[icon], 26)}<span>${text}</span>${svg(I.chev, 18)}</div>`;
    const tag = TAGS[S.mood]?.[Object.keys(GOAL).indexOf(S.goal)] || '플레이리스트';
    const action = vehicle ? (S.applied ? `<div class="btns">${pbtn('확인', 'go', 'home')}${qbtn('되돌리기', 'revert')}</div>` : qbtn(no, 'go', 'result'))
      : k === 'PLACE' ? PLACE_KINDS[S.goal].slice(0, 2).map(p => mini('PLACE', `근처 ${p}`, 'send', p)).join('') + `<div class="link tap" data-a="go" data-v="home">${no}</div>`
      : k === 'MUSIC' ? [tag, `${tag} 모음`].map(t => mini('MUSIC', t, 'music', t)).join('') + `<div class="link tap" data-a="go" data-v="home">${no}</div>`
      : k === 'REST' ? pbtn(S.driving ? yes : '10분 쉬기', 'rest')
      : `<div class="btns">${pbtn(yes, 'reset')}${qbtn(no, 'go', 'home')}</div>`;
    return head(NAME[k]) + `<div class="jadeview">
        <div class="stage">
          ${S.applied ? `<span class="badge">${svg(I.check, 16)} 적용했어요</span>` : ''}
          <h3>${k === 'COMFORT' && S.p.comfort[0].includes('통풍') ? '시원한 통풍을 준비했어요' : MAIN[k]}</h3>
        </div>
        <div class="jcards">
          <div class="jc g main"><span class="k">다음</span><div class="jc-act">${action}</div></div>
          <div class="jc g"><span class="k">지금 차 안</span><b>${c.temp}°C</b><em>열선 ${c.heat}단계 · 통풍 ${c.vent}단계 · 바람 ${c.fan}단계${c.dim ? ' · 조명 낮춤' : ''}</em></div>
          <div class="jc g"><span class="k">${vehicle ? '바뀐 설정' : '안내'}</span><div class="dets">${(vehicle ? items : details(k)).map(t => `<span>${t}</span>`).join('')}</div></div>
        </div>
      </div>`;
  },
  reset() {
    const opts = S.timerRest ? '' : `<div class="seg g">${[3, 5].map(m => `<div class="tap ${S.timerMin === m ? 'on' : ''}" data-a="len" data-v="${m}">${m}분</div>`).join('')}</div>`;
    return head(S.timerRest ? '쉬어가기' : '잠깐 휴식') + `<div class="breath">
      <div class="lung"><b id="clock"></b><span id="phase"></span></div>
      <p class="guide">${S.timerRest ? '10분 정도 쉬거나 가볍게 걸어보세요' : '숨을 천천히 들이쉬고, 더 길게 내쉬어요'}</p>
      ${opts}${qbtn('닫기', 'endtimer')}</div>`;
  },
  feedback() {
    const opts = [['+', '좋았어요', MOODS[0][2]], ['=', '보통', MOODS[2][2]], ['-', '별로였어요', MOODS[4][2]]];
    return head('케어 평가') + `<div class="fb"><h1>방금 ${NAME[S.feedback] || '케어'}는 어땠나요?</h1>
      <div class="rate">${opts.map(([v, l, g]) => `<div class="drop tap" style="--i:0" data-a="rate" data-v="${v}"><div class="orb">${face(g, 92)}</div><span>${l}</span></div>`).join('')}</div>
      <div class="link tap" data-a="rate" data-v="">건너뛰기</div></div>`;
  },
  sos() {
    const cat = S.sosCat, bar = `<div class="sosbar">${[['TOILET', '화장실'], ['STORE', '편의점']].map(([k, l]) => qbtn(l, 'sos', k, 'q g' + (cat === k ? ' on' : ''))).join('')}</div>`;
    let body = !cat ? `<p class="empty">무엇을 찾을까요? 가까운 곳 3곳을 보여드려요</p>`
      : S.sosLoading ? ''
      : S.sosMode === 'none' ? `<div class="panel g"><h4>근처에서 찾지 못했어요</h4><div class="btns" style="margin-top:24px">${qbtn('다시 검색', 'sos', cat)}${qbtn('반대 카테고리 검색', 'sos', cat === 'TOILET' ? 'STORE' : 'TOILET')}</div></div>`
      : SOS_MSG[S.sosMode] ? `<div class="panel g"><h4>${SOS_MSG[S.sosMode]}</h4><div style="margin-top:24px">${qbtn('다시 시도', 'sos', cat)}</div></div>`
      : (S.sosMode === 'weak' ? `<p class="sub" style="font-size:19px;margin:0 0 14px 12px">위치 정확도가 낮아 결과가 다를 수 있어요</p>` : '') + SOS[cat].map((x, i) => `<div class="place g"><div class="top"><div><div class="nm">${x.nm}</div>
          <div class="meta">약 ${x.drive + x.walk}분, 차량 약 ${x.drive}분 + 도보 약 ${x.walk}분 (추정) · ${x.open}</div></div>
          ${pbtn(S.driving ? '경유지 추가' : '안내', 'nav', i)}</div>
          ${S.driving ? '' : S.sosOpen === i ? `<div class="det">${x.park}, 빈자리는 보장되지 않아요<br>${x.acc ? x.acc + '<br>' : ''}${x.rel}</div><div class="more tap" data-a="more" data-v="${i}">접기</div>`
            : `<div class="more tap" data-a="more" data-v="${i}">자세히</div>`}</div>`).join('')
        + (cat === 'STORE' && !['none', 'gps', 'net'].includes(S.sosMode) ? `<p class="sub" style="font-size:19px;margin:6px 0 0 12px">생리용품 재고는 확인되지 않아요</p>` : '');
    return head('급할 때 찾기') + bar + `<div>${body}</div>`;
  },
  calendar() {
    const p = S.p, base = new Date(S.today.getFullYear(), S.today.getMonth() + S.month, 1);
    const { E, rs, re } = forecast(), prev = addD(p.last, -p.mu), ph = phase();
    const inP = (d, s) => d >= s && d < addD(s, p.period);
    const day = Math.round((S.today - p.last) / 864e5), until = Math.round((rs - S.today) / 864e5);
    const now = ph === 'P1' ? `생리 ${day + 1}일째` : ph === 'OVERDUE' ? '예정일 지남' : ph === 'P5' ? '곧 시작해요' : '평소 기간';
    const sub = ph === 'OVERDUE' ? '기록을 확인해 주세요' : until > 0 ? `다음 시작까지 약 ${until}일` : '예정 범위 안이에요';
    const R = 118, C = 2 * Math.PI * R, arc = (from, len, cls) => `<circle class="${cls}" r="${R}" cx="150" cy="150" transform="rotate(-90 150 150)" stroke-dasharray="${Math.max(0, len) / p.mu * C} ${C}" stroke-dashoffset="${-from / p.mu * C}"/>`;
    const ang = (day % p.mu) / p.mu * 2 * Math.PI - Math.PI / 2;
    const dial = `<svg viewBox="0 0 300 300" class="dial-svg">
        <circle class="track" r="${R}" cx="150" cy="150"/>${Array.from({length: p.mu}, (_, i) => { const a2 = i / p.mu * 2 * Math.PI - Math.PI / 2; return `<circle class="tick" r="2" cx="${150 + (R - 26) * Math.cos(a2)}" cy="${150 + (R - 26) * Math.sin(a2)}"/>`; }).join('')}
        ${arc(p.mu - 6, 6, 'pre')}${arc(Math.round((rs - p.last) / 864e5), Math.round((re - rs) / 864e5) + 1, 'rng')}${arc(0, p.period, 'per')}
        <circle class="me" r="11" cx="${150 + R * Math.cos(ang)}" cy="${150 + R * Math.sin(ang)}"/></svg>`;
    const cells = [];
    for (let i = 0; i < base.getDay(); i++) cells.push('<div></div>');
    for (let n = 1; n <= new Date(base.getFullYear(), base.getMonth() + 1, 0).getDate(); n++) {
      const d = new Date(base.getFullYear(), base.getMonth(), n), cls = ['dd'];
      if (inP(d, p.last) || inP(d, prev)) cls.push('p1'); else if (inP(d, E)) cls.push('pred'); else if (d >= addD(E, -6) && d < E) cls.push('p5');
      if (d >= rs && d <= re) cls.push('rng');
      if (same(d, S.today)) cls.push('today');
      cells.push(`<div class="${cls.join(' ')}"><span>${n}</span>${same(d, S.today) ? '<small>오늘</small>' : ''}</div>`);
    }
    const notes = (p.specialist && !p.specialistSeen ? `<div class="note-chip">강한 불편이 반복되고 있어요. 전문의와 상담해 보는 것도 좋아요.<b class="tap" data-a="specialist">확인</b></div>` : '');
    return head('캘린더', pbtn(S.compact ? '기록' : '오늘 기록하기', 'go', 'record')) + `<div class="cal2">
      <div class="cside">
        ${notes}
        <div class="dial g ${ph === 'P1' ? 'on' : ''}">${dial}<div class="dial-c"><b>${now}</b><span>${sub}</span></div></div>
        <div class="crow">
          <div class="cc g"><span>다음 시작 예상</span><b>${md(rs)} – ${md(re)}</b><em>추정</em></div>
          <div class="cc g"><span>예측 정확도</span><b>${p.stage}</b><div class="steps3">${['시작', '학습 중', '개인화'].map((x, i) => `<i class="${i <= ['시작', '학습 중', '개인화'].indexOf(p.stage) ? 'f' : ''}"></i>`).join('')}</div></div>
        </div>
        <p class="fine">예상 범위는 추정이며 실제와 다를 수 있어요. 피임이나 임신 판단에 사용할 수 없어요.</p>
      </div>
      <div class="month2 g">
        <div class="mh2"><b>${base.getMonth() + 1}월</b><span>${base.getFullYear()}</span>
          <div class="mnav">${S.month ? `<div class="btn q tap today-btn" data-a="month" data-v="${-S.month}">오늘</div>` : ''}<div class="back tap" data-a="month" data-v="-1">${svg(I.back, 22)}</div><div class="back tap" data-a="month" data-v="1">${svg(I.chev, 22)}</div></div></div>
        <div class="grid2">${['일', '월', '화', '수', '목', '금', '토'].map(w => `<div class="wd">${w}</div>`).join('')}${cells.join('')}</div>
        <div class="lg2"><span><i class="k p1"></i>생리</span><span><i class="k pred"></i>예상 생리</span><span><i class="k p5"></i>생리 전</span><span><i class="k rng"></i>예정 범위</span></div>
      </div>
    </div>`;
  },
  record() {
    const day = S.periodOn ? Math.round((S.today - S.p.last) / 864e5) + 1 : 0;
    const log = (S.p.log ||= {})[S.today.getTime()] ||= {};
    const picked = SYMPTOMS.map((n, i) => [n, log[i] || 0]).filter(([, v]) => v);
    return head(`${md(S.today)} 기록`) + `<div class="rec">
      <div class="left g">
        <div class="pstat ${S.periodOn ? 'on' : ''}"><span>생리</span><b>${S.periodOn ? `${day}일째` : '기록 없음'}</b></div>
        <div class="pbtn tap ${S.periodOn ? 'done' : ''}" data-a="period">${svg(I.drop, 37)}<b>${S.periodOn ? '시작 기록됨' : '오늘 시작'}</b></div>
        <div class="pbtn tap" data-a="periodEnd">${svg(I.check, 37)}<b>오늘 끝남</b></div>
      </div>
      <div class="right g">
        <div class="rh"><h4>오늘 느낀 것</h4><span>한 번 누르면 약하게, 두 번 보통, 세 번 강하게</span></div>
        <div class="chips">${SYMPTOMS.map((n, i) => { const v = log[i] || 0;
          return `<div class="chip tap lv${v}" data-a="sym" data-v="${i}"><b>${n}</b><span class="lvw">${LEVEL[v]}</span><span class="lvb">${[1, 2, 3].map(k => `<i class="${k <= v ? 'f' : ''}"></i>`).join('')}</span></div>`; }).join('')}</div>
        <div class="savebar"><span>${picked.length ? picked.map(([n, v]) => `${n} ${LEVEL[v]}`).join(', ') : '누르면 바로 저장돼요'}</span><div class="autosave ${S.savedFlash ? 'flash' : ''}">${svg(I.check, 18)} 자동 저장</div></div>
      </div></div>`;
  },
  settings() {
    const c = S.consents, tg = (k, l) => `<div class="tg tap" data-a="consent" data-v="${k}"><span>${l}<small>${WHY[k]}</small></span><i class="sw ${c[k] ? 'on' : ''}"></i></div>`;
    const cap = (k, l) => `<div class="tg tap" data-a="cap" data-v="${k}"><span>${l}</span><i class="sw ${S.caps[k] ? 'on' : ''}"></i></div>`;
    return head('설정') + `<div class="cols">
      <div class="panel g"><h4>개인정보·동의</h4>
        ${S.p.cycle ? tg('health', '주기·증상으로 케어 맞춤') : ''}${tg('learn', '케어 평가로 추천 학습')}${tg('location', '위치 (주변 장소·급할 때)')}
        ${tg('vehicle', '차량 설정 제어')}${tg('music', '음악 서비스 연결')}${tg('cards', '상황 알림 카드')}${S.p.cycle ? tg('fp', '배란 추정 표시 (가족계획)') : ''}</div>
      <div class="panel g"><h4>데이터</h4><div class="stack">
        ${S.p.cycle ? qbtn('초기 질문 다시', 'go', 'onboarding') : ''}${qbtn('데이터 내보내기', 'export')}
        ${S.delAsk ? `<div class="confirmrow">모든 기록과 설정을 지울까요? 되돌릴 수 없어요.<div class="btns">${qbtn('삭제', 'delete', '', 'danger')}${qbtn('취소', 'delAsk', '0')}</div></div>` : qbtn('전체 삭제', 'delAsk', '1', 'danger')}</div>
        <h4 style="margin-top:36px">시연 모드</h4><div class="tg tap" data-a="demo"><span>시연 모드</span><i class="sw ${S.demo ? 'on' : ''}"></i></div>
        ${S.demo ? cap('seat', '시트 열선·통풍 지원') + cap('fan', '바람·조명 지원') + cap('nav', '내비 연동') + cap('music', '음악 서비스')
          + `<div class="q" style="margin:24px 0 12px;font-size:21px">급할 때 찾기 결과</div><div class="opts">${[['ok', '정상'], ['weak', '위치 부정확'], ['none', '결과 없음'], ['gps', '위치 오류'], ['net', '네트워크 오류']]
            .map(([k, l]) => `<div class="opt tap ${S.sosMode === k ? 'on' : ''}" data-a="sosMode" data-v="${k}">${l}</div>`).join('')}</div>`
          + `<div class="btns" style="margin-top:20px">${qbtn('사용자 A 불러오기', 'loadA')}${qbtn('시연 초기화', 'resetDemo')}</div>` : ''}</div></div>`;
  },
  onboarding() {
    const A = S.obA, step = S.ob, opts = (k, list) => `<div class="opts">${list.map(o => `<div class="opt tap ${A[k] === o ? 'on' : ''}" data-a="ans" data-v="${k}|${o}">${o}</div>`).join('')}</div>`;
    const timing = ['생리 전', '생리 중', '둘 다'];
    const pages = [
      () => `<div class="q">온미가 사용할 정보를 골라 주세요. 언제든 설정에서 바꿀 수 있어요.</div>` + ['health|주기·증상으로 케어 맞춤', 'learn|케어 평가로 추천 학습', 'location|위치 (주변 장소·급할 때)', 'vehicle|차량 설정 제어', 'music|음악 서비스 연결', 'cards|상황 알림 카드']
        .map(x => { const [k, l] = x.split('|'); return `<div class="tg tap" data-a="consent" data-v="${k}"><span>${l}<small>${WHY[k]}</small></span><i class="sw ${S.consents[k] ? 'on' : ''}"></i></div>`; }).join(''),
      () => `<div><div class="q">마지막 생리 시작일</div><div class="stepper"><div class="back tap" data-a="ago" data-v="1">${svg(I.minus, 28)}</div>${md(addD(S.today, -(A.ago || 0)))} (${A.ago ? A.ago + '일 전' : '오늘'})<div class="back tap" data-a="ago" data-v="-1">${svg(I.plus, 28)}</div></div></div>`
        + `<div class="tg tap" data-a="ans" data-v="unsure|${A.unsure === '1' ? '' : '1'}"><span>정확하지 않아요</span><i class="sw ${A.unsure === '1' ? 'on' : ''}"></i></div>`,
      () => `<div><div class="q">평소 주기</div>${opts('cycle', ['21–24일', '25–28일', '29–32일', '33–35일', '36일 이상', '모르겠어요'])}</div>
        <div><div class="q">주기가 얼마나 일정한가요?</div>${opts('reg', ['거의 같아요', '며칠 차이', '많이 달라요', '모르겠어요'])}</div>
        <div><div class="q">생리 기간</div>${opts('dur', ['3–4일', '5–6일', '7일 이상', '모르겠어요'])}</div>`,
      () => `<div class="q">자주 불편한 시기가 있나요? 누를 때마다 생리 전, 생리 중, 둘 다, 없음 순으로 바뀌어요</div><div class="opts">${SYMPTOMS.map((s, i) => { const t = (A.sig || {})[i];
          return `<div class="opt tap ${t != null ? 'on' : ''}" data-a="sig" data-v="${i}">${s}${t != null ? ', ' + timing[t] : ''}</div>`; }).join('')}</div>`,
      () => `<div><div class="q">차 안 온도</div>${opts('thermal', ['추위를 잘 타요', '더위를 잘 타요', '보통이에요'])}</div>
        <div><div class="q">좋아하는 열선 단계</div>${opts('heat', ['안 써요', '1단계', '2단계', '3단계'])}</div>
        <div><div class="q">운전 패턴</div>${opts('pattern', ['출퇴근', '장거리 잦음', '가끔'])}</div>
        <div><div class="q">해당하는 상황</div>${opts('sit', ['호르몬 피임약', '임신 중', '출산 후', '폐경 전후', '해당 없음', '답하지 않을래요'])}</div>`,
    ];
    const nav = `<div class="btns" style="margin-top:auto">${step ? qbtn('이전', 'ob', '-1') : qbtn('나중에', 'go', 'home')}${step === 2 || step === 3 ? qbtn('건너뛰기', 'ob', '1') : ''}${step < 4 ? pbtn('다음', 'ob', '1') : pbtn('완료', 'obDone')}</div>`;
    return head('초기 질문', `<div class="steps">${[0, 1, 2, 3, 4].map(i => `<i class="${i <= step ? 'f' : ''}"></i>`).join('')}</div>`) + `<div class="ob g">${pages[step]()}${nav}</div>`;
  },
};

function rail() {
  const on = s => S.screen === s ? 'on' : '', p = S.p;
  return `<div class="rb me tap" data-a="switch" title="프로필 바꾸기"><i style="background:radial-gradient(circle at 36% 30%, #fff, ${p.color})">${esc(p.role[0])}</i>${esc(p.role)}</div><hr>
    <div class="rb tap ${on('home') || (['result', 'confirm'].includes(S.screen) ? 'on' : '')}" data-a="go" data-v="home">${svg(I.home, 28)}홈</div>
    ${p.cycle ? `<div class="rb tap park-only ${on('record')}" data-a="go" data-v="record">${svg(I.rec, 28)}기록</div><div class="rb tap park-only ${on('calendar')}" data-a="go" data-v="calendar">${svg(I.cal, 28)}캘린더</div>` : ''}
    <div class="rb tap park-only ${on('settings')}" data-a="go" data-v="settings">${svg(I.set, 28)}설정</div>
    <div class="rb sos tap ${on('sos')}" data-a="go" data-v="sos"><i class="dot"></i>급할 때</div>`;
}
function card() {
  if (!S.card) return '';
  const c = CARDS[S.card];
  return `<div class="cardx g"><div class="ct"><b>${c.text}</b>${c.sub ? `<span>${c.sub}</span>` : ''}</div>${pbtn(c.ok, 'cardOk')}${S.card === 'TR3' ? '' : qbtn('나중에', 'cardLater')}</div>`;
}
function ask() {
  if (!S.ask) return '';
  const ans = [['맞았어요', '예상 범위 안', 'ok', I.check], ['더 빨랐어요', '범위보다 일찍', 'early', I.back], ['더 늦었어요', '범위보다 늦게', 'late', I.chev]];
  return `<div class="scrim tap" data-a="askClose"><div class="modal g" data-stop="1"><div class="mclose tap" data-a="askClose" role="button" aria-label="닫기">${svg(I.close, 26)}</div><span class="mk">예상 범위</span><b class="mrange">${md(S.ask.rs)} ~ ${md(S.ask.re)}</b><h3>이 범위 안에 시작했나요?</h3>
    <div class="answers">${ans.map(([l, h, k, ic]) => `<div class="ans ${k} tap" data-a="answer" data-v="${l}"><i>${svg(ic, 22)}</i><span><b>${l}</b><em>${h}</em></span></div>`).join('')}</div></div></div>`;
}
function demoBar() {
  if (!S.demo) return '';
  const c = S.cabin, seg = [['PARKED', '정차'], ['DRIVING', '주행'], ['UNKNOWN', '신호 없음']];
  return `<div class="demobar g"><b>시연</b><div class="seg">${seg.map(([k, l]) => `<div class="tap ${S.drive === k ? 'on' : ''}" data-a="drive" data-v="${k}">${l}</div>`).join('')}</div>
    <div class="btn q tap" data-a="plus90">주행 +90분</div>
    <div class="dd"><div class="back tap" data-a="day" data-v="-1">${svg(I.back, 19)}</div>${md(S.today)}<div class="back tap" data-a="day" data-v="1">${svg(I.chev, 19)}</div></div>
    <span class="cab">열선 ${c.heat} · 통풍 ${c.vent} · ${c.temp}°C · 바람 ${c.fan} · 조명 ${c.dim ? '낮춤' : '기본'}${S.driving ? ` · 주행 ${S.driveMin}분` : ''}</span></div>`;
}
function cycleProgress() {
  const p = S.p;
  if (!p.cycle) return .5;
  const days = Math.round((S.today - p.last) / 864e5);
  return ((days % p.mu) + p.mu) % p.mu / p.mu;
}
function mountJade() {
  let slot = $('#bg3d');
  if (!slot) { slot = document.createElement('div'); slot.id = 'bg3d'; $('#home').prepend(slot); }
  const on = S.screen === 'confirm';
  $('#home').classList.toggle('full3d', on);
  if (!on) { slot.innerHTML = ''; slot.dataset.src = ''; return; }
  const run = !S.driving && !matchMedia('(prefers-reduced-motion: reduce)').matches;
  const p = S.p, mu = Math.min(60, Math.max(15, p.cycle ? p.mu : 28)), first = new Date(S.today.getFullYear(), S.today.getMonth(), 1);
  const rel = p.cycle ? Math.round((p.last - first) / 864e5) : 0, count = new Date(S.today.getFullYear(), S.today.getMonth() + 1, 0).getDate();
  const q = new URLSearchParams({ month: `${S.today.getFullYear()}-${String(S.today.getMonth() + 1).padStart(2, '0')}`, day: S.today.getDate(),
    period: Math.min(count, ((rel % mu) + mu) % mu + 1), cycle: mu, periodDays: p.cycle ? p.period : 5, restricted: run ? 0 : 1, theme: 'light' });
  const src = `cycle/index.html?${q}`;
  if (slot.dataset.src !== src) { slot.dataset.src = src; slot.innerHTML = `<iframe title="주기 옥" src="${src}"></iframe>`; }
}
function sosOverlay() {
  if (S.screen !== 'sos' || !S.sosLoading) return '';
  const what = S.sosCat === 'TOILET' ? '화장실' : '편의점', st = S.sosStep || 0;
  const steps = ['현재 위치 확인', st >= 1 ? `${[1, 3][Math.min(st - 1, 1)]}km 안에서 ${what} 찾는 중` : `가까운 ${what} 찾기`, '가까운 순으로 정리'];
  return `<div class="sosfull"><h2>가까운 ${what}을 찾고 있어요</h2><p>위치는 찾을 때만 쓰고 저장하지 않아요</p>
    <div class="radar"><i></i><i></i><i></i><i></i><b>${svg(I.PLACE, 48)}</b></div>
    <div class="steps">${steps.map((l, i) => `<div class="step ${i < st ? 'done' : i === st ? 'now' : ''}"><span>${i < st ? svg(I.check, 20) : ''}</span>${l}</div>`).join('')}</div>
    ${qbtn('취소', 'sosCancel')}</div>`;
}
function render() {
  document.documentElement.dataset.screen = S.screen;
  checkCards();
  $('#home').classList.toggle('demo', S.demo);
  $('#home').classList.toggle('hascard', !!S.card);
  $('#rail').innerHTML = rail();
  $('#view').innerHTML = V[S.screen]();
  $('#over').innerHTML = card() + sosOverlay() + ask() + demoBar();
  if (S.screen === 'reset') startTimer();
  mountJade();
}
function startTimer() {
  clearInterval(tick);
  const total = (S.timerRest ? 10 : S.timerMin) * 60, t0 = Date.now() - (total - S.timer) * 1000;
  const paint = () => {
    S.timer = Math.max(0, total - Math.floor((Date.now() - t0) / 1000));
    $('#clock').textContent = `${Math.floor(S.timer / 60)}:${String(S.timer % 60).padStart(2, '0')}`;
    const inBreath = ((Date.now() - t0) / 1000) % 10 < 4;
    const ph = S.timerRest ? '' : inBreath ? '들이쉬어요' : '천천히 내쉬어요';
    if (ph && ph !== $('#phase').textContent) say(ph);
    $('#phase').textContent = ph;
    if (!S.timer) endTimer();
  };
  paint(); tick = setInterval(paint, 250);
}
function endTimer() { clearInterval(tick); S.feedback = S.timerRest ? 'REST' : 'RESET'; go('feedback'); }
const A = {
  go: v => go(v),
  switch: () => { clearInterval(tick); window.onmeSwitch(); },
  mood: (v, el) => { if (S.driving) return A.pick(v); el.classList.add('ring'); setTimeout(() => A.pick(v), 220); },
  pick: v => { S.mood = v; S.goal = DEFAULT_GOAL[v]; S.rec = recommend(); go('result'); },
  goal: v => { if (ALLOWED[S.mood] && !ALLOWED[S.mood].includes(v)) return; S.goal = v; S.rec = recommend(); render(); },
  care: v => { S.care = v; S.applied = false; if (VEHICLE.includes(v)) return A.apply(); go('confirm'); },
  apply: () => {
    S.prevCabin = { ...S.cabin };
    const c = S.care, set = c === 'QUIET' ? { fan: Math.max(1, S.cabin.fan - 1), dim: true } : c === 'REPLAY' ? PROFILES[0].cset : S.p.cset;
    Object.assign(S.cabin, set); if (c === 'QUIET') S.quietDrive = true; S.applied = true; S.feedback = S.care; toast('적용했어요'); go('confirm');
  },
  revert: () => { if (S.prevCabin) S.cabin = S.prevCabin; S.applied = false; S.feedback = null; toast('이전 설정으로 되돌렸어요'); go('result'); },
  send: () => { S.feedback = 'PLACE'; toast('내비로 보냈어요'); go('home'); },
  music: v => { S.feedback = 'MUSIC'; toast(`${v} 플레이리스트를 열었어요`); go('home'); },
  rest: () => { if (S.driving) { say('안전한 곳에서 잠시 쉬어가세요'); return go('home'); } S.timerRest = true; S.timer = 600; go('reset'); },
  reset: () => { S.timerRest = false; S.timer = S.timerMin * 60; go('reset'); },
  len: v => { S.timerMin = +v; S.timer = S.timerMin * 60; render(); },
  endtimer: () => endTimer(),
  rate: v => { if (v) (S.p.ratings[S.feedback] = S.p.ratings[S.feedback] || []).push(v); S.feedback = null; if (v) toast('다음 추천에 반영할게요'); go('home'); },
  sos: v => {
    clearTimeout(S.sosTimer); S.sosCat = v; S.sosLoading = true; S.sosStep = 0; S.sosOpen = -1; render();
    const next = () => { if (!S.sosLoading) return; S.sosStep++; if (S.sosStep > 2) S.sosLoading = false; if (S.screen === 'sos') render(); if (S.sosLoading) S.sosTimer = setTimeout(next, 650); };
    S.sosTimer = setTimeout(next, 650);
  },
  sosCancel: () => { clearTimeout(S.sosTimer); S.sosLoading = false; S.sosCat = null; render(); },
  nav: () => { toast('내비로 보냈어요'); },
  more: v => { S.sosOpen = S.sosOpen === +v ? -1 : +v; render(); },
  month: v => { S.month += +v; render(); },
  period: () => {
    const f = forecast();
    if (f && !S.periodOn) S.ask = { rs: f.rs, re: f.re };
    S.periodOn = true; S.p.last = S.today; S.p.stage = S.p.stage === '시작' ? '학습 중' : S.p.stage; toast('생리 시작을 기록했어요'); render();
  },
  askClose: () => { S.ask = null; render(); },
  answer: v => {
    S.ask = null; S.p.misses = v === '맞았어요' ? 0 : (S.p.misses || 0) + 1;
    if (S.p.misses >= 2) { S.p.misses = 0; S.ob = 2; toast('평소 주기를 다시 알려주세요'); return go('onboarding'); }
    toast('예측에 반영할게요'); render();
  },
  specialist: () => { S.p.specialistSeen = true; render(); },
  cardOk: () => {
    const id = S.card; S.card = null;
    if (id === 'TR2') toast(`${SOS.TOILET[0].nm}을 경유지로 보냈어요`);
    if (id === 'TR1') { S.screen = 'home'; toast('지금 기분을 골라 주세요'); }
    render();
  },
  cardLater: () => { S.snooze[S.card] = S.today.getTime(); S.card = null; render(); },
  drive: v => setDrive(v),
  plus90: () => { if (!S.driving) setDrive('DRIVING'); S.driveMin += 90; render(); },
  day: v => { S.today = addD(S.today, +v); render(); },
  sosMode: v => { S.sosMode = v; render(); },
  loadA: () => { window.onmeSwitch(); setTimeout(() => document.querySelector('.card')?.click(), 50); },
  resetDemo: () => { Object.assign(S, { today: day('2026-11-05'), cabin: { heat: 0, vent: 0, temp: 22, fan: 3, dim: false }, snooze: {}, card: null, sosMode: 'ok', tr1Done: false, cardsThisDrive: 0 }); setDrive('PARKED'); toast('시연을 초기화했어요'); },
  periodEnd: () => { S.periodOn = false; toast('생리 종료를 기록했어요'); render(); },
  sym: v => { const log = (S.p.log ||= {})[S.today.getTime()] ||= {}; log[v] = ((log[v] || 0) + 1) % 4; S.savedFlash = true; render(); clearTimeout(A.ft); A.ft = setTimeout(() => { S.savedFlash = false; if (S.screen === 'record') render(); }, 900); },
  consent: v => { S.consents[v] = !S.consents[v]; render(); },
  cap: v => { S.caps[v] = !S.caps[v]; render(); },
  demo: () => { S.demo = !S.demo; render(); },
  export: () => toast('내보낼 파일을 만들었어요'),
  delAsk: v => { S.delAsk = v === '1'; render(); },
  delete: () => { S.p.ratings = {}; S.p.log = {}; S.feedback = null; S.delAsk = false; toast('모든 기록을 지웠어요'); go('home'); },
  ob: v => { S.ob = Math.max(0, Math.min(4, S.ob + +v)); render(); },
  obDone: () => { S.p.onboarded = true; S.p.last = addD(S.today, -(S.obA.ago || 0)); S.ob = 0; toast('초기 질문을 저장했어요'); go('home'); },
  ans: v => { const [k, o] = v.split('|'); S.obA[k] = S.obA[k] === o ? undefined : o; render(); },
  ago: v => { S.obA.ago = Math.max(0, Math.min(90, (S.obA.ago || 0) + +v)); render(); },
  sig: v => { const s = S.obA.sig = S.obA.sig || {}; s[v] = s[v] == null ? 0 : s[v] === 2 ? null : s[v] + 1; render(); },
};
$('#home').innerHTML = `<nav class="rail g" id="rail"></nav><div id="view"></div><div id="over"></div><div class="toast g" id="toast"></div>`;
$('#home').addEventListener('click', e => { const t = e.target.closest('[data-a], [data-stop]'); if (t && t.dataset.a) A[t.dataset.a](t.dataset.v, t); });
document.addEventListener('keydown', e => { if (e.key === 'Escape' && S.ask) A.askClose(); });

window.App = {
  setCompact(c) { S.compact = c; $('#home').classList.toggle('compact', c); if (S.p) render(); },
  open(i) { S.p = PROFILES[i]; Object.assign(S, { screen: 'home', mood: null, rec: null, care: null, feedback: null, sosCat: null, month: 0, card: null, ask: null, tr1Done: false, cardsThisDrive: 0, periodOn: false }); render(); },
  setDriving,
  setVoice(on) { voiceOn = on; if (!on && 'speechSynthesis' in window) speechSynthesis.cancel(); },
  act: (a, v) => a === 'mood' ? A.pick(v) : A[a](v),
};
})();
