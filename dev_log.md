# 개발 동기
기존 회사에서 아이피가 범위에 포함되는지 여부를 apache-james의 NetMatcher을 통해 확인하고 있었다.</br>
(https://github.com/apache/james-project/blob/master/server/dns-service/dnsservice-library/src/main/java/org/apache/james/dnsservice/library/netmatcher/NetMatcher.java)

해당 클래스에서는 아이피 범위(Subnet Mask)들을 SortedSet(TreeSet)에 저장하고 있었다.</br>
<img src="img/img.png" style="width: 400px; height: auto;"/>

<img src="img/img_1.png" style="width: 600px; height: auto;"/>

허나 아이피는 그저 작은 숫자가 먼저 오도록 정렬되고 있었고 포함 여부를 확인하는 메소드에서도 별 다른 활용을 하지 않고 있었다.

<img src="img/img_2.png" style="width: 600px; height: auto;"/>

따라서 단순히 HashSet으로 변경하여 성능을 좀 더 올려볼까라고 생각하였으나 <b>'정렬을 활용하면 더 좋은 성능을 끌어낼 수 있지 않을까'</b> 라는 생각이 들어 새로운 방법을 찾기 시작하였다.

# 기능 설계
우선 '라이브러리에서 TreeSet을 사용하는 이유가 있지 않을까?'해서 이를 활용할 수 있는 방법에 대해 GPT에게 물어보았다.</br>
GPT가 제시한 방법은 String으로 정렬된 TreeMap의 대역대들 중 아이피보다 작은 대역대들만 탐색하는 것이었다.

<details>
<summary>GPT가 작성했던 코드</summary>

```
    public boolean isIpInSubnet(String ip) {
        Inet4Address inetAddress = (Inet4Address) InetAddresses.forString(ip);
        InetNetwork ipSubnet = InetNetwork.fromCidr(ip + "/32");

        // headSet(ipSubnet, true)로 ip보다 작은 서브넷들을 가져오기
        NavigableSet<InetNetwork> possibleSubnets = subnets.headSet(ipSubnet, true);

        // for 문을 활용하여 서브넷이 포함하는지 확인 (즉시 반환)
        for (InetNetwork subnet : possibleSubnets) {
            if (subnet.contains(inetAddress)) {
                return true; // 서브넷 내 포함되면 즉시 true 반환
            }
        }

        return false; // 포함된 서브넷이 없으면 false 반환
    }
```
</details>

이렇게 되었을 경우에 확인해야하는 대역대 범위를 줄일 수 있어 탐색 속도를 더 빠르게 할 수 있으나</br>
'192.168.1.247/8'와 같은 대역대가 들어있을 경우 192.168.1.10과 같은 아이피를 정상적으로 탐지하지못한다.</br>
<b>(글을 작성하면서 알게된 건데 이 표현은 서브넷 마스크 문법에 맞지 않는 표현이었다.)</b>

그리하여 나는 새로운 프롬프트로 질문을 다시 시작하였다.

<img src="img/img_4.png" style="width: 600px; height: auto;"/>

이때 GPT는 아이피 포함 여부를 확인해주는 코드를 작성해주었는데 그 중 <b>ipToInt라는 IP를 int로 변환해주는 메소드가 눈에 띄였다.</b>

<img src="img/img_5.png" style="width: 600px; height: auto;"/>

<details>
<summary>GPT가 작성한 전체 코드</summary>

```
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class IPSubnetChecker {

    // CIDR 범위를 나타내는 클래스
    static class Subnet {
        private final int subnetAddress;
        private final int subnetMask;

        public Subnet(String cidr) throws UnknownHostException {
            String[] parts = cidr.split("/");
            String ip = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            this.subnetAddress = ipToInt(ip);
            this.subnetMask = (0xFFFFFFFF << (32 - prefixLength));
        }

        public boolean contains(String ip) throws UnknownHostException {
            int ipInt = ipToInt(ip);
            return (ipInt & subnetMask) == (subnetAddress & subnetMask);
        }

        private static int ipToInt(String ip) throws UnknownHostException {
            byte[] bytes = InetAddress.getByName(ip).getAddress();
            int result = 0;
            for (byte b : bytes) {
                result = (result << 8) | (b & 0xFF);
            }
            return result;
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        List<Subnet> subnets = new ArrayList<>();
        subnets.add(new Subnet("192.168.1.0/24"));
        subnets.add(new Subnet("10.0.0.0/8"));

        String testIp = "192.168.1.100";

        for (Subnet subnet : subnets) {
            if (subnet.contains(testIp)) {
                System.out.println(testIp + " is in " + subnet);
            }
        }
    }
}
```
</details>

이를 보고 <b>'만일 대역대를 숫자 범위로 변환 후 중복되는 범위를 병합한다면 String으로 정렬하였을 때의 문제점을 해결할 수 있지 않을까?'</b> 라는 생각이 들어 개발을 시작하게 되었다.

# 개발 과정
## TDD(Test Driven Development)
TDD로 진행하여 기본적인 기능인 "대역대 추가", "아이피 포함 여부 확인" 테스트부터 작성하여 진행하였다.

여기서 바로 문제가 발생하였는데, 등록된 아이피 대역대 정보는 클래스 내부에서 포함 여부를 확인하기 위해서만 사용되는 데이터이므로 외부에 노출될 필요가 없는 데이터라 생각하여 private으로 되어 있었다.

허나 이로 인해 테스트 메소드에서 아이피 대역대 추가 후 확인하는 것이 불가능하여 어떻게 해결 할 지 방안에 대해 고민하던 중</br>
<b>만일 정상적으로 대역대가 추가되었다면 해당 대역대에 포함되는 아이피가 포함 여부 확인 메소드에서 true가 반환</b> 되어야 한다 생각하였다.

따라서 대역대 추가 테스트는 정상적으로 실행되는지만 확인 후 실제 Map에 정상적으로 추가되었는지 여부는 isInclude 메소드를 통해 확인하고자 아래와 같이 테스트 코드를 작성하였다.

<details>
<summary>작성한 테스트 코드</summary>

```
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
   * 대역대가 추가되어 있을 경우 대역대에 포함되는 아이피의 포함 여부 정상적으로 반환되는지 확인
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
```

</details>

테스트 코드를 통해 기본적인 기능을 만든 후 이제 중복되는 대역대에 대한 처리가 필요하였다.

## 중복되는 대역대 병합

서브넷 마스크로 분리된 아이피 대역대들은 하나의 큰 범위를 비트수에 따라 쪼개 놓은 것이다. 모든 대역대는 특정 지점에서 시작하고 특정 지점에서 끝나게 된다.

<img src="https://github.com/user-attachments/assets/b76851c0-18ac-42b3-abbd-2d014d2c03b8" style="width: 900px; height: auto;"/>

이러한 서브넷 마스크의 특성으로 인해 아래 그림처럼 일부만 겹치는 경우는 발생할 수 없다.

<img src="https://github.com/user-attachments/assets/a9bd0c5e-cba6-49f6-bceb-206826f8ef3d" style="width: 900px; height: auto;"/>

따라서 대역대 추가 시 중복되는 범위 병합 여부 확인을 위해 테스트해야할 케이스는 아래와 같다.

1. 두 대역대 시작 지점이 동일한 경우
- (기존 < 추가)</br>
  <img src="https://github.com/user-attachments/assets/303b2c92-0a67-4e44-8c03-c1fdea862186" style="width: 900px; height: auto;"/>

- (기존 > 추가)</br>
  <img src="https://github.com/user-attachments/assets/dc39dd01-4c80-44a4-9c10-2615b6ab9b3c" style="width: 900px; height: auto;"/>

2. 두 대역대 끝 지점이 동일한 경우
- (기존 < 추가)</br>
  <img src="https://github.com/user-attachments/assets/68bf9d7d-bef4-4a35-9ab6-7398a6e02684" style="width: 900px; height: auto;"/>

- (기존 > 추가)</br>
  <img src="https://github.com/user-attachments/assets/efba76cd-b18c-41ef-8dbd-549b917727e5" style="width: 900px; height: auto;"/>

3. 기존 대역대가 추가 대역대를 포함하는 경우</br>
   <img src="https://github.com/user-attachments/assets/cf4776e1-4682-4b2c-9e52-4e1837dd5417" style="width: 900px; height: auto;"/>

4. 추가 대역대가 기존 대역대를 포함하는 경우</br>
   <img src="https://github.com/user-attachments/assets/d4ec461d-55de-4177-8fcd-dfe39f725df6" style="width: 900px; height: auto;"/>

총 6개의 테스트 케이스를 작성하였고, 이 케이스들을 모두 통과할 수 있도록 아이피 대역대 추가 기능을 구현하였다.

## 화면 추가
대역대를 시각화한다면, 추후 왜 해당 아이피가 포함되어있는 것으로 판단되었는지 더 쉽게 확인할 수 있겠다는 생각이 들어 프론트엔드 구성을 시작하게 되었다.

대역대의 경우 이미 서버에서 Long값으로 가지고 있으니 이를 프론트에서 불러와 다시 IP로 변환하면 좋겠다는 생각이 들었고</br>
해당 내용을 바탕으로 어떻게 구성하면 좋을지 GPT에게 물어보았다.

<img src="https://github.com/user-attachments/assets/71a75b7b-06e1-43cd-af22-6066fcd1359b" style="width: 300px; height: auto;"/></br>
<img src="https://github.com/user-attachments/assets/ba9007fb-8003-4e6e-a5f8-8771cc7055bc" style="width: 300px; height: auto;"/>

GPT가 어느정도 기본은 작성해주었으나 디테일이 많이 부족하였고, 정상적으로 동작하지 않는 부분도 많았다.</br>
다행이 프론트에 대한 기본적인 이해는 있어 충분히 해결할 수 있었고, 아래와 같이 기본적인 화면을 구성할 수 있었다.

<img src="https://github.com/user-attachments/assets/3ce7ace4-941e-42ff-bd34-073521b2ee49" style="width: 900px; height: auto;"/>

## 누락된 테스트 케이스 발생
화면을 구성한 후 이런 저런 데이터를 넣어보다, 초기에 테스트 케이스 고려 시 놓친 부분이 있다는 것을 깨닫게 되었다.

두 대역대를 등록하고 앞쪽의 대역대에 이미 포함되는 대역대를 추가할 떄 발생하는 문제였는데, 아래와 같이 테스트하다 알게 되었다.

1. "192.168.1.0/24"와 "192.168.5.0/24"를 등록</br>
   ![image](https://github.com/user-attachments/assets/eb0c773a-1186-4aa4-b905-2ae7ac7a9405)

2. "192.168.1.0/24"에 포함되는 "192.168.1.247/31" 추가</br>
   ![image](https://github.com/user-attachments/assets/14982c4b-191c-4840-8f89-c8485331d207)
   (192.168.1.0/24에 이미 포함되어 있으므로 시각화된 대역대는 변화가 없어야하는데 비정상적으로 병합되는 모습)

이를 해결하기 위해 해당 케이스를 테스트 코드로 작성해주었고, 서비스 코드를 확인해보았는데 여기에 문제점이 있었다.

<img src="https://github.com/user-attachments/assets/68da967a-da57-4c73-a036-71fef97ebcec" style="width: 600px; height: auto;"/>

추가되는 대역대와 뒤쪽 대역대의 병합 가능 여부를 판단할 때는, 뒤쪽 대역대의 시작점(higherEntry의 key)과 추가되는 대역대의 종료점(rangeEnd)을 비교해야 하는데,
현재는 잘못된 방식으로 뒤쪽 대역대의 종료점(higherEntry의 value)과 추가되는 대역대의 시작점(rangeStart)을 비교하고 있어 문제가 발생하고 있었다.

정상적으로 비교하도록 아래와 같이 수정하니 테스트는 통과되었다.

```
    // 앞쪽 대역대와의 병합
    if (lowerEntry != null && lowerEntry.getValue() >= rangeStart - 1) {
      newStart = Math.min(lowerEntry.getKey(), newStart);
      newEnd = Math.max(lowerEntry.getValue(), newEnd);
      ipRangeMap.remove(lowerEntry.getKey());
    }

    // 뒤쪽 대역대와의 병합
    if (higherEntry != null && higherEntry.getKey() <= rangeEnd + 1) {
      newStart = Math.min(higherEntry.getKey(), newStart);
      newEnd = Math.max(higherEntry.getValue(), newEnd);
      ipRangeMap.remove(higherEntry.getKey());
    }
```

추가로 지금 서비스 로직을 보면 우리는 대역대 추가 로직에서 앞의 대역대와의 병합, 뒤의 대역대와의 병합 총 두번의 병합이 이루어진다.

이를 고려하여 두번의 병합이 모두 정상적으로 동작할 수 있는지 여부는 테스트할 필요가 있다는 생각이 들어 아래 케이스를 추가하였고 정상적으로 통과되는 것을 확인했다.

- 기존 두개의 대역대 사이에 대역대가 추가되는 경우(병합이 앞뒤 모두 이루어지는 경우)
  <img src="https://github.com/user-attachments/assets/cb3b7f5e-4c20-494c-ae53-d62eaff1eed8" style="width: 900px; height: auto;"/>

## 아이피 포함 여부 확인 프론트엔드 개발 
이제 화면에서 아이피 포함 여부를 확인할 수 있도록 기능을 추가해보자

GPT에게 요청을 하여 아이피 대역대를 판단할 수 있는 기능을 요청하여 판단 여부는 쉽게 추가할 수 있었다.

<img src="img/is_contains.png" style="width: 600px; height: auto;"/>

허나 사용자가 좀 더 쉽게 알아볼 수 있도록 하기 위해 포함 여부 확인할 아이피의 위치도 시각화하고 싶어 요청했으나 정상적으로 처리되지 않았다.

디버깅을 통해 확인해보니 아이피를 정수로 변환하는 과정에서 정상적으로 처리되고 있지 않았다.

<img src="img/ip_parse_wrong.png" style="width: 600px; height: auto;"/>

이의 원인은 ip를 Long으로 변환하는 과정에서 비트 연산자를 사용하여 발생하는 문제였는데</br>
<b>js의 number는 9,007,199,254,740,991(2^53 – 1)까지 지원하지만 js의 비트 연산자는 2,147,483,647(32비트)까지 밖에 지원하지 않았다.</b>

이로 인하여 비트 연산 과정에서 오버플로우가 발생하여 변환한 값이 정상적이지 않았고</br>
아래와 같이 unsinged 정수로 변환하는 과정을 통해 정상적으로 처리할 수 있었다.

```
function ipToLong(ip) {
  const parts = ip.split('.').map(Number);

  // js의 비트 연산자는 32비트까지만 지원하여 음수가 반환 될 수 있음
  let result = (parts[0] << 24) | (parts[1] << 16) | (parts[2] << 8) | parts[3];

  // unsigned로 변환하여 정상적인 숫자로 변환
  return result >>> 0;
}
```

이렇게 처리하니 정상적으로 확인하려는 아이피의 위치에 마커를 추가할 수 있었다.

<img src="img/show_maker.png" style="width: 600px; height: auto;"/>

## 웹 서비스를 위한 사용자 별 대역대 관리
현재는 서버에 대역대 데이터를 보관하고 있는 상태이다. 허나 만일 서비스로 운영하게 된다면 동시에 여러 사용자가 사용하게 되는 경우가 발생한다.

이를 해결하기 위해선 대역대를 각 사용자 별로 관리해야 하는데, 브라우저에서 해당 사용자의 대역대 데이터를 보관하는 것이 좋겠다 생각했다.

자바 스크립트 변수, sessionStorage, localStorage 중 어떤 것을 사용할지에 대해 고민하였는데 이들의 특징은 아래와 같았다.

| 항목                     | 자바 스크립트 변수   | sessionStorage         | localStorage           |
|--------------------------|--------------|------------------------|-------------------------|
| 데이터 유지 시간         | 페이지 로드 중만 유지 | 탭이 닫힐 때까지        | 명시적으로 삭제 전까지  |
| 새로고침 시 데이터        | 삭제됨          | 유지됨                 | 유지됨                  |
| 브라우저 종료 시 데이터  | 삭제됨          | 삭제됨                 | 유지됨                  |


사용자가 확인하고 싶은 대역대의 정보는 일시적인 것이라, 다시 접속하였을 때는 다른 대역대를 확인하고 싶을 것이라 생각하여 자바 스크립트 변수나 sessionStorage를 사용하려 했었다.

허나 한명의 사용자로 생각해보았을 때 나도 브라우저를 사용하다 실수로 종료하는 경우가 있었고, 만일 이 경우 여태까지 등록한 데이터가 초기화 된다면 불편할 것이라 생각하였다.</br>
추가한 대역대를 초기화하는 기능이 있기 때문에 기존 데이터를 유지하더라도 사용자가 크게 불편하진 않을것이라 생각하여 <b>localStorage를 사용하기로 결정하였다.</b>


TO-BE에서는 요청을 받을 때 브라우저에서 전체 대역대 목록도 받아와야 하므로 대역대 추가 메소드와 포함 여부 확인 메소드에 파라미터를 새로 추가해야했다.
<img src="img/add_param.png" style="width: 600px; height: auto;"/>

위와 같은 변경사항으로 인해 <b> TDD로 진행하여 작성해두었던 테스트 코드들을 모두 변경해주어야해 번거로움이 발생하였다.</b>

또 변경된 비지니스 로직에서 마음에 들지 않는 부분들이 발생하였는데....
