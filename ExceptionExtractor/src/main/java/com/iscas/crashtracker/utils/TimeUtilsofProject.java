package com.iscas.crashtracker.utils;

import com.iscas.crashtracker.base.MyConfig;
import com.iscas.crashtracker.client.BaseClient;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public class TimeUtilsofProject {
	/**
	 * Analyze will terminate when timeout.
	 * 
	 * @param client
	 */
	public static void setTotalTimer(BaseClient client) {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				log.info(MyConfig.getInstance().getClient() + " time = "
						+ MyConfig.getInstance().getTimeLimit() + " minutes, timeout!");
				MyConfig.getInstance().setStopFlag(true);
				try {
					client.clientOutput();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (DocumentException e) {
					e.printStackTrace();
				}
				System.exit(0);
			}
		}, MyConfig.getInstance().getTimeLimit() * 60 * 1000);

	}
}
