package com.plotsquared.bukkit.titles;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.plotsquared.bukkit.chat.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class HackTitleManager extends TitleManager {

	/**
	 * Create a new 1.8 title.
	 *
	 * @param title       Title text
	 * @param subtitle    Subtitle text
	 * @param fadeInTime  Fade in time
	 * @param stayTime    Stay on screen time
	 * @param fadeOutTime Fade out time
	 */
	public HackTitleManager(String title, String subtitle, int fadeInTime, int stayTime, int fadeOutTime)
	{
		super(title, subtitle, fadeInTime, stayTime, fadeOutTime);
	}

	/**
	 * Load spigot and NMS classes.
	 */
	@Override
	void loadClasses()
	{
		this.packetTitle = this.getClass("org.spigotmc.ProtocolInjector$PacketTitle");
		this.packetActions = this.getClass("org.spigotmc.ProtocolInjector$PacketTitle$Action");
		this.nmsChatSerializer = Reflection.getNMSClass("ChatSerializer");
	}

	@Override
	public void send(Player player) throws IllegalArgumentException, ReflectiveOperationException, SecurityException
	{
		if ((this.getProtocolVersion(player) >= 47) && this.isSpigot() && (this.packetTitle != null))
		{
			// First reset previous settings
			this.resetTitle(player);
			// Send timings first
			Object handle = this.getHandle(player);
			Object connection = this.getField(handle.getClass(), "playerConnection").get(handle);
			Object[] actions = this.packetActions.getEnumConstants();
			Method sendPacket = this.getMethod(connection.getClass(), "sendPacket");
			Object packet = this.packetTitle.getConstructor(this.packetActions, Integer.TYPE, Integer.TYPE, Integer.TYPE).newInstance(actions[2],
																																	  this.fadeInTime * (this.ticks ? 1 : 20),
																																	  this.stayTime * (this.ticks ? 1 : 20),
																																	  this.fadeOutTime * (this.ticks ? 1 : 20));
			// Send if set
			if ((this.fadeInTime != -1) && (this.fadeOutTime != -1) && (this.stayTime != -1))
			{
				sendPacket.invoke(connection, packet);
			}
			// Send title
			Object serialized = this.getMethod(this.nmsChatSerializer, "a", String.class).invoke(null,
																								 "{text:\"" + ChatColor.translateAlternateColorCodes('&', this.getTitle()) + "\",color:" + this.titleColor.name().toLowerCase()
																								 + '}');
			packet = this.packetTitle.getConstructor(this.packetActions, Reflection.getNMSClass("IChatBaseComponent"))
									 .newInstance(actions[0], serialized);
			sendPacket.invoke(connection, packet);
			if (!this.getSubtitle().isEmpty())
			{
				// Send subtitle if present
				serialized = this.getMethod(this.nmsChatSerializer, "a", String.class).invoke(null,
																							  "{text:\"" + ChatColor.translateAlternateColorCodes('&', this.getSubtitle()) + "\",color:" + this.subtitleColor.name()
																																																		.toLowerCase() + '}');
				packet = this.packetTitle.getConstructor(this.packetActions, Reflection.getNMSClass("IChatBaseComponent"))
										 .newInstance(actions[1], serialized);
				sendPacket.invoke(connection, packet);
			}
		}
	}

	@Override
	public void clearTitle(Player player) throws IllegalArgumentException, ReflectiveOperationException, SecurityException
	{
		if ((this.getProtocolVersion(player) >= 47) && this.isSpigot())
		{
			// Send timings first
			Object handle = this.getHandle(player);
			Object connection = this.getField(handle.getClass(), "playerConnection").get(handle);
			Object[] actions = this.packetActions.getEnumConstants();
			Method sendPacket = this.getMethod(connection.getClass(), "sendPacket");
			Object packet = this.packetTitle.getConstructor(this.packetActions).newInstance(actions[3]);
			sendPacket.invoke(connection, packet);
		}
	}

	@Override
	public void resetTitle(Player player) throws IllegalArgumentException, ReflectiveOperationException, SecurityException
	{
		if ((this.getProtocolVersion(player) >= 47) && this.isSpigot())
		{
			// Send timings first
			Object handle = this.getHandle(player);
			Object connection = this.getField(handle.getClass(), "playerConnection").get(handle);
			Object[] actions = this.packetActions.getEnumConstants();
			Method sendPacket = this.getMethod(connection.getClass(), "sendPacket");
			Object packet = this.packetTitle.getConstructor(this.packetActions).newInstance(actions[4]);
			sendPacket.invoke(connection, packet);
		}
	}

	/**
	 * Get the protocol version of the player.
	 *
	 * @param player Player
	 * @return Protocol version
	 * @throws IllegalArgumentException
	 * @throws ReflectiveOperationException
	 * @throws SecurityException
	 */
	private int getProtocolVersion(Player player) throws IllegalArgumentException, ReflectiveOperationException, SecurityException
	{
		Object handle = this.getHandle(player);
		Object connection = this.getField(handle.getClass(), "playerConnection").get(handle);
		Object networkManager = this.getValue("networkManager", connection);
		return (Integer) this.getMethod("getVersion", networkManager.getClass()).invoke(networkManager);
	}

	/**
	 * Check if running spigot.
	 *
	 * @return Spigot
	 */
	private boolean isSpigot()
	{
		return Bukkit.getVersion().contains("Spigot");
	}

	/**
	 * Get class by url.
	 *
	 * @param namespace Namespace url
	 * @return Class
	 */
	private Class<?> getClass(String namespace)
	{
		try
		{
			return Class.forName(namespace);
		}
		catch (ClassNotFoundException ignored) {}
		return null;
	}

	private Field getField(String name, Class<?> clazz) throws NoSuchFieldException, SecurityException
	{
		return clazz.getDeclaredField(name);
	}

	private Object getValue(String name, Object obj) throws ReflectiveOperationException, SecurityException, IllegalArgumentException
	{
		Field f = this.getField(name, obj.getClass());
		f.setAccessible(true);
		return f.get(obj);
	}

	private Field getField(Class<?> clazz, String name)
	{
		try
		{
			Field field = clazz.getDeclaredField(name);
			field.setAccessible(true);
			return field;
		}
		catch (SecurityException | NoSuchFieldException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	private Method getMethod(Class<?> clazz, String name, Class<?>... args)
	{
		for (Method m : clazz.getMethods())
		{
			if (m.getName().equals(name) && ((args.length == 0) || this.classListEqual(args, m.getParameterTypes())))
			{
				m.setAccessible(true);
				return m;
			}
		}
		return null;
	}

	private boolean classListEqual(Class<?>[] l1, Class<?>[] l2)
	{
		if (l1.length != l2.length)
		{
			return false;
		}
		boolean equal = true;
		for (int i = 0; i < l1.length; i++)
		{
			if (l1[i] != l2[i])
			{
				equal = false;
				break;
			}
		}
		return equal;
	}
}
