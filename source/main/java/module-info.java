module com.parallelsymmetry.updater {
	// Compile time only

	// Compile and runtime
	requires com.parallelsymmetry.utility;
	requires java.desktop;
	requires java.logging;
	requires java.management;

	exports com.parallelsymmetry.updater;
}
