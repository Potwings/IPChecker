package org.potwings.ipchecker.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.TreeMap;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class IPRangeCheckService {

  /**
   * 아이피 입력 시 Map에 있는 Range들을 확인하여 포함되는지 여부를 반환하는 메소드
   *
   * @param ipRangeMap 사용자가 추가한 아이피 대역대 전체
   * @param ip 포함 여부 확인할 아이피
   * @return 아이피 포함 여부
   */
  public boolean isIncludeIP(TreeMap<Long,Long> ipRangeMap, String ip) {

    Long ipLong = ipToLong(ip);
    if (ipLong == null) {
      return false;
    }

    Map.Entry<Long, Long> entry = ipRangeMap.floorEntry(ipLong);
    return entry != null && ipLong >= entry.getKey() && ipLong <= entry.getValue();
  }

  /**
   * SubnetUtils를 통하여 대역대의 lowIp와 high 아이피를 불러옴
   * 그 후 해당 아이피들을 long으로 변환하여 key: start, value: end로 Map에 저장
   *
   * 새로운 값이 추가될 경우 Map의 데이터를 어떻게 중복제거 할 것인가?
   * 중복되는 범위를 확인 후 중복되는 entry 제거 후 추가
   *
   * @param ipRangeMap 사용자가 추가한 아이피 대역대 전체
   * @param ipRange 범위에 포함할 신규 대역대
   */
  public TreeMap<Long, Long> addRange(TreeMap<Long,Long> ipRangeMap, String ipRange) {
    SubnetUtils subnetUtils = new SubnetUtils(ipRange);
    // 네트워크 주소, 브로드 캐스트 주소도 포함하여 검사
    subnetUtils.setInclusiveHostCount(true);
    SubnetInfo info = subnetUtils.getInfo();
    String lowAddress = info.getLowAddress();
    String highAddress = info.getHighAddress();
    Long rangeStart = ipToLong(lowAddress);
    Long rangeEnd = ipToLong(highAddress);
    if (rangeStart == null || rangeEnd == null) {
      return null;
    }
    Map.Entry<Long, Long> lowerEntry = ipRangeMap.floorEntry(rangeStart);
    Map.Entry<Long, Long> higherEntry = ipRangeMap.ceilingEntry(rangeStart);

    long newStart = rangeStart;
    long newEnd = rangeEnd;

    /*
     * 앞쪽 대역대와 병합 가능 여부 확인
     * 현재 대역대의 시작점보다 앞쪽 대역대의 종료점이 더 클 경우 둘 병합
     */
    if (lowerEntry != null && lowerEntry.getValue() >= rangeStart - 1) {
      newStart = Math.min(lowerEntry.getKey(), newStart);
      newEnd = Math.max(lowerEntry.getValue(), newEnd);
      ipRangeMap.remove(lowerEntry.getKey());
    }

    /*
     * 뒤쪽 대역대와 병합 가능 여부 확인
     * 현재 대역대의 종료점보다 뒤쪽 대역대의 시작점이 더 작을 경우 둘 병합
     */
    if (higherEntry != null && higherEntry.getKey() <= rangeEnd + 1) {
      newStart = Math.min(higherEntry.getKey(), newStart);
      newEnd = Math.max(higherEntry.getValue(), newEnd);
      ipRangeMap.remove(higherEntry.getKey());
    }

    ipRangeMap.put(newStart, newEnd);

    return ipRangeMap;
  }

  /**
   * IP를 숫자(Long)으로 변환해주는 메소드 클래스 내부에서만 쓰이므로 private로 선언 테스트는 어떻게 진행할까??? -> 모든 메소드의 테스트 코드를 만들 필요는
   * 없다. private는 public 메소드를 테스트하면서 간접적으로 테스트 될 것
   *
   * @param ip 숫자로 변환할 아이피
   * @return 숫자로 변환한 아이피 값
   */
  private Long ipToLong(String ip) {
    byte[] bytes = null;
    try {
      bytes = InetAddress.getByName(ip).getAddress(); // IP를 바이트 배열로 변환
    } catch (UnknownHostException e) {
      log.warn("IP Parse Fail : ip: {}", e.getMessage(), e);
    }
    long result = 0;
    for (byte b : bytes) {
      result = (result << 8) | (b & 0xFF); // 왼쪽 시프트 후 OR 연산
    }
    return result;
  }
}
