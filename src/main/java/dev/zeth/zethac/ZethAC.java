package dev.zeth.zethac;

import dev.zeth.zethac.alerts.AlertManager;
import dev.zeth.zethac.api.ZethACAPI;
import dev.zeth.zethac.checks.combat.*;
import dev.zeth.zethac.checks.movement.*;
import dev.zeth.zethac.checks.player.*;
import dev.zeth.zethac.checks.world.XRayCheck;
import dev.zeth.zethac.commands.ZethACCommand;
import dev.zeth.zethac.config.ConfigManager;
import dev.zeth.zethac.data.PlayerDataManager;
import dev.zeth.zethac.events.PacketListener;
import dev.zeth.zethac.manager.CheckManager;
import dev.zeth.zethac.manager.PunishmentManager;
import dev.zeth.zethac.utils.LogManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ZethAC extends JavaPlugin {

    private static ZethAC instance;
    private ConfigManager     configManager;
    private PlayerDataManager playerDataManager;
    private CheckManager      checkManager;
    private AlertManager      alertManager;
    private PunishmentManager punishmentManager;
    private LogManager        logManager;

    @Override
    public void onEnable() {
        instance = this;
        long start = System.currentTimeMillis();

        saveDefaultConfig();
        configManager     = new ConfigManager(this);
        logManager        = new LogManager(this);
        playerDataManager = new PlayerDataManager(this);
        alertManager      = new AlertManager(this);
        punishmentManager = new PunishmentManager(this);
        checkManager      = new CheckManager(this);

        registerChecks();
        new PacketListener(this);

        ZethACAPI.init(this);

        ZethACCommand cmd = new ZethACCommand(this);
        var zac = getCommand("zethac");
        if (zac != null) { zac.setExecutor(cmd); zac.setTabCompleter(cmd); }

        startTickTask();
        printBanner(System.currentTimeMillis() - start);
    }

    @Override
    public void onDisable() {
        if (logManager != null) logManager.close();
        if (playerDataManager != null) playerDataManager.saveAll();
        getLogger().info("ZethAC disabled. All data saved.");
    }

    private void registerChecks() {
        // Combat
        checkManager.register(new KillAuraCheck(this));
        checkManager.register(new ReachCheck(this));
        checkManager.register(new AutoClickerCheck(this));
        checkManager.register(new AimAssistCheck(this));
        checkManager.register(new VelocityCheck(this));
        checkManager.register(new CriticalsCheck(this));
        checkManager.register(new NoSwingCheck(this));
        // Movement
        checkManager.register(new SpeedCheck(this));
        checkManager.register(new FlyCheck(this));
        checkManager.register(new NoFallCheck(this));
        checkManager.register(new StepCheck(this));
        checkManager.register(new PhaseCheck(this));
        checkManager.register(new TimerCheck(this));
        checkManager.register(new SpiderCheck(this));
        checkManager.register(new JesusCheck(this));
        checkManager.register(new GroundSpoofCheck(this));
        checkManager.register(new InvalidMotionCheck(this));
        // Player
        checkManager.register(new BadPacketsCheck(this));
        checkManager.register(new ScaffoldCheck(this));
        checkManager.register(new FastPlaceCheck(this));
        checkManager.register(new FastBreakCheck(this));
        checkManager.register(new NukerCheck(this));
        checkManager.register(new ChestStealerCheck(this));
        // World
        checkManager.register(new XRayCheck(this));
    }

    private void startTickTask() {
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (var data : playerDataManager.getAll()) {
                data.tick();
                if (checkManager.isCheckable(data)) {
                    checkManager.tickAll(data);
                }
            }
        }, 1L, 1L);
    }

    private void printBanner(long ms) {
        getLogger().info("\u00a7b  ______     _   _       ___   ____");
        getLogger().info("\u00a7b |_  / __|_| |_| |_     /_\\ / ___|");
        getLogger().info("\u00a7b  / / _/ -_)  _| ' \\   / _ \\\\__ \\");
        getLogger().info("\u00a7b /___\\__\\___|\\__|_||_| /_/ \\_\\___/");
        getLogger().info("\u00a77  by Zeth Development");
        getLogger().info(String.format("\u00a73  Advanced Anti-Cheat v%s", getDescription().getVersion()));
        getLogger().info(String.format("ZethAC enabled in %dms — %d checks active.", ms, checkManager.getCheckCount()));
    }

    public static ZethAC getInstance()             { return instance; }
    public ConfigManager     getConfigManager()    { return configManager; }
    public PlayerDataManager getPlayerDataManager(){ return playerDataManager; }
    public CheckManager      getCheckManager()     { return checkManager; }
    public AlertManager      getAlertManager()     { return alertManager; }
    public PunishmentManager getPunishmentManager(){ return punishmentManager; }
    public LogManager        getLogManager()       { return logManager; }
}
