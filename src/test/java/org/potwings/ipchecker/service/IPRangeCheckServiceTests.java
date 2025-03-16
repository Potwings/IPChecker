package org.potwings.ipchecker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.commons.net.util.SubnetUtils;
import org.junit.jupiter.api.Assertions;
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
    // Arrange (입력 데이터 및 예상 결과)
    String smallRange = "192.168.1.10/30"; // 192.168.1.10 ~ 192.168.1.13
    String largerRange = "192.168.1.8/29"; // 192.168.1.8 ~ 192.168.1.15
    String includedIp1 = "192.168.1.8";
    String includedIp2 = "192.168.1.15";
    String excludedIp = "192.168.1.16";

    // Act (실행)
    ipRangeCheckService.addRange(smallRange);
    ipRangeCheckService.addRange(largerRange);

    // Assert (검증)
    assertTrue(ipRangeCheckService.isIncludeIP(includedIp1));
    assertTrue(ipRangeCheckService.isIncludeIP(includedIp2));
    assertFalse(ipRangeCheckService.isIncludeIP(excludedIp));
  }

  @Test
  @DisplayName("기존 범위가 추가 범위보다 더 큰 경우")
  public void testExistingRangeIsLarger() {
    // Arrange
    String largerRange = "192.168.1.8/29"; // 192.168.1.8 ~ 192.168.1.15
    String smallRange = "192.168.1.10/30"; // 192.168.1.10 ~ 192.168.1.13
    String includedIp1 = "192.168.1.8";
    String includedIp2 = "192.168.1.15";
    String excludedIp = "192.168.1.16";

    // Act
    ipRangeCheckService.addRange(largerRange);
    ipRangeCheckService.addRange(smallRange);

    // Assert
    assertTrue(ipRangeCheckService.isIncludeIP(includedIp1));
    assertTrue(ipRangeCheckService.isIncludeIP(includedIp2));
    assertFalse(ipRangeCheckService.isIncludeIP(excludedIp));
  }

  @Test
  @DisplayName("추가하는 범위가 기존 범위와 시작점이 중복되는 경우")
  public void testDupeStartPoint() {
    // Arrange
    String existingRange = "192.168.1.8/29";  // 192.168.1.8 ~ 192.168.1.15
    String newRange = "192.168.1.15/30"; // 192.168.1.15 ~ 192.168.1.18 (시작점 중복)
    String includedIp1 = "192.168.1.8";
    String includedIp2 = "192.168.1.18";
    String excludedIp = "192.168.1.19";

    // Act
    ipRangeCheckService.addRange(existingRange);
    ipRangeCheckService.addRange(newRange);

    // Assert
    assertTrue(ipRangeCheckService.isIncludeIP(includedIp1));
    assertTrue(ipRangeCheckService.isIncludeIP(includedIp2));
    assertFalse(ipRangeCheckService.isIncludeIP(excludedIp));
  }

  @Test
  @DisplayName("추가하는 범위가 기존 범위와 종료점이 중복되는 경우")
  public void testDupeEndPoint() {
    // Arrange
    String existingRange = "192.168.1.8/30";  // 192.168.1.8 ~ 192.168.1.11
    String newRange = "192.168.1.4/29";  // 192.168.1.4 ~ 192.168.1.11 (종료점 중복)
    String includedIp1 = "192.168.1.4";
    String includedIp2 = "192.168.1.11";
    String excludedIp = "192.168.1.3";

    // Act
    ipRangeCheckService.addRange(existingRange);
    ipRangeCheckService.addRange(newRange);

    // Assert
    assertTrue(ipRangeCheckService.isIncludeIP(includedIp1));
    assertTrue(ipRangeCheckService.isIncludeIP(includedIp2));
    assertFalse(ipRangeCheckService.isIncludeIP(excludedIp));
  }

  /**
   * IPv6의 경우 Long 값으로 변환이 불가능하므로 다른 방법을 고안해야한다.
   */
  @Test
  @DisplayName("IPv6 테스트")
  public void isIncludeIPv6Test() {
    // arrange
    String ip = "fe80::db6c:a3ec:6c94:5f70%10";

    // act

    // assert
  }
}
