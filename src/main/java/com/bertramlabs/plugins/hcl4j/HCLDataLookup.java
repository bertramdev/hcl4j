package com.bertramlabs.plugins.hcl4j;
import java.util.Map;
@FunctionalInterface
public interface HCLDataLookup {
		Map<String,Object> method(Map<String,Object> arguments);
}
