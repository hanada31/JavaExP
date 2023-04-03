package com.iscas.crashtracker.client.soot;

import soot.Body;
import soot.BodyTransformer;

import java.util.Map;

public class ActiveBodyTransformer extends BodyTransformer {

	@Override
	/**
	 * must override
	 */
	protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
		b.getMethod().setActiveBody(b);
	}
}