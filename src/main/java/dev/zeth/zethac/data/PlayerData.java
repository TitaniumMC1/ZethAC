package dev.zeth.zethac.data;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerData {

    // Player reference
    private final Player player;
    private final UUID uuid;

    // Violation tracking
    private final Map<String, Integer> violations = new HashMap<>();
    private final Map<String, Long> lastViolationTime = new HashMap<>();
    private final List<ViolationEntry> violationLog = new ArrayList<>();

    // Position data
    private Location lastLocation;
    private Location currentLocation;
    private final Deque<Location> positionHistory = new ArrayDeque<>();
    public static final int POSITION_HISTORY_SIZE = 20;

    // Movement deltas
    private double deltaX, deltaY, deltaZ;
    private double lastDeltaX, lastDeltaY, lastDeltaZ;
    private double horizontalSpeed;

    // Ground state
    private boolean onGround;
    private boolean lastOnGround;

    // Player states
    private boolean sprinting, sneaking, flying, gliding, swimming;

    // Tick counters
    private int airTicks, groundTicks;
    private double fallDistance;
    private long lastMoveTime;
    private int moveCount;
    private long moveWindowStart;

    // Rotation
    private float yaw, pitch, lastYaw, lastPitch;
    private float deltaYaw, deltaPitch;
    private final Deque<float[]> rotationHistory = new ArrayDeque<>();
    public static final int ROTATION_HISTORY_SIZE = 10;

    // Combat
    private long lastAttackTime;
    private int attacksThisTick;
    private final List<Long> clickTimes = new ArrayList<>();
    private double lastReach;
    private int targetsThisTick;
    private final Set<UUID> targetsHitThisTick = new HashSet<>();
    private boolean swungArm;
    private int consecutiveNoSwing;

    // Block interaction
    private long lastPlaceTime;
    private long lastBreakTime;
    private int blocksPlacedThisTick;
    private int blocksBrokenThisTick;
    private Location lastBreakLocation;

    // World checks
    private int oreMineCount;
    private long oreWindowStart;

    // Inventory
    private long lastInventoryAction;
    private int inventoryActionsThisTick;

    // Velocity / knockback
    private double expectedVelocityX, expectedVelocityY, expectedVelocityZ;
    private boolean velocityApplied;
    private long velocityAppliedTime;

    // Misc
    private int ping;
    private boolean verboseMode;
    private boolean receiveAlerts;
    private long ticks;

    // ---- FIX: added for GrimAC-inspired grace period tracking ----
    private int ticksSinceLastTeleport = 999;
    private int ticksSinceTargetSwitch = 999;
    private int ticksSinceBlockChange  = 999;
    private int ticksSincePotionChange = 999;
    private int ticksSinceVelocityApplied = 999;

    public PlayerData(Player player) {
        this.player = player;
        this.uuid = player.getUniqueId();
        this.currentLocation = player.getLocation().clone();
        this.lastLocation = currentLocation.clone();
        this.yaw = player.getLocation().getYaw();
        this.pitch = player.getLocation().getPitch();
        this.moveWindowStart = System.currentTimeMillis();
        this.oreWindowStart = System.currentTimeMillis();
        this.ping = player.getPing();
    }

    public void tick() {
        long now = System.currentTimeMillis();

        // Decay click times older than 1 second
        clickTimes.removeIf(t -> now - t > 1000L);

        // Reset per-tick counters
        attacksThisTick = 0;
        targetsThisTick = 0;
        targetsHitThisTick.clear();
        blocksPlacedThisTick = 0;
        blocksBrokenThisTick = 0;
        inventoryActionsThisTick = 0;
        swungArm = false;
        moveCount++;
        ticks++;
        ping = player.getPing();

        // FIX: increment grace tick counters
        if (ticksSinceLastTeleport < 999)  ticksSinceLastTeleport++;
        if (ticksSinceTargetSwitch < 999)  ticksSinceTargetSwitch++;
        if (ticksSinceBlockChange < 999)   ticksSinceBlockChange++;
        if (ticksSincePotionChange < 999)  ticksSincePotionChange++;
        if (ticksSinceVelocityApplied < 999) ticksSinceVelocityApplied++;
    }

    public int addViolation(String checkName, String detail) {
        int current = violations.getOrDefault(checkName, 0) + 1;
        violations.put(checkName, current);
        lastViolationTime.put(checkName, System.currentTimeMillis());

        violationLog.add(new ViolationEntry(checkName, detail, System.currentTimeMillis()));
        if (violationLog.size() > 100) violationLog.remove(0);

        return current;
    }

    public void decayViolation(String checkName, int amount) {
        violations.merge(checkName, -amount, (a, b) -> Math.max(0, a + b));
    }

    public int getViolations(String checkName) {
        return violations.getOrDefault(checkName, 0);
    }

    public Map<String, Integer> getAllViolations() {
        return Collections.unmodifiableMap(violations);
    }

    public List<ViolationEntry> getViolationLog() {
        return Collections.unmodifiableList(violationLog);
    }

    public void registerClick() {
        clickTimes.add(System.currentTimeMillis());
    }

    public double getCPS() {
        long now = System.currentTimeMillis();
        long count = clickTimes.stream().filter(t -> now - t <= 1000L).count();
        return count;
    }

    public double getCPSVariance() {
        if (clickTimes.size() < 2) return 0;
        List<Long> sorted = new ArrayList<>(clickTimes);
        Collections.sort(sorted);
        List<Double> intervals = new ArrayList<>();
        for (int i = 1; i < sorted.size(); i++) {
            intervals.add((double)(sorted.get(i) - sorted.get(i-1)));
        }
        double avg = intervals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return intervals.stream().mapToDouble(d -> Math.pow(d - avg, 2)).average().orElse(0);
    }

    public void updatePosition(Location to, boolean onGround) {
        this.lastLocation = this.currentLocation != null ? this.currentLocation.clone() : to.clone();
        this.currentLocation = to.clone();

        this.lastDeltaX = this.deltaX;
        this.lastDeltaY = this.deltaY;
        this.lastDeltaZ = this.deltaZ;

        this.deltaX = to.getX() - lastLocation.getX();
        this.deltaY = to.getY() - lastLocation.getY();
        this.deltaZ = to.getZ() - lastLocation.getZ();
        this.horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        this.lastOnGround = this.onGround;
        this.onGround = onGround;

        if (onGround) {
            airTicks = 0;
            groundTicks++;
            if (deltaY < 0) fallDistance += Math.abs(deltaY);
            else fallDistance = 0;
        } else {
            airTicks++;
            groundTicks = 0;
            fallDistance = 0;
        }

        this.lastMoveTime = System.currentTimeMillis();

        positionHistory.addFirst(to.clone());
        if (positionHistory.size() > POSITION_HISTORY_SIZE) positionHistory.pollLast();
    }

    public void updateRotation(float yaw, float pitch) {
        this.lastYaw   = this.yaw;
        this.lastPitch = this.pitch;
        this.yaw       = yaw;
        this.pitch     = pitch;
        this.deltaYaw   = Math.abs(yaw - lastYaw);
        this.deltaPitch = Math.abs(pitch - lastPitch);

        rotationHistory.addFirst(new float[]{yaw, pitch});
        if (rotationHistory.size() > ROTATION_HISTORY_SIZE) rotationHistory.pollLast();
    }

    // ─── Teleport / grace helpers ───────────────────────────────────────────
    public void onTeleport() {
        ticksSinceLastTeleport = 0;
    }

    public void onTargetSwitch() {
        ticksSinceTargetSwitch = 0;
    }

    public void onBlockChange() {
        ticksSinceBlockChange = 0;
    }

    public void onPotionChange() {
        ticksSincePotionChange = 0;
    }

    public int getTicksSinceLastTeleport()    { return ticksSinceLastTeleport; }
    public int getTicksSinceTargetSwitch()    { return ticksSinceTargetSwitch; }
    public int getTicksSinceBlockChange()     { return ticksSinceBlockChange; }
    public int getTicksSincePotionChange()    { return ticksSincePotionChange; }
    public int getTicksSinceVelocityApplied() { return ticksSinceVelocityApplied; }

    // ─── Getters / Setters ──────────────────────────────────────────────────
    public Player  getPlayer()          { return player; }
    public UUID    getUuid()            { return uuid; }
    public Location getLastLocation()   { return lastLocation; }
    public Location getCurrentLocation(){ return currentLocation; }
    public Deque<Location> getPositionHistory() { return positionHistory; }

    public double getDeltaX()          { return deltaX; }
    public double getDeltaY()          { return deltaY; }
    public double getDeltaZ()          { return deltaZ; }
    public double getLastDeltaY()      { return lastDeltaY; }
    public double getHorizontalSpeed() { return horizontalSpeed; }

    public boolean isOnGround()        { return onGround; }
    public boolean wasOnGround()       { return lastOnGround; }
    public boolean isSprinting()       { return sprinting; }
    public void    setSprinting(boolean v) { sprinting = v; }
    public boolean isSneaking()        { return sneaking; }
    public void    setSneaking(boolean v)  { sneaking = v; }
    public boolean isFlying()          { return flying; }
    public void    setFlying(boolean v)    { flying = v; }
    public boolean isGliding()         { return gliding; }
    public void    setGliding(boolean v)   { gliding = v; }
    public boolean isSwimming()        { return swimming; }
    public void    setSwimming(boolean v)  { swimming = v; }

    public int    getAirTicks()        { return airTicks; }
    public int    getGroundTicks()     { return groundTicks; }
    public double getFallDistance()    { return fallDistance; }
    public long   getLastMoveTime()    { return lastMoveTime; }
    public int    getMoveCount()       { return moveCount; }
    public long   getMoveWindowStart() { return moveWindowStart; }

    public float  getYaw()             { return yaw; }
    public float  getPitch()           { return pitch; }
    public float  getLastYaw()         { return lastYaw; }
    public float  getLastPitch()       { return lastPitch; }
    public float  getDeltaYaw()        { return deltaYaw; }
    public float  getDeltaPitch()      { return deltaPitch; }
    public Deque<float[]> getRotationHistory() { return rotationHistory; }

    public long  getLastAttackTime()   { return lastAttackTime; }
    public void  setLastAttackTime(long t) { lastAttackTime = t; }
    public int   getAttacksThisTick()  { return attacksThisTick; }

    public void  incrementTargetsHit(UUID id) {
        if (targetsHitThisTick.add(id)) {
            attacksThisTick++;
            targetsThisTick++;
            // FIX: detect target switch
            if (targetsHitThisTick.size() > 1) onTargetSwitch();
        }
    }
    public int   getTargetsThisTick() { return targetsThisTick; }
    public double getLastReach()      { return lastReach; }
    public void   setLastReach(double v) { lastReach = v; }

    public boolean isSwungArm()        { return swungArm; }
    public void    setSwungArm(boolean v) { swungArm = v; }
    public int     getConsecutiveNoSwing() { return consecutiveNoSwing; }
    public void    incrementNoSwing()  { consecutiveNoSwing++; }
    public void    resetNoSwing()      { consecutiveNoSwing = 0; }

    public long getLastPlaceTime()     { return lastPlaceTime; }
    public void setLastPlaceTime(long t) { lastPlaceTime = t; }
    public long getLastBreakTime()     { return lastBreakTime; }
    public void setLastBreakTime(long t) { lastBreakTime = t; }
    public int  getBlocksPlacedThisTick()  { return blocksPlacedThisTick; }
    public void incrementBlocksPlaced()    { blocksPlacedThisTick++; }
    public int  getBlocksBrokenThisTick()  { return blocksBrokenThisTick; }
    public void incrementBlocksBroken()    { blocksBrokenThisTick++; }
    public Location getLastBreakLocation() { return lastBreakLocation; }
    public void setLastBreakLocation(Location l) { lastBreakLocation = l; }

    public int  getOreMineCount()      { return oreMineCount; }
    public void incrementOreMine()     { oreMineCount++; }
    public void resetOreMine()         { oreMineCount = 0; }
    public long getOreWindowStart()    { return oreWindowStart; }
    public void setOreWindowStart(long t) { oreWindowStart = t; }

    public long getLastInventoryAction()   { return lastInventoryAction; }
    public void setLastInventoryAction(long t) { lastInventoryAction = t; }
    public int  getInventoryActionsThisTick()  { return inventoryActionsThisTick; }
    public void incrementInventoryAction()     { inventoryActionsThisTick++; }

    public double getExpectedVelocityX() { return expectedVelocityX; }
    public double getExpectedVelocityY() { return expectedVelocityY; }
    public double getExpectedVelocityZ() { return expectedVelocityZ; }
    public void setExpectedVelocity(double x, double y, double z) {
        expectedVelocityX = x;
        expectedVelocityY = y;
        expectedVelocityZ = z;
        velocityApplied = true;
        velocityAppliedTime = System.currentTimeMillis();
        ticksSinceVelocityApplied = 0;
    }
    public boolean isVelocityApplied()  { return velocityApplied; }
    public void    clearVelocity()      { velocityApplied = false; }
    public long    getVelocityAppliedTime() { return velocityAppliedTime; }

    public int     getPing()            { return ping; }
    public long    getTicks()           { return ticks; }
    public boolean isVerboseMode()      { return verboseMode; }
    public void    setVerboseMode(boolean v) { verboseMode = v; }
    public boolean isReceiveAlerts()    { return receiveAlerts; }
    public void    setReceiveAlerts(boolean v) { receiveAlerts = v; }

    // ─── Inner class ────────────────────────────────────────────────────────
    public static class ViolationEntry {
        public final String checkName;
        public final String detail;
        public final long   timestamp;

        public ViolationEntry(String checkName, String detail, long timestamp) {
            this.checkName = checkName;
            this.detail    = detail;
            this.timestamp = timestamp;
        }
    }
}
