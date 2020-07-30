package xyz.derkades.serverselectorx.placeholders;

// This class needs to exist, because any PlaceholderAPI only APIs can not be used in the Main class
// or there will be a NoClassDefFound error when you don't have PAPI installed.
public class PapiExpansionRegistrar {

	@SuppressWarnings("deprecation")
	public static void register() {
		// This method is not actually deprecated, the developer said they just wanted to bring attention
		// to the fact that the method is going to be final in the future, which they abused deprecation for.
		new PapiExpansion().register();
	}

}
