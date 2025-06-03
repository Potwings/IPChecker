package org.potwings.ipchecker.controller;

import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.potwings.ipchecker.dto.IpRangeCheckRequest;
import org.potwings.ipchecker.service.IPRangeCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class IPRangeCheckRestController {

  private final IPRangeCheckService service;

  @PostMapping("/ipRanges")
  public ResponseEntity<TreeMap> addIpRange(@RequestBody IpRangeCheckRequest request) {
    TreeMap<Long, Long> ipRangeMap = service.addRange(request.getIpRangeMap(), request.getNewIpRange());

    return ResponseEntity.ok().body(ipRangeMap);
  }

  @PostMapping("isInclude")
  public ResponseEntity<Boolean> isInclude(@RequestBody IpRangeCheckRequest request) {
    boolean result = service.isIncludeIP(request.getIpRangeMap(), request.getCheckingIp());

    return ResponseEntity.ok(result);  // 200 OK + true
  }
}
