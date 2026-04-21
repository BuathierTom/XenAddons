package xenus.xenaddons;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class XenAddonsPreLaunch implements PreLaunchEntrypoint {
	@Override
	public void onPreLaunch() {
		// DevAuth reads its enable flag very early during startup, so force it here.
		System.setProperty("devauth.enabled", "true");

		String account = System.getProperty("xenaddons.devauth.account");
		if (account == null || account.isBlank()) {
			account = System.getenv("XENADDONS_DEVAUTH_ACCOUNT");
		}
		if (account != null && !account.isBlank()) {
			System.setProperty("devauth.account", account);
		}
	}
}
