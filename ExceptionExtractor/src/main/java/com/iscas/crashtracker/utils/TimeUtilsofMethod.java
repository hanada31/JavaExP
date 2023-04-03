package com.iscas.crashtracker.utils;

import com.iscas.crashtracker.base.MyConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.Timer;
import java.util.TimerTask;
@Slf4j
public class TimeUtilsofMethod {

	private boolean methodTimeout = false;
	private final Timer timer;
	public TimeUtilsofMethod() {
		timer = new Timer();
	}

	public void schedule() {
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				setMethodTimeout(true);
				log.info("Skip current method due to timeout of 1 minutes!");
			}
			// }, MyConfig.getInstance().getTimeLimit() * 5 *1000 );
		}, MyConfig.getInstance().getTimeLimit() * 1000);
	}

	public void cancel() {
		timer.cancel();
	}

	/**
	 * @return the methodTimeout
	 */
	public boolean isMethodTimeout() {
		return methodTimeout;
	}

	/**
	 * @param methodTimeout
	 *            the methodTimeout to set
	 */
	public void setMethodTimeout(boolean methodTimeout) {
		this.methodTimeout = methodTimeout;
	}
}
