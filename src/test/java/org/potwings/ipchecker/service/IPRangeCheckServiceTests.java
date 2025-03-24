package org.potwings.ipchecker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class IPRangeCheckServiceTests {

  @Autowired
  private IPRangeCheckService ipRangeCheckService;

  /**
   * 대역대 범위에 대해 확인하려하는데 어떻게 테스트를 진행해야할까?
   * 범위를 추가한 후에 해당 범위가 정상적으로 ipRangeMap에 포함되었는지 확인할 것 허나
   * ipRangeMap는 service 클래스 내부에서만 범위 확인을 위해서만 쓰이는데 테스트를 위해 외부에 노출시키는 것이 맞을까??
   * <p>
   * 만일 다른 방법으로 정상적으로 추가되었는지 확인한다면 어떻게 확인해야할까??
   * 대역대를 addRange메소드를 통하여 추가한 후 isIncludeIP 메소드를 통하여 확인 진행하자
   * <p>
   * 현재 addRangeTest에서는 정상적으로 실행되는지만 확인 추후 isIncludeIP 테스트에서 정확한 값이 추가되었는지 간접적으로 확인 가능
   */
  @Test
  @DisplayName("대역대 추가 테스트")
  public void addRangeTest() {

    // arrange
    String ipRange = "192.168.1.0/24";

    // act
    boolean result = ipRangeCheckService.addRange(ipRange);

    // assert
    assertThat(result).isTrue();
  }

  /**
   * 대역대에 해당하는 아이피가 추가되어 있을 경우 정상적으로 테스트 되는지 확인
   */
  @Test
  @DisplayName("아이피 포함 여부 확인 테스트")
  public void isIncludeIPTest() {
    // arrange
    String ipRange = "192.168.1.0/24";
    ipRangeCheckService.addRange(ipRange);
    String ip = "192.168.1.247";

    // act
    boolean result = ipRangeCheckService.isIncludeIP(ip);

    // assert
    assertThat(result).isTrue();
  }

  /**
   * 중복되는 범위를 가지고 있는 대역대가 이미 등록되어 있는 경우를 위한 테스트
   * 만일 단순 숫자만으로 이야기 한다면
   * 10~100이 이미 등록되어 있을 때 1~11이 추가된다면
   * 1~100으로 변환하여 등록되어야 한다.
   *
   * 아이피로 생각하면 192.168.1.0/24와 192.168.0.0/16이 등록된다면
   * 192.168.0.0/16의 범위만 나와야 한다.
   *
   * 허나 private 변수라 직접 범위를 확인할 수는 없으므로
   * 192.168.2.247 이라는 아이피의 포함 여부를 조회했을 때 true가 반환되어야한다.
   * 만일 중복되는 범위가 제거되지 않았을 경우
   * isIncludeIP의 floorEntry 메소드에서 192.168.1.0/24 가 반환되어 포함하지 않는다고 나올 것
   */
  @Test
  @DisplayName("추가 범위가 기존 범위보다 더 큰 경우")
  public void testLargerNewRange() {
    // Arrange
    String existingRange = "192.168.1.0/30"; // 192.168.1.0 ~ 192.168.1.3
    String newRange = "192.168.1.0/29"; // 192.168.1.0 ~ 192.168.1.7
    String includedIp1 = "192.168.1.1";
    String includedIp2 = "192.168.1.5";
    String excludedIp = "192.168.1.8";

    // Act
    ipRangeCheckService.addRange(existingRange);
    ipRangeCheckService.addRange(newRange);

    // Assert
    assertTrue(ipRangeCheckService.isIncludeIP(includedIp1));
    assertTrue(ipRangeCheckService.isIncludeIP(includedIp2));
    assertFalse(ipRangeCheckService.isIncludeIP(excludedIp));
  }

  @Test
  @DisplayName("기존 범위의 시작점 보다 추가 범위의 시작점이 더 큰 경우")
  public void testExistingRangeIsLarger() {
    // Arrange
    String existingRange = "192.168.1.12/30"; // 192.168.1.12 ~ 192.168.1.15
    String newRange = "192.168.1.8/29"; // 192.168.1.8 ~ 192.168.1.15
    String includedIp1 = "192.168.1.8";
    String includedIp2 = "192.168.1.10";
    String excludedIp = "192.168.1.16";

    // Act
    ipRangeCheckService.addRange(existingRange);
    ipRangeCheckService.addRange(newRange);

    /*
     * 현재 병합이 안되어 Map에 값이 두개나 있음에도 불구하고 테스트가 통과되고 있음
     */
    // Assert
    assertTrue(ipRangeCheckService.isIncludeIP(includedIp1));
    assertTrue(ipRangeCheckService.isIncludeIP(includedIp2));
    assertFalse(ipRangeCheckService.isIncludeIP(excludedIp));
  }

  @Test
  @DisplayName("중복 범위 병합 여부 테스트(신규 범위가 더 큰 경우)")
  public void testIpIncludedMergedLargerNew () {
    // Arrange
    String existingRange = "192.168.1.8/30"; // 192.168.1.8 ~ 192.168.1.11
    String newRange = "192.168.1.0/27"; // 192.168.1.0 ~ 192.168.1.31
    String includedIp = "192.168.1.15";

    // Act
    ipRangeCheckService.addRange(existingRange);
    ipRangeCheckService.addRange(newRange);

    // Assert
    // 정상적으로 병합되었다면 isIncludeIP 진행 시 floorEntry에서 newRange가 반환되어 포함으로 판정되어야함.
    assertTrue(ipRangeCheckService.isIncludeIP(includedIp));
  }
}
