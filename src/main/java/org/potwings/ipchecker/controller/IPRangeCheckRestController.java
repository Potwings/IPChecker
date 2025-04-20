package org.potwings.ipchecker.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.potwings.ipchecker.service.IPRangeCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class IPRangeCheckRestController {

  private final IPRangeCheckService service;


  @GetMapping("/ipRanges")
  public Map<Long, Long> getIpRange() {
    return service.getIpRangeMap();
  }

  @PostMapping("/ipRanges")
  public ResponseEntity<Void> addIpRange(@RequestBody String ipRange) {
    if (service.addRange(ipRange)) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.badRequest().build();
    }
  }

  @DeleteMapping("ipRanges")
  public ResponseEntity<Void> clearIpRange() {
    service.clearIpRangeMap();
    return ResponseEntity.ok().build();
  }
}
