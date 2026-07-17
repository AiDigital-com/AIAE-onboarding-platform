// Test-only helper service impl, also placed in a package matching the
// auto-log pointcut. Used by UsageLoggingAspectTest to prove that a nested
// *ServiceImpl call triggered from within an already-logged call does not
// emit its own usage event (see UsageLoggingAspect invariant 9).
package com.aidigital.aionboarding.service.demo.services.impl;

public class DemoHelperServiceImpl {

	public String helperMethod() {
		return "helped";
	}
}
