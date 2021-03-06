package com.intellectualcrafters.plot.object;

import java.util.Set;
import java.util.UUID;

public final class PlotHandler {
	private PlotHandler() {}

	public static boolean sameOwners(Plot plot1, Plot plot2)
	{
		if (plot1.owner == null || plot2.owner == null)
		{
			return false;
		}
		Set<UUID> owners = plot1.getOwners();
		for (UUID owner : owners)
		{
			if (plot2.isOwner(owner))
			{
				return true;
			}
		}
		return false;
	}
}
