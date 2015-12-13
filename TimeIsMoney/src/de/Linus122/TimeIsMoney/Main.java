package de.Linus122.TimeIsMoney;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;


import modules.atm.ATM;
import net.milkbowl.vault.economy.Economy;
import webapi.VersionChecker;

public class Main extends JavaPlugin{
	
	List<Payout> payouts = new ArrayList<Payout>();
	HashMap<String, Double> payedMoney = new HashMap<String, Double>();
	
	HashMap<Player, Integer> onlineSeconds = new HashMap<Player, Integer>();
	
	HashMap<Player, Location> lastLocation = new HashMap<Player, Location>();
	
	public static Economy economy = null;
	public static Utils utils = null;
	String message;
	
	ConsoleCommandSender clogger = this.getServer().getConsoleSender();
	
	public static int version = 8;
	
	int currentDay = 0;
	
	public static YamlConfiguration finalconfig;
	
	boolean use18Features = true;
	
	@SuppressWarnings({ "deprecation", "unchecked" })
	@Override
	public void onEnable(){
		
		this.getCommand("timeismoney").setExecutor(new Cmd(this));
		
		currentDay = (new Date()).getDay();
		
		File config = new File("plugins/TimeIsMoney/config.yml");
		if(config.exists()){
			YamlConfiguration cfg = YamlConfiguration.loadConfiguration(config);	
			String old_config = "config_old " + cfg.getInt("configuration-version") + ".yml";
			if(cfg.contains("configuration-version")){
				if(cfg.getInt("configuration-version") < version){
					clogger.sendMessage("[TimeIsMoney] §cYOU ARE USING AN OLD CONFIG-VERSION. The plugin CANT work with this.");
					clogger.sendMessage("[TimeIsMoney] §cI have created an new config for you. The old one is saved as config_old.yml.");
					config.renameTo(new File("plugins/TimeIsMoney/" + old_config));
				}
			}
			this.saveDefaultConfig();
			for(String key : cfg.getConfigurationSection("").getKeys(true)){
				if(!this.getConfig().contains(key)){
					this.getConfig().set(key, cfg.get(key));
				}
			}
		}else{
			this.saveDefaultConfig();
		}
		
		new ATM(this);
		
		finalconfig = YamlConfiguration.loadConfiguration(config);
		final int seconds = getConfig().getInt("give_money_every_second");
		Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, new Runnable(){
			public void run(){
				for(Player p : Bukkit.getOnlinePlayers()){
					if(onlineSeconds.containsKey(p)){
						
						onlineSeconds.put(p, onlineSeconds.get(p) + 1);
					}else{
						onlineSeconds.put(p, 1);
					}
					if(onlineSeconds.get(p) > seconds){
						pay(p);
						onlineSeconds.remove(p);
					}
				}
			}
		}, 20L, 20L);
		Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, new Runnable(){
			public void run(){
				if(currentDay != new Date().getDay()){ //Next day, clear payouts!
					payedMoney.clear();
					currentDay = new Date().getDay();
				}
			}
		}, 20L * 60, 20L * 60);
		setupEconomy();
		
		message = finalconfig.getString("message");
		message = message.replace('&', '§');

		try{
		    FileInputStream fis = new FileInputStream(new File("plugins/TimeIsMoney/payed_today.data"));
		    ObjectInputStream ois = new ObjectInputStream(fis);
		    payedMoney = (HashMap<String, Double>) ((HashMap<String, Double>) ois.readObject()).clone();

		    ois.close();	
		}catch(Exception e){
			
		}
		
		loadPayouts();
		
		if(this.version < VersionChecker.getVersion()){
			clogger.sendMessage("[TimeIsMoney] §cYou are using an old version, please update at");
			clogger.sendMessage("§chttps://www.spigotmc.org/resources/time-is-money.12409/");
		}
		
		 String packageName = this.getServer().getClass().getPackage().getName();
        // Get full package string of CraftServer.
        // org.bukkit.craftbukkit.version
        String Bukkitversion = packageName.substring(packageName.lastIndexOf('.') + 1);
        // Get the last element of the package
        try {
            final Class<?> clazz = Class.forName(Bukkitversion + ".NBTUtils");
            // Check if we have a NMSHandler class at that location.
            if (Utils.class.isAssignableFrom(clazz)) { // Make sure it actually implements NMS
                utils = (Utils) clazz.getConstructor().newInstance(); // Set our handler
     
            }
            
        } catch (final Exception e) {
            this.getLogger().severe("Actionbars are not supported on your spigot version, sorry.");
            use18Features = false;
            return;
        }
	    Bukkit.getScheduler().scheduleAsyncDelayedTask(this,  new Runnable(){
	    	public void run(){
	    		VersionChecker.register();
	    	}
	    }, 1L);
		if(Bukkit.getPluginManager().isPluginEnabled("Essentials")){
			this.getLogger().severe("Essentials found. Hook in it -> Will use Essentials's AFK feature if afk is enabled.");
		}
		
	}
	@Override
	public void onDisable(){
	    FileOutputStream fos;
	    try {
	    	fos = new FileOutputStream(new File("plugins/TimeIsMoney/payed_today.data"));
	        ObjectOutputStream oos = new ObjectOutputStream(fos);
	        oos.writeObject(payedMoney);
	        oos.close();
	    }catch(Exception e){
	    	
	    }
	}
	public void reload(){
		File config = new File("plugins/TimeIsMoney/config.yml");
		finalconfig = YamlConfiguration.loadConfiguration(config);
		
		loadPayouts();
	}
	public void loadPayouts(){
		try{
			payouts.clear();
			for(String key : finalconfig.getConfigurationSection("payouts").getKeys(false)){
				Payout payout = new Payout();
				payout.max_payout_per_day = finalconfig.getDouble("payouts." + key + ".max_payout_per_day");
				payout.payout_amount = finalconfig.getDouble("payouts." + key + ".payout_amount");
				if(finalconfig.getString("payouts." + key + ".permission") != null){
					payout.permission = finalconfig.getString("payouts." + key + ".permission");	
				}
				if(finalconfig.getString("payouts." + key + ".commands") != null){
					payout.commands = finalconfig.getStringList("payouts." + key + ".commands");
				}
				payouts.add(payout);
			}
			clogger.sendMessage("[TimeIsMoney] §aLoaded " + finalconfig.getConfigurationSection("payouts").getKeys(false).size() + " Payouts!");
		}catch(Exception e){
			e.printStackTrace();
			clogger.sendMessage("[TimeIsMoney] §aFailed to load Payouts! (May made an mistake in config.yml?)");
		}
	}
    boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
    public Payout getPayOutForPlayer(Player p){
    	Payout finalpayout = null;
		for(Payout payout: payouts){
			if(payout.permission == "") finalpayout = payout;
			if(p.hasPermission(payout.permission)){
				finalpayout = payout;
			}
		}
    	return finalpayout;
    }
	@SuppressWarnings("deprecation")
	public void pay(Player p){
		//REACHED MAX PAYOUT CHECK
		double payed = 0;
		if(payedMoney.containsKey(p.getName())){
			payed = payedMoney.get(p.getName());
		}
		Payout payout = getPayOutForPlayer(p);
		if(payout == null) return;
		if(payed >= payout.max_payout_per_day){ //Reached max payout
			if(finalconfig.getBoolean("display-messages-in-chat")){
				p.sendMessage(finalconfig.getString("message_payoutlimit_reached").replace('&', '§'));
			}
			if(finalconfig.getBoolean("display-messages-in-actionbar") && use18Features){
				sendActionbar(p, finalconfig.getString("message_payoutlimit_reached").replace('&', '§'));
			}
			return;
		}
		
		//AFK CHECK
		if(!finalconfig.getBoolean("afk_payout")){
			//ESENTIALS_AFK_FEATURE
			if(Bukkit.getServer().getPluginManager().isPluginEnabled("Essentials")){
				com.earth2me.essentials.Essentials essentials = (com.earth2me.essentials.Essentials) Bukkit.getServer().getPluginManager().getPlugin("Essentials");
			    if(essentials.getUser(p).isAfk()){
			    	//AFK
					if(finalconfig.getBoolean("display-messages-in-chat")){
						p.sendMessage(finalconfig.getString("message_afk").replace('&', '§'));	
					}
					if(finalconfig.getBoolean("display-messages-in-actionbar") && use18Features){
						sendActionbar(p, finalconfig.getString("message_afk").replace('&', '§'));
					}
					return;
			    }
			}else
			//PLUGIN_AFK_FEATURE
			if(lastLocation.containsKey(p)){ //AntiAFK
				if(lastLocation.get(p).getX() == p.getLocation().getX() && lastLocation.get(p).getY() == p.getLocation().getY() && lastLocation.get(p).getZ() == p.getLocation().getZ() || lastLocation.get(p).getYaw() == p.getLocation().getYaw()){
					//AFK
					if(finalconfig.getBoolean("display-messages-in-chat")){
						p.sendMessage(finalconfig.getString("message_afk").replace('&', '§'));	
					}
					if(finalconfig.getBoolean("display-messages-in-actionbar") && use18Features){
						sendActionbar(p, finalconfig.getString("message_afk").replace('&', '§'));
					}
					return;
				}
			}	
		}
		
		//DEPOSIT
		if(finalconfig.getBoolean("store-money-in-bank")){
			String bank = p.getName() + "_TimBANK";
			if(!Main.economy.hasAccount(bank)){
				Main.economy.createPlayerAccount(bank);
			}
			Main.economy.depositPlayer(bank, payout.payout_amount);
		}else{
			economy.depositPlayer(p, payout.payout_amount);	
		}
		if(finalconfig.getBoolean("display-messages-in-chat")){
			p.sendMessage(message.replace("%money%", economy.format(payout.payout_amount)));	
		}
		if(finalconfig.getBoolean("display-messages-in-actionbar") && use18Features){
			sendActionbar(p, message.replace("%money%", economy.format(payout.payout_amount)));
		}
		for(String cmd : payout.commands){
			this.getServer().dispatchCommand(this.getServer().getConsoleSender(), cmd.replace("/", "").replaceAll("%player%", p.getName()));
		}
		
		//ADD PAYED MONEY
		if(payedMoney.containsKey(p.getName())){
			payedMoney.put(p.getName(), payedMoney.get(p.getName()) + payout.payout_amount);
		}else{
			payedMoney.put(p.getName(), payout.payout_amount);
		}
		
		lastLocation.put(p, p.getLocation());
	
	}
	public void sendActionbar(final Player p, final String msg){
		int times = finalconfig.getInt("display-messages-in-actionbar-time");
		if(times == 1){
			utils.sendActionBarMessage(p, msg);
		}else if(times > 1){
			utils.sendActionBarMessage(p, msg);
			times--;
			for(int i = 0; i < times; i++){
				Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
					public void run(){
						utils.sendActionBarMessage(p, msg);
					}
				}, 20L * i);
			}
		}
		
		
	}
}
