package org.potwings.ipchecker.dto;

import java.util.TreeMap;
import lombok.Data;

@Data
public class IpRangeCheckRequest {

  private TreeMap<Long, Long> ipRangeMap;

  private String newIpRange;

  private String checkingIp;
}
