package com.parallelsymmetry.updater;

import com.parallelsymmetry.utility.CommonFlag;

public interface UpdaterFlag extends CommonFlag {

	public static final String CALLBACK = "-callback";

	public static final String ELEVATED = "-elevated";

	public static final String LAUNCH = "--launch";

	public static final String LAUNCH_DELAY = "-launch.delay";

	public static final String LAUNCH_HOME = "-launch.home";

	public static final String STDIN = "-stdin";

	public static final String UI = "-ui";

	public static final String UI_MESSAGE = "-ui.message";

	public static final String UPDATE = "--update";

	public static final String UPDATE_DELAY = "-update.delay";

}
