package org.potwings.ipchecker.service;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
// 각각의 테스트 메소드가 클래스 변수를 공유하지 않도록 DirtiesContext 추가
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Slf4j
public class IPRangeCheckServiceTests {

  @Autowired
  private IPRangeCheckService ipRangeCheckService;

  @Test
  @DisplayName("대역대 추가 테스트")
  public void testAddRange() {

    // arrange
    String ipRange = "192.168.1.0/24";

    // act
    boolean result = ipRangeCheckService.addRange(ipRange);

    // assert
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("아이피 포함 여부 확인 테스트")
  public void testIsIncludeIP() {
    // arrange
    String ipRange = "192.168.1.0/24";
    ipRangeCheckService.addRange(ipRange);
    String ip = "192.168.1.247";

    // act
    boolean result = ipRangeCheckService.isIncludeIP(ip);

    // assert
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("두 대역대 시작 지점이 동일한 경우 병합 테스트 (기존 < 추가)")
  public void testSameStartLargerNewMerge() {
    // arrange
    String existRange = "192.168.2.16/31"; // 192.168.2.16 ~ 192.168.2.17
    String newRange = "192.168.2.16/29"; // 192.168.2.16 ~ 192.168.2.23
    String testIp = "192.168.2.22"; // 기존 포함 X 추가 후 포함 O
    ipRangeCheckService.addRange(existRange);
    boolean beforeAddResult = ipRangeCheckService.isIncludeIP(testIp);

    // act
    ipRangeCheckService.addRange(newRange);
    boolean afterAddResult = ipRangeCheckService.isIncludeIP(testIp);

    //assert
    assertThat(beforeAddResult).isFalse();
    // 정상적으로 병합되었다면 추가 대역대의 범위를 기준으로 판단
    assertThat(afterAddResult).isTrue();
  }

  @Test
  @DisplayName("두 대역대 시작 지점이 동일한 경우 병합 테스트 (기존 > 추가)")
  public void testSameStartLargerExistMerge() {
    // arrange
    String existRange = "192.168.1.0/24"; // 192.168.1.0 ~ 192.168.1.255
    String newRange = "192.168.1.0/25"; // 192.168.1.0 ~ 192.168.1.127
    String testIp = "192.168.1.250"; // 기존 포함 O 추가 후 포함 O
    ipRangeCheckService.addRange(existRange);
    boolean beforeAddResult = ipRangeCheckService.isIncludeIP(testIp);

    // act
    ipRangeCheckService.addRange(newRange);
    boolean afterAddResult = ipRangeCheckService.isIncludeIP(testIp);

    //assert
    assertThat(beforeAddResult).isTrue();
    // 정상적으로 병합되었다면 추가 대역대가 아닌 기존 대역대의 범위를 기준으로 판단
    assertThat(afterAddResult).isTrue();
  }

  @Test
  @DisplayName("두 대역대 끝 지점이 동일한 경우 병합 테스트 (기존 < 추가)")
  public void testSameEndLargerNewMerge() {
    // arrange
    String existRange = "192.6.0.0/15"; // 192.6.0.0 ~ 192.7.255.255
    String newRange = "192.0.0.0/13"; // 192.0.0.0 ~ 192.7.255.255
    String testIp = "192.5.1.5"; // 기존 포함 X 추가 후 포함 O
    ipRangeCheckService.addRange(existRange);
    boolean beforeAddResult = ipRangeCheckService.isIncludeIP(testIp);

    // act
    ipRangeCheckService.addRange(newRange);
    boolean afterAddResult = ipRangeCheckService.isIncludeIP(testIp);

    //assert
    assertThat(beforeAddResult).isFalse();
    // 정상적으로 병합되었다면 추가 대역대의 범위를 기준으로 판단
    assertThat(afterAddResult).isTrue();
  }

  @Test
  @DisplayName("두 대역대 끝 지점이 동일한 경우 병합 테스트 (기존 > 추가)")
  public void testSameEndLargerExistMerge() {
    // arrange
    String existRange = "192.168.0.0/23"; // 192.168.0.0 ~ 192.168.1.255
    String newRange = "192.168.1.240/28"; // 192.168.1.240 ~ 192.168.1.255
    String testIp = "192.168.1.247"; // 기존 포함 O 추가 후 포함 O
    ipRangeCheckService.addRange(existRange);
    boolean beforeAddResult = ipRangeCheckService.isIncludeIP(testIp);

    // act
    ipRangeCheckService.addRange(newRange);
    boolean afterAddResult = ipRangeCheckService.isIncludeIP(testIp);

    //assert
    // 둘 다 기존 대역대를 기준으로 판단
    assertThat(beforeAddResult).isTrue();
    assertThat(afterAddResult).isTrue();
  }

  @Test
  @DisplayName("기존 대역대가 추가 대역대를 포함하는 경우 병합 테스트")
  public void testExistIncludeNewMerge() {
    // arrange
    String existRange = "192.0.0.0/9"; // 192.0.0.0 ~ 192.127.255.255
    String newRange = "192.2.0.0/15"; // 192.2.0.0 ~ 192.3.255.255
    String testIp = "192.10.1.247"; // 기존 포함 O 추가 후 포함 O
    ipRangeCheckService.addRange(existRange);
    boolean beforeAddResult = ipRangeCheckService.isIncludeIP(testIp);

    // act
    ipRangeCheckService.addRange(newRange);
    boolean afterAddResult = ipRangeCheckService.isIncludeIP(testIp);

    //assert
    // 둘 다 기존 대역대를 기준으로 판단
    assertThat(beforeAddResult).isTrue();
    assertThat(afterAddResult).isTrue();

  }

  @Test
  @DisplayName("추가 대역대가 기존 대역대를 포함하는 경우 병합 테스트")
  public void testNewIncludeExistMerge() {
    // arrange
    String existRange = "200.0.0.0/6"; // 200.0.0.0 ~ 203.255.255.255
    String newRange = "192.0.0.0/4"; // 192.0.0.0 ~ 207.255.255.255
    String testIp = "192.168.1.247"; // 기존 포함 X 추가 후 포함 O
    ipRangeCheckService.addRange(existRange);
    boolean beforeAddResult = ipRangeCheckService.isIncludeIP(testIp);

    // act
    ipRangeCheckService.addRange(newRange);
    boolean afterAddResult = ipRangeCheckService.isIncludeIP(testIp);

    //assert
    assertThat(beforeAddResult).isFalse();
    // 정상적으로 병합되었을 경우 추가 대역대를 기준으로 판단
    assertThat(afterAddResult).isTrue();
  }
}
