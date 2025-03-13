package org.potwings.ipchecker.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.net.util.SubnetUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class IPRangeCheckServiceTests {

  @Autowired private IPRangeCheckService ipRangeCheckService;

  @Test
  @DisplayName("대역대 추가 테스트")
  public void addRangeTest() {

    /*
     * 대역대 범위에 대해 확인하려하는데 어떻게 테스트를 진행해야할까?
     * 범위를 추가한 후에 해당 범위가 정상적으로 ipRangeMap에 포함되었는지 확인할 것
     * 허나 ipRangeMap는 service 클래스 내부에서만 범위 확인을 위해서만 쓰이는데
     * 테스트를 위해 외부에 노출시키는 것이 맞을까??
     *
     * 만일 다른 방법으로 정상적으로 추가되었는지 확인한다면 어떻게 확인해야할까??
     * 대역대를 addRange메소드를 통하여 추가한 후 isIncludeIP 메소드를 통하여 확인 진행하자
     *
     *
     * 근데 하나의 테스트 코드에서는 하나의 메소드만 테스트 해야하지 않을까??
     */

    // arrange
    String ipRange = "192.168.1.0/24";
    String ip = "192.168.247";

    // act
    ipRangeCheckService.addRange(ipRange);

    // assert
    //    boolean isInclude = ipRangeCheckService.isIncludeIP(ip); // 포함 여부
    //    assertThat(isInclude).isTrue();
  }

  @Test
  @DisplayName("중복 대역대 추가 테스트")
  public void addDupeRangeTest() {
    /*
     * 중복되는 범위를 가지고 있는 대역대가 이미 등록되어 있는 경우를 위한 테스트
     * 만일 단순 숫자만으로 고려한다면
     * 10~100이 이미 등록되어 있을 때 1~11이 추가된다면
     * 1~100으로 변환하여 등록되어야 한다.
     *
     * 1. 등록될 때 마다 Map을 새로 생성
     * 2. 중복되는 범위를 확인 후 중복되는 범위들만 수정하여 entry 추가
     */
  }

  @Test
  @DisplayName("중복 대역대 추가 테스트")
  public void isIncludeIPTest() {
    /*
     * 대역대에 해당하는 아이피가 추가되었을 경우 정상적으로 테스트 되는지 확인
     *
     */
  }
}
