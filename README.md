# IPChecker
아이피가 특정 범위에 포함되는지 빠르게 판단하기 위한 모듈

## 개발 동기
기존 회사에서 아이피가 범위에 포함되는지 여부를 apache-james의 NetMatcher을 통해 확인하고 있었다.
(https://github.com/apache/james-project/blob/master/server/dns-service/dnsservice-library/src/main/java/org/apache/james/dnsservice/library/netmatcher/NetMatcher.java)

해당 클래스에서는 아이피 범위(Subnet Mask)들을 SortedSet(TreeSet)에 저장하고 있었다.
![img.png](img.png)
![img_1.png](img_1.png)

허나 아이피는 그저 작은 숫자가 먼저 오도록 정렬되고 있었고 포함 여부를 확인하는 메소드에서도 별 다른 활용을 하지 않고 있었다.
![img_2.png](img_2.png)

따라서 그냥 HashSet으로 변경하여 성능을 좀 더 올려볼까라고 생각하였으나 정렬을 활용하면 더 좋은 성능을 끌어낼 수 있지 않을까 라는 생각이 들어 새로운 방법을 찾기 시작하였다.

## 개발 과정
### GPT와의 협업

