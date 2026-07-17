// Test-only service impl placed in a package that matches the auto-log
// pointcut (*..service..services.impl.*ServiceImpl). Used by
// UsageLoggingAspectTest to prove the aspect fires with NO annotation — the
// behaviour that previously silently broke when methods were left un-annotated.
package com.aidigital.aionboarding.service.demo.services.impl;

import com.aidigital.aionboarding.usagelogging.LogUsage;

public class DemoServiceImpl {

	private final DemoHelperServiceImpl helper;

	public DemoServiceImpl(DemoHelperServiceImpl helper) {
		this.helper = helper;
	}

	public String doThing() {
		return "ok";
	}

	public void boom() {
		throw new IllegalStateException("kaboom");
	}

	@LogUsage(action = "custom.action.name")
	public String custom() {
		return "ok";
	}

	/**
	 * Delegates to another auto-logged service-impl bean through the Spring proxy.
	 */
	public String callsHelper() {
		return helper.helperMethod();
	}
}
