module com.parallelsymmetry.updater {
	exports com.parallelsymmetry.updater;
	// Compile time only

	// Compile and runtime
	requires com.parallelsymmetry.utility;
	requires java.desktop;
	requires java.management;
	requires java.logging;
}
