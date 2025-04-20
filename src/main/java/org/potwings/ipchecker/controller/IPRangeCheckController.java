package org.potwings.ipchecker.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class IPRangeCheckController {

  // 화면 구성용
  @GetMapping("/ipchecker")
  public void ipchecker() {
  }
}
