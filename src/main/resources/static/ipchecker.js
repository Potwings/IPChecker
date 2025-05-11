const COLOR_PALETTE = [
  "#0d6efd", "#6610f2", "#6f42c1", "#d63384", "#dc3545",
  "#fd7e14", "#ffc107", "#198754", "#20c997", "#0dcaf0",
  "#6c757d", "#343a40", "#495057", "#adb5bd", "#f8f9fa"
];

window.onload = () => {
  loadIpRanges();
};

async function loadIpRanges() {
  try {
    const response = await fetch("/ipRanges");
    const data = await response.json();
    visualizeRanges(data);
  } catch (e) {
    console.error("IP 대역 불러오기 실패:", e);
  }
}

let globalRangeStart = 0;
let globalRangeEnd = 0;

function visualizeRanges(rangeMap) {
  const container = document.getElementById("visualization");
  container.innerHTML = "";

  const keys = Object.keys(rangeMap).map(k => parseInt(k)).sort(
      (a, b) => a - b);
  if (keys.length === 0) {
    return;
  }

  const min = Math.min(...keys);
  const max = Math.max(...keys.map(k => rangeMap[k]));
  const totalWidth = container.clientWidth;
  const range = max - min || 1;
  const baselineY = 70;

  const baseline = document.createElement("div");
  baseline.className = "baseline";
  container.appendChild(baseline);

  keys.forEach((start, index) => {
    const end = rangeMap[start];
    const leftRatio = (start - min) / range;
    const rightRatio = (end - min) / range;

    const barLeft = leftRatio * totalWidth;
    const barWidth = Math.max((rightRatio - leftRatio) * totalWidth, 2);

    const startIp = longToIp(start);
    const endIp = longToIp(end);

    const bar = document.createElement("div");
    bar.className = "ip-bar";
    bar.style.left = `${barLeft}px`;
    bar.style.top = `${baselineY - 10}px`;
    bar.style.width = `${barWidth}px`;
    bar.style.backgroundColor = COLOR_PALETTE[index % COLOR_PALETTE.length];
    container.appendChild(bar);

    const startLabel = document.createElement("div");
    startLabel.className = "ip-label left";
    startLabel.textContent = startIp;
    startLabel.style.left = `${barLeft}px`;
    startLabel.style.top = `${baselineY + 8}px`;
    container.appendChild(startLabel);

    const endLabel = document.createElement("div");
    endLabel.className = "ip-label right";
    endLabel.textContent = endIp;
    endLabel.style.left = `${barLeft + barWidth}px`;
    endLabel.style.top = `${baselineY + 8}px`;
    container.appendChild(endLabel);

    globalRangeStart = min;
    globalRangeEnd = max;
  });
}

async function addRange() {
  const cidr = document.getElementById("rangeInput").value.trim();
  if (!cidr || !cidr.includes("/")) {
    alert("CIDR 형식이 잘못되었습니다.");
    return;
  }

  try {
    const response = await fetch("/ipRanges", {
      method: "POST",
      headers: {"Content-Type": "text/plain"},
      body: cidr
    });

    if (response.ok) {
      await loadIpRanges();
      document.getElementById("rangeInput").value = "";
    } else {
      alert("대역대 추가 실패");
    }
  } catch (e) {
    console.error("추가 요청 실패:", e);
  }
}

async function resetIpRanges() {
  if (!confirm("정말 모든 IP 대역대를 초기화하시겠습니까?")) {
    return;
  }

  try {
    const response = await fetch("/ipRanges", {method: "DELETE"});
    if (response.ok) {
      await loadIpRanges();
      alert("초기화 완료");
    } else {
      alert("초기화 실패");
    }
  } catch (e) {
    console.error("초기화 요청 실패:", e);
    alert("요청 중 오류 발생");
  }
}

function longToIp(long) {
  return [
    (long >>> 24) & 0xff,
    (long >>> 16) & 0xff,
    (long >>> 8) & 0xff,
    long & 0xff
  ].join('.');
}

async function checkIp() {
  const ip = document.getElementById("checkIpInput").value.trim();
  const resultEl = document.getElementById("checkResult");
  removeExistingMarkers();

  if (!ip || !/^\d{1,3}(\.\d{1,3}){3}$/.test(ip)) {
    alert("유효한 IP 주소를 입력하세요.");
    return;
  }

  try {
    const response = await fetch("/isInclude", {
      method: "POST",
      headers: {"Content-Type": "text/plain"},
      body: ip
    });

    if (!response.ok) {
      resultEl.textContent = "서버 응답 오류";
      resultEl.className = "text-danger text-center";
      return;
    }

    const isIncluded = await response.json(); // true / false
    if (isIncluded) {
      resultEl.textContent = `${ip}는 포함된 IP입니다.`;
      resultEl.className = "text-success text-center";
    } else {
      resultEl.textContent = `${ip}는 포함되지 않은 IP입니다.`;
      resultEl.className = "text-danger text-center";
    }

    // 위치 마커는 항상 표시
    addMarker(ip);
  } catch (e) {
    console.error("IP 포함 여부 확인 실패:", e);
    resultEl.textContent = "요청 중 오류 발생";
    resultEl.className = "text-danger text-center";
  }
}

function addMarker(ip) {
  const ipLong = ipToLong(ip);
  const container = document.getElementById("visualization");
  const totalWidth = container.clientWidth;
  const range = globalRangeEnd - globalRangeStart || 1;
  const leftRatio = (ipLong - globalRangeStart) / range;
  const x = leftRatio * totalWidth;

  const markerContainer = document.createElement("div");
  markerContainer.className = "ip-marker-container";
  markerContainer.style.left = `${x}px`;

  // IP 텍스트 라벨
  const label = document.createElement("div");
  label.className = "ip-marker-label";
  label.textContent = ip;
  markerContainer.appendChild(label);

  // 핀 아이콘
  const icon = document.createElement("div");
  icon.className = "ip-marker-icon";
  markerContainer.appendChild(icon);

  container.appendChild(markerContainer);
}

function ipToLong(ip) {
  const parts = ip.split('.').map(Number);

  // js의 비트 연산자는 32비트까지만 지원하여 음수가 반환 될 수 있음
  let result = (parts[0] << 24) | (parts[1] << 16) | (parts[2] << 8) | parts[3];

  // unsigned로 변환하여 정상적인 숫자로 변환
  return result >>> 0;
}

function removeExistingMarkers() {
  document.querySelectorAll(".ip-marker-container").forEach(el => el.remove());
}

