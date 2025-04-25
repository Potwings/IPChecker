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

function visualizeRanges(rangeMap) {
  const container = document.getElementById("visualization");
  container.innerHTML = "";

  const keys = Object.keys(rangeMap).map(k => parseInt(k)).sort((a, b) => a - b);
  if (keys.length === 0) return;

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
      headers: { "Content-Type": "text/plain" },
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
  if (!confirm("정말 모든 IP 대역대를 초기화하시겠습니까?")) return;

  try {
    const response = await fetch("/ipRanges", { method: "DELETE" });
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
