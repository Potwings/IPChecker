package org.potwings.ipchecker.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class IPRangeCheckService {

  // key: 범위 start, value: 범위 end 형식으로 아이피 범위를 저장
  private Map<Long, Long> ipRangeMap = new HashMap<>();

  public boolean checkIP(String ip) {

    // 아이피 입력 시 Map에 있는 Range들을 확인하여 포함되는지 여부를 반환
    return false;
  }

  public void addRange(String ipRange) {

    /*
     * SubnetUtils를 통하여 대역대의 lowIp와 high 아이피를 불러옴
     * 그 후 해당 아이피들을 long으로 변환하여 key: start, value: end로 Map에 저장
     */

  }
}
