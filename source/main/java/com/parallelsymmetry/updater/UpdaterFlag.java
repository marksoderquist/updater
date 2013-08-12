package com.parallelsymmetry.updater;

import com.parallelsymmetry.utility.CommonFlag;

public interface UpdaterFlag extends CommonFlag {

	public static final String LAUNCH = "--launch";

	public static final String LAUNCH_DELAY = "-launch.delay";

	public static final String LAUNCH_ELEVATED = "-launch.elevated";

	public static final String LAUNCH_HOME = "-launch.home";

	public static final String UPDATE = "--update";

	public static final String UPDATE_DELAY = "-update.delay";

}