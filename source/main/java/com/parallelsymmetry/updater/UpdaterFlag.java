package com.parallelsymmetry.updater;

import com.parallelsymmetry.utility.CommonFlag;

public interface UpdaterFlag extends CommonFlag {

	String CALLBACK = "-callback";

	String ELEVATED = "-elevated";

	String LAUNCH = "--launch";

	String LAUNCH_DELAY = "-launch.delay";

	String LAUNCH_HOME = "-launch.home";

	String STDIN = "-stdin";

	String UI = "-ui";

	String UI_MESSAGE = "-ui.message";

	String UPDATE = "--update";

	String UPDATE_DELAY = "-update.delay";

}
