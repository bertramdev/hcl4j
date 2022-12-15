package com.bertramlabs.plugins.hcl4j;

import java.util.List;

@FunctionalInterface
public interface HCLFunction {
    Object method(List<Object> arguments);
}
