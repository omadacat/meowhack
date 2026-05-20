package cat.omada.meowhack.modules.movement;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import cat.omada.meowhack.util.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.ChestSwap;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.MovementType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/* Tweaked version of ElytraFlyPlusPlus from Jeff Mod with additional features and Quality of Life improvements */

public class ElytraBounce extends Module {

    private final SettingGroup sgGeneral        = settings.getDefaultGroup();
    private final SettingGroup sgObstaclePasser = settings.createGroup("Obstacle Passer");
    private final SettingGroup sgDiagBounce     = settings.createGroup("Diag Bounce");
    private final Setting<Boolean> bounce = sgGeneral.add(new BoolSetting.Builder()
        .name("bounce")
        .description("Automatically does bounce efly.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> motionYBoost = sgGeneral.add(new BoolSetting.Builder()
        .name("motion-y-boost")
        .description("Greatly increases speed by cancelling Y momentum.")
        .defaultValue(false)
        .visible(bounce::get)
        .build()
    );

    private final Setting<Boolean> onlyWhileColliding = sgGeneral.add(new BoolSetting.Builder()
        .name("only-while-colliding")
        .description("Only enables motion y boost if colliding with a wall.")
        .defaultValue(true)
        .visible(() -> bounce.get() && motionYBoost.get())
        .build()
    );

    private final Setting<Boolean> tunnelBounce = sgGeneral.add(new BoolSetting.Builder()
        .name("tunnel-bounce")
        .description("Allows you to bounce in 1x2 tunnels. This should not be on if you are not in a tunnel.")
        .defaultValue(false)
        .visible(() -> bounce.get() && motionYBoost.get())
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("The speed in blocks per second to keep you at.")
        .defaultValue(100.0)
        .sliderRange(20, 250)
        .visible(() -> bounce.get() && motionYBoost.get())
        .build()
    );

    private final Setting<Boolean> lockPitch = sgGeneral.add(new BoolSetting.Builder()
        .name("lock-pitch")
        .description("Whether to lock your pitch when bounce is enabled.")
        .defaultValue(true)
        .visible(bounce::get)
        .build()
    );

    private final Setting<Double> pitch = sgGeneral.add(new DoubleSetting.Builder()
        .name("pitch")
        .description("The pitch to set when bounce is enabled.")
        .defaultValue(90.0)
        .sliderRange(-90, 90)
        .visible(() -> bounce.get() && lockPitch.get())
        .build()
    );

    private final Setting<Boolean> freePitch = sgGeneral.add(new BoolSetting.Builder()
        .name("free-pitch")
        .description("Server pitch stays locked for bouncing while your camera pitch is free to look around.")
        .defaultValue(true)
        .visible(() -> bounce.get() && lockPitch.get())
        .build()
    );

    private final Setting<Boolean> lockYaw = sgGeneral.add(new BoolSetting.Builder()
        .name("lock-yaw")
        .description("Whether to lock your yaw when bounce is enabled.")
        .defaultValue(false)
        .visible(bounce::get)
        .build()
    );

    private final Setting<Boolean> useCustomYaw = sgGeneral.add(new BoolSetting.Builder()
        .name("use-custom-yaw")
        .description("Enable this if you want to use a yaw that isn't a factor of 45. WARNING: This effects the baritone goal for obstacle passer, " +
            "use the default Rotations module if you only want a different yawlock.")
        .defaultValue(false)
        .visible(bounce::get)
        .build()
    );

    private final Setting<Double> yaw = sgGeneral.add(new DoubleSetting.Builder()
        .name("yaw")
        .description("The yaw to set when bounce is enabled. This is auto set to the closest 45 deg angle to you unless Use Custom Yaw is enabled. " +
            "WARNING: This effects the baritone goal for obstacle passer, use the default Rotations module if you only want a different yawlock.")
        .defaultValue(0.0)
        .sliderRange(0, 359)
        .visible(() -> bounce.get() && useCustomYaw.get())
        .build()
    );

    private final Setting<Boolean> fakeFly = sgGeneral.add(new BoolSetting.Builder()
        .name("chestplate-fakefly")
        .description("Lets you fly using a chestplate to use almost 0 elytra durability. Must have elytra in hotbar.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> toggleElytra = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-elytra")
        .description("Equips an elytra on activate, and a chestplate on deactivate.")
        .defaultValue(false)
        .visible(() -> !fakeFly.get())
        .build()
    );

    private final Setting<Boolean> highwayObstaclePasser = sgObstaclePasser.add(new BoolSetting.Builder()
        .name("highway-obstacle-passer")
        .description("Uses baritone to pass obstacles.")
        .defaultValue(false)
        .visible(bounce::get)
        .build()
    );

    private final Setting<Boolean> useCustomStartPos = sgObstaclePasser.add(new BoolSetting.Builder()
        .name("use-custom-start-position")
        .description("Enable and set this ONLY if you are on a ringroad or don't want to be locked to a highway. Otherwise (0, 0) is the start position and will be automatically used.")
        .defaultValue(false)
        .visible(() -> bounce.get() && highwayObstaclePasser.get())
        .build()
    );

    private final Setting<BlockPos> startPos = sgObstaclePasser.add(new BlockPosSetting.Builder()
        .name("start-position")
        .description("The start position to use when using a custom start position.")
        .defaultValue(new BlockPos(0,0,0))
        .visible(() -> bounce.get() && highwayObstaclePasser.get() && useCustomStartPos.get())
        .build()
    );

    private final Setting<Boolean> awayFromStartPos = sgObstaclePasser.add(new BoolSetting.Builder()
        .name("away-from-start-position")
        .description("If true, will go away from the start position instead of towards it. The start pos is (0,0) if it is not set to a custom start pos.")
        .defaultValue(true)
        .visible(() -> bounce.get() && highwayObstaclePasser.get())
        .build()
    );

    private final Setting<Double> distance = sgObstaclePasser.add(new DoubleSetting.Builder()
        .name("distance")
        .description("The distance to set the baritone goal for path realignment.")
        .defaultValue(10.0)
        .visible(() -> bounce.get() && highwayObstaclePasser.get())
        .build()
    );

    private final Setting<Integer> targetY = sgObstaclePasser.add(new IntSetting.Builder()
        .name("y-level")
        .description("The Y level to bounce at. This must be correct or bounce will not start properly.")
        .defaultValue(120)
        .visible(() -> bounce.get() && highwayObstaclePasser.get())
        .build()
    );

    private final Setting<Boolean> avoidPortalTraps = sgObstaclePasser.add(new BoolSetting.Builder()
        .name("avoid-portal-traps")
        .description("Will attempt to detect portal traps on chunk load and avoid them.")
        .defaultValue(false)
        .visible(() -> bounce.get() && highwayObstaclePasser.get())
        .build()
    );

    private final Setting<Double> portalAvoidDistance = sgObstaclePasser.add(new DoubleSetting.Builder()
        .name("portal-avoid-distance")
        .description("The distance to a portal trap where the obstacle passer will takeover and go around it.")
        .defaultValue(20)
        .min(0)
        .sliderMax(50)
        .visible(() -> bounce.get() && highwayObstaclePasser.get() && avoidPortalTraps.get())
        .build()
    );

    private final Setting<Integer> portalScanWidth = sgObstaclePasser.add(new IntSetting.Builder()
        .name("portal-scan-width")
        .description("The width on the axis of the highway that will be scanned for portal traps.")
        .defaultValue(5)
        .min(3)
        .sliderMax(10)
        .visible(() -> bounce.get() && highwayObstaclePasser.get() && avoidPortalTraps.get())
        .build()
    );

    private final Setting<Boolean> diagBounce = sgDiagBounce.add(new BoolSetting.Builder()
        .name("diag-bounce")
        .description("High-speed diagonal elytra bounce state machine.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> diagSpeed = sgDiagBounce.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Target speed in blocks per second.")
        .defaultValue(110.0)
        .sliderRange(20.0, 200.0)
        .visible(diagBounce::get)
        .build()
    );

    private final Setting<Double> diagPitch = sgDiagBounce.add(new DoubleSetting.Builder()
        .name("pitch")
        .description("Pitch to lock to while bouncing.")
        .defaultValue(85.0)
        .sliderRange(-90.0, 90.0)
        .visible(diagBounce::get)
        .build()
    );

    private final Setting<Boolean> diagDebugMessages = sgDiagBounce.add(new BoolSetting.Builder()
        .name("debug-messages")
        .description("Show debug messages in chat.")
        .defaultValue(false)
        .visible(diagBounce::get)
        .build()
    );

    private final Setting<Boolean> diagAutoFreeLook = sgDiagBounce.add(new BoolSetting.Builder()
        .name("auto-free-look")
        .description("Enable Meteor's FreeLook module while diag bounce is active.")
        .defaultValue(true)
        .visible(diagBounce::get)
        .build()
    );

    public ElytraBounce() {
        super(
            Categories.Movement,
            "ElytraBounce",
            "Elytra fly with some more features."
        );
    }

    private boolean startSprinting;
    private BlockPos portalTrap = null;
    private boolean paused = false;
    private boolean elytraToggled = false;
    private Vec3d lastUnstuckPos;
    private int stuckTimer = 0;
    private Vec3d lastPos;
    public float cameraPitch;
    public float lockedPitch;
    public float prevEntityPitch;
    public enum DiagState { ALIGNING, BOUNCING, BOOSTING, OBSTACLE_PASSING }

    private static final double DIAG_BOOST_ENGAGE_SPEED = 20.0;
    private static final double DIAG_OBSTACLE_DISTANCE  = 6.0;
    private static final double DIAG_MAX_OBSTACLE_RANGE = 16.0 * 5;

    private DiagState diagState;
    private float diagLockedYaw;
    private Vec3d diagActivationPos;
    private int diagCapturedTargetY;
    private int diagCollisionTicks;
    private boolean diagWasAlignedCollision;
    private Vec3d diagLastPos;
    private int diagStuckTimer;
    private int diagBouncingTicks;
    private int diagZeroSpeedTicks;
    private int diagLowBoostTicks;
    private boolean diagPrevOnGround;
    private boolean diagWasCruising;
    private int diagObstaclePassingTicks;
    private int diagRubberBandCount;
    private int diagDriftTicks;
    private int diagSettleTicksRemaining;
    private int diagPostSlipTicks;
    private int diagPreBounceDelay;
    private int diagPostPasserCooldown;
    private int diagPrePasserDelay;
    private BlockPos diagPendingPasserGoal;
    private boolean diagFreeLookEnabledByUs;
    private boolean baritoneLoaded;
    private boolean diagBaritoneLoaded;

    @Override
    public void onActivate()
    {
        if (mc.player == null || mc.player.getAbilities().allowFlying) return;

        baritoneLoaded = FabricLoader.getInstance().isModLoaded("baritone")
            || FabricLoader.getInstance().isModLoaded("baritone-meteor");
        startSprinting = mc.player.isSprinting();
        tempPath = null;
        portalTrap = null;
        paused = false;
        waitingForChunksToLoad = false;
        elytraToggled = false;
        lastPos = mc.player.getPos();
        lastUnstuckPos = mc.player.getPos();
        stuckTimer = 0;
        cameraPitch = mc.player.getPitch();
        lockedPitch = pitch.get().floatValue();
        prevEntityPitch = lockedPitch;

        if (bounce.get() && baritoneLoaded && mc.player.getPos().multiply(1, 0, 1).length() >= 100)
        {
            if (BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().currentDestination() == null)
            {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(null);
            }

            if (!useCustomStartPos.get())
            {
                startPos.set(new BlockPos(0, 0, 0));
            }

            if (!useCustomYaw.get())
            {
                if (mc.player.getBlockPos().getSquaredDistance(startPos.get()) < 10_000 || !highwayObstaclePasser.get())
                {
                    double playerAngleNormalized = MoveUtils.angleOnAxis(mc.player.getYaw());
                    yaw.set(playerAngleNormalized);
                }
                else
                {
                    BlockPos directionVec = mc.player.getBlockPos().subtract(startPos.get());
                    double angle = Math.toDegrees(Math.atan2(-directionVec.getX(), directionVec.getZ()));
                    double angleNormalized = MoveUtils.angleOnAxis(angle);
                    if (!awayFromStartPos.get())
                    {
                        angleNormalized += 180;
                    }

                    yaw.set(angleNormalized);
                }
            }
        }

        if (diagBounce.get())
        {
            diagBaritoneLoaded = FabricLoader.getInstance().isModLoaded("baritone")
                || FabricLoader.getInstance().isModLoaded("baritone-meteor");
            diagState = DiagState.ALIGNING;
            diagLockedYaw = nearestDiagonal(mc.player.getYaw());
            diagActivationPos = mc.player.getPos();
            diagCapturedTargetY = (int) Math.floor(mc.player.getY());
            diagCollisionTicks = 0;
            diagWasAlignedCollision = false;
            diagLastPos = mc.player.getPos();
            diagStuckTimer = 0;
            diagBouncingTicks = 0;
            diagZeroSpeedTicks = 0;
            diagLowBoostTicks = 0;
            diagPrevOnGround = false;
            diagWasCruising = false;
            diagObstaclePassingTicks = 0;
            diagRubberBandCount = 0;
            diagDriftTicks = 0;
            diagSettleTicksRemaining = 0;
            diagPostSlipTicks = 0;
            diagPreBounceDelay = 0;
            diagPostPasserCooldown = 0;
            diagPrePasserDelay = 0;
            diagPendingPasserGoal = null;
            diagFreeLookEnabledByUs = false;

            if (diagAutoFreeLook.get())
            {
                Module freeLook = Modules.get().get("free-look");
                if (freeLook != null && !freeLook.isActive())
                {
                    freeLook.toggle();
                    diagFreeLookEnabledByUs = true;
                }
            }
        }
    }

    @Override
    public void onDeactivate()
    {
        if (mc.player == null) return;

        if (bounce.get() && baritoneLoaded)
        {
            if (BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().currentDestination() == null)
            {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(null);
            }
        }

        mc.player.setSprinting(startSprinting);

        if (freePitch.get() && lockPitch.get() && bounce.get())
        {
            mc.player.setPitch(cameraPitch);
        }

        if (toggleElytra.get() && !fakeFly.get())
        {
            if (!mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().toString().contains("chestplate")) {
                Modules.get().get(ChestSwap.class).swap();
            }
        }

        if (diagBounce.get())
        {
            diagReleaseForwardKey();
            if (diagBaritoneLoaded && mc.world != null)
            {
                if (BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().currentDestination() == null)
                {
                    BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(null);
                }
            }
            if (diagFreeLookEnabledByUs)
            {
                Module freeLook = Modules.get().get("free-look");
                if (freeLook != null && freeLook.isActive()) freeLook.toggle();
                diagFreeLookEnabledByUs = false;
            }
        }
    }

    @Override
    public String getInfoString()
    {
        if (diagBounce.get() && diagState != null) return diagState.name();
        return null;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event)
    {
        if (event.packet instanceof PlayerRespawnS2CPacket && diagBounce.get())
        {
            mc.execute(() -> { if (isActive()) toggle(); });
            return;
        }

        if (event.packet instanceof PlayerPositionLookS2CPacket && diagBounce.get())
        {
            if (diagState == DiagState.OBSTACLE_PASSING)
            {
                mc.execute(() -> diagDbg("rubber-band (walking) in OBSTACLE_PASSING bps=" + String.format("%.1f", diagHorizontalSpeedBps())));
                return;
            }
            if (diagState == DiagState.BOUNCING || diagState == DiagState.BOOSTING)
            {
                mc.execute(() -> {
                    double bps = diagHorizontalSpeedBps();
                    if (diagPostPasserCooldown > 0)
                    {
                        diagDbg("rubber-band during cooldown (" + diagPostPasserCooldown + "t left), ignoring bps=" + String.format("%.1f", bps));
                        diagLastPos = null;
                        diagWasCruising = false;
                        diagLowBoostTicks = 0;
                        diagState = DiagState.BOUNCING;
                        return;
                    }
                    diagRubberBandCount++;
                    diagDbg("rubber-band #" + diagRubberBandCount + " in " + diagState + " bps=" + String.format("%.1f", bps));
                    if (diagBaritoneLoaded && diagRubberBandCount >= 6)
                    {
                        diagDbg("obstacle: rubber-band loop x" + diagRubberBandCount + ", engaging passer");
                        diagRubberBandCount = 0;
                        diagEngageObstaclePasser();
                        return;
                    }
                    diagLastPos = null;
                    diagWasCruising = false;
                    diagLowBoostTicks = 0;
                    diagState = DiagState.BOUNCING;
                });
                return;
            }
        }

        if (event.packet instanceof CloseScreenS2CPacket)
        {
            event.cancel();
        }
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event)
    {
        if (mc.player == null || event.type != MovementType.SELF) return;

        // Bounce mode Y boost
        if (bounce.get() && motionYBoost.get() && enabled())
        {
            if (!onlyWhileColliding.get() || mc.player.horizontalCollision)
            {
                if (lastPos != null)
                {
                    double speedBps = mc.player.getPos().subtract(lastPos).multiply(20, 0, 20).length();
                    Timer timer = Modules.get().get(Timer.class);
                    if (timer.isActive()) speedBps *= timer.getMultiplier();

                    if (mc.player.isOnGround() && mc.player.isSprinting() && speedBps < speed.get())
                    {
                        if (speedBps > 20 || tunnelBounce.get())
                        {
                            ((IVec3d)event.movement).meteor$setY(0.0);
                            mc.player.setVelocity(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z);
                        }
                    }
                }
            }
            lastPos = mc.player.getPos();
        }

        // Diag Bounce Y boost (BOOSTING state)
        if (diagBounce.get() && diagState == DiagState.BOOSTING)
        {
            if (mc.player.isOnGround() && mc.player.isSprinting())
            {
                double speedBps = mc.player.getVelocity().multiply(1, 0, 1).length() * 20.0;
                if (speedBps < diagSpeed.get())
                {
                    ((IVec3d) event.movement).meteor$setY(0.0);
                    mc.player.setVelocity(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z);
                }
            }
        }
    }

    // 5 chunks forwards
    private final double maxDistance = 16 * 5;
    private BlockPos tempPath = null;
    private boolean waitingForChunksToLoad;

    @EventHandler
    private void onTick(TickEvent.Pre event)
    {
        if (mc.player == null || mc.player.getAbilities().allowFlying) return;

        if (toggleElytra.get() && !fakeFly.get() && !elytraToggled)
        {
            if (!(mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA)))
            {
                Modules.get().get(ChestSwap.class).swap();
            }
            else
            {
                elytraToggled = true;
            }
        }

        if (enabled()) mc.player.setSprinting(true);

        if (bounce.get())
        {
            if (baritoneLoaded && tempPath != null && mc.player.getBlockPos().getSquaredDistance(tempPath) < 500)
            {
                tempPath = null;
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(null);
            }
            else if (baritoneLoaded && tempPath != null)
            {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(tempPath));
                return;
            }

            if (highwayObstaclePasser.get() && baritoneLoaded && BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().getGoal() != null)
            {
                return;
            }

            if (mc.player.squaredDistanceTo(lastUnstuckPos) < 25)
            {
                stuckTimer++;
            }
            else
            {
                stuckTimer = 0;
                lastUnstuckPos = mc.player.getPos();
            }

            if (highwayObstaclePasser.get() && baritoneLoaded && mc.player.getPos().length() > 100 &&
                (mc.player.getY() < targetY.get() || mc.player.getY() > targetY.get() + 2 || (mc.player.horizontalCollision && !mc.player.collidedSoftly)
                || (portalTrap != null && portalTrap.getSquaredDistance(mc.player.getBlockPos()) < portalAvoidDistance.get() * portalAvoidDistance.get())
                || waitingForChunksToLoad
                || stuckTimer > 50))
            {
                waitingForChunksToLoad = false;
                paused = true;
                BlockPos goal = mc.player.getBlockPos();
                double currDistance = distance.get();

                if (portalTrap != null) {
                    currDistance += mc.player.getPos().distanceTo(portalTrap.toCenterPos());
                    portalTrap = null;
                    info("Pathing around portal.");
                }

                do
                {
                    if (currDistance > maxDistance)
                    {
                        tempPath = goal;
                        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(goal));
                        return;
                    }
                    Vec3d unitYawVec = MoveUtils.yawToDirection(yaw.get());
                    Vec3d travelVec = mc.player.getPos().subtract(startPos.get().toCenterPos());

                    double parallelCurrPosDot = travelVec.multiply(new Vec3d(1, 0, 1)).dotProduct(unitYawVec);
                    Vec3d parallelCurrPosComponent = unitYawVec.multiply(parallelCurrPosDot);

                    Vec3d pos = startPos.get().toCenterPos().add(parallelCurrPosComponent);
                    pos = MoveUtils.positionInDirection(pos, yaw.get(), currDistance);

                    goal = new BlockPos((int)(Math.floor(pos.x)), targetY.get(), (int)Math.floor(pos.z));
                    currDistance++;

                    if (mc.world.getBlockState(goal).getBlock() == Blocks.VOID_AIR)
                    {
                        waitingForChunksToLoad = true;
                        return;
                    }
                }
                while (!mc.world.getBlockState(goal.down()).isSolidBlock(mc.world, goal.down()) ||
                    mc.world.getBlockState(goal).getBlock() == Blocks.NETHER_PORTAL ||
                    !mc.world.getBlockState(goal).isAir());
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(goal));
            }
            else
            {
                paused = false;
                if (!enabled()) return;

                if (!fakeFly.get())
                {
                    if (mc.player.isOnGround() && (!motionYBoost.get() || Utils.getPlayerSpeed().multiply(1, 0, 1).length() < speed.get()))
                    {
                        mc.player.jump();
                    }
                }

                if (lockYaw.get())
                {
                    mc.player.setYaw(yaw.get().floatValue());
                }
                if (lockPitch.get())
                {
                    if (freePitch.get()) {
                        lockedPitch = pitch.get().floatValue();
                        prevEntityPitch = lockedPitch;
                    }
                    mc.player.setPitch(pitch.get().floatValue());
                }
            }
        }

        if (enabled())
        {
            if (fakeFly.get())
            {
                doGrimEflyStuff();
            }
            else
            {
                sendStartFlyingPacket();
            }
        }

        if (diagBounce.get())
        {
            diagTick();
        }
    }

    public boolean enabled()
    {
        return this.isActive() && !paused && mc.player != null && (fakeFly.get() || mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA));
    }

    public boolean isFreePitchEnabled()
    {
        return enabled() && bounce.get() && lockPitch.get() && freePitch.get();
    }

    private void doGrimEflyStuff()
    {
        FindItemResult itemResult = InvUtils.findInHotbar(Items.ELYTRA);
        if (!itemResult.found()) return;

        swapToItem(itemResult.slot());
        sendStartFlyingPacket();

        if (bounce.get() && mc.player.isOnGround() && (!motionYBoost.get() || Utils.getPlayerSpeed().multiply(1, 0, 1).length() < speed.get()))
        {
            mc.player.jump();
        }

        swapToItem(itemResult.slot());
    }

    @EventHandler
    private void onPlaySound(PlaySoundEvent event)
    {
        if (!fakeFly.get()) return;
        List<Identifier> armorEquipSounds = List.of(
            Identifier.of("minecraft:item.armor.equip_generic"),
            Identifier.of("minecraft:item.armor.equip_netherite"),
            Identifier.of("minecraft:item.armor.equip_elytra"),
            Identifier.of("minecraft:item.armor.equip_diamond"),
            Identifier.of("minecraft:item.armor.equip_gold"),
            Identifier.of("minecraft:item.armor.equip_iron"),
            Identifier.of("minecraft:item.armor.equip_chain"),
            Identifier.of("minecraft:item.armor.equip_leather"),
            Identifier.of("minecraft:item.elytra.flying")
        );
        for (Identifier identifier : armorEquipSounds) {
            if (identifier.equals(event.sound.getId())) {
                event.cancel();
                break;
            }
        }
    }

    // 38 is the meteor mapping for chestplate
    // serverside uses default mappings: https://imgs.search.brave.com/cyvAxjIhLweeF1qeRXpC_8ESRlImhUmMGWbV_n2to_A/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly9jNGsz/LmdpdGh1Yi5pby93/aWtpLnZnL2ltYWdlcy8xLzEzL0ludmVudG9yeS1zbG90cy5wbmc
    private void swapToItem(int slot) {
        ItemStack chestItem = mc.player.getInventory().getStack(38);
        ItemStack hotbarSwapItem = mc.player.getInventory().getStack(slot);

        Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectOpenHashMap<>();
        changedSlots.put(6, hotbarSwapItem);
        changedSlots.put(slot + 36, chestItem);

        sendSwapPacket(changedSlots, slot);
    }

    private void sendStartFlyingPacket() {
        if (mc.player == null || mc.player.networkHandler == null) return;
        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(
            mc.player,
            ClientCommandC2SPacket.Mode.START_FALL_FLYING
        ));
    }

    private void sendSwapPacket(Int2ObjectMap<ItemStack> changedSlots, int buttonNum) {
        int syncId  = mc.player.currentScreenHandler.syncId;
        int stateId = mc.player.currentScreenHandler.getRevision();

        mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(
            syncId,
            stateId,
            6,
            buttonNum,
            SlotActionType.SWAP,
            new ItemStack(Items.AIR),
            changedSlots
        ));
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event)
    {
        if (!avoidPortalTraps.get() || !highwayObstaclePasser.get()) return;
        ChunkPos pos = event.chunk().getPos();

        BlockPos centerPos = pos.getCenterAtY(targetY.get());

        Vec3d moveDir = MoveUtils.yawToDirection(yaw.get());
        double distanceToHighway = MoveUtils.distancePointToDirection(Vec3d.of(centerPos), moveDir, mc.player.getPos());

        if (distanceToHighway > 21) return;

        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                for (int y = targetY.get(); y < targetY.get() + 3; y++)
                {
                    BlockPos position = new BlockPos(pos.x * 16 + x, y, pos.z * 16 + z);

                    if (MoveUtils.distancePointToDirection(Vec3d.of(position), moveDir, mc.player.getPos()) > portalScanWidth.get()) continue;

                    if (mc.world.getBlockState(position).getBlock().equals(Blocks.NETHER_PORTAL))
                    {
                        BlockPos posBehind = new BlockPos((int)Math.floor(position.getX() + moveDir.x), position.getY(), (int) Math.floor(position.getZ() + moveDir.z));

                        if (mc.world.getBlockState(posBehind).isSolidBlock(mc.world, posBehind) ||
                            mc.world.getBlockState(posBehind).getBlock() == Blocks.NETHER_PORTAL)
                        {
                            if (portalTrap == null || (
                                portalTrap.getSquaredDistance(posBehind) > 100 &&
                                    mc.player.getBlockPos().getSquaredDistance(posBehind) < mc.player.getBlockPos().getSquaredDistance(portalTrap))
                            )
                            {
                                portalTrap = posBehind;
                            }
                        }
                    }
                }
            }
        }
    }

    private void diagTick()
    {
        if (mc.world == null || mc.player.getAbilities().allowFlying) return;

        switch (diagState)
        {
            case ALIGNING         -> diagTickAligning();
            case BOUNCING         -> diagTickBouncing();
            case BOOSTING         -> diagTickBoosting();
            case OBSTACLE_PASSING -> diagTickObstaclePassing();
        }

        diagLastPos = mc.player.getPos();
    }

    private void diagTickAligning()
    {
        if (diagSettleTicksRemaining > 0)
        {
            diagHoldForwardKey(false);
            mc.player.setSprinting(false);
            diagSettleTicksRemaining--;
            return;
        }
        mc.player.setYaw(diagLockedYaw);
        mc.player.setSprinting(true);
        diagHoldForwardKey(true);
        diagStuckTimer++;

        if (mc.player.horizontalCollision)
        {
            diagCollisionTicks++;
            diagPostSlipTicks = 0;
            if (diagCollisionTicks >= 1) diagWasAlignedCollision = true;
            if (diagWasAlignedCollision && diagCollisionTicks > 40)
            {
                diagDbg("align: fallback stuck " + diagCollisionTicks + "t");
                diagEnterBouncing();
            }
        }
        else
        {
            if (diagWasAlignedCollision)
            {
                if (diagPostSlipTicks == 0)
                {
                    diagDbg("align: slip-off at " + diagCollisionTicks + "t, walking 10t");
                    diagPostSlipTicks = 10;
                }
                else
                {
                    diagPostSlipTicks--;
                    if (diagPostSlipTicks == 0) diagEnterBouncing();
                }
            }
            else
            {
                diagCollisionTicks = 0;
            }
        }

        if (!diagWasAlignedCollision && diagStuckTimer > 100)
        {
            diagDbg("align: timeout (" + diagStuckTimer + "t, no collision)");
            diagEnterBouncing();
        }
    }

    private void diagEnterBouncing()
    {
        diagHoldForwardKey(false);
        diagCapturedTargetY = (int) Math.floor(mc.player.getY());
        diagLastPos = mc.player.getPos();
        diagStuckTimer = 0;
        diagBouncingTicks = 0;
        diagZeroSpeedTicks = 0;
        diagRubberBandCount = 0;
        diagDriftTicks = 0;
        diagPostSlipTicks = 0;
        diagPreBounceDelay = 5;
        diagPostPasserCooldown = 40;
        diagWasCruising = false;
        diagCollisionTicks = 0;
        diagWasAlignedCollision = false;
        diagDbg("bounce start: y=" + diagCapturedTargetY);
        diagState = DiagState.BOUNCING;
    }

    private void diagTickBouncing()
    {
        mc.player.setYaw(diagLockedYaw);
        mc.player.setPitch(diagPitch.get().floatValue());
        sendStartFlyingPacket();

        if (diagPreBounceDelay > 0)
        {
            diagPreBounceDelay--;
            if (mc.player.isOnGround()) mc.player.jump();
            return;
        }

        if (mc.player.isOnGround()) mc.player.jump();

        double hspeed = diagHorizontalSpeedBps();
        if (hspeed >= 40.0) diagWasCruising = true;

        if (hspeed >= DIAG_BOOST_ENGAGE_SPEED)
        {
            diagZeroSpeedTicks = 0;
            diagLowBoostTicks = 0;
            diagBouncingTicks = 0;
            diagState = DiagState.BOOSTING;
            return;
        }

        if (diagWasCruising) diagLowBoostTicks++;
        if (diagLowBoostTicks > 40)
        {
            diagDbg("obstacle: stuck low speed bps=" + String.format("%.1f", hspeed));
            diagLowBoostTicks = 0;
            diagEngageObstaclePasser();
            return;
        }

        diagBouncingTicks++;
        if (hspeed < 1.0) diagZeroSpeedTicks++;
        else diagZeroSpeedTicks = 0;

        if (diagBouncingTicks > 200)
        {
            diagDbg("obstacle: bouncing timeout " + diagBouncingTicks + "t");
            diagEngageObstaclePasser();
            return;
        }
        if (diagZeroSpeedTicks > 40)
        {
            diagDbg("obstacle: zero speed " + diagZeroSpeedTicks + "t in BOUNCING");
            diagEngageObstaclePasser();
            return;
        }

        if (diagPostPasserCooldown > 0) diagPostPasserCooldown--;
        else if (diagShouldPassObstacle()) diagEngageObstaclePasser();
    }

    private void diagTickBoosting()
    {
        mc.player.setYaw(diagLockedYaw);
        mc.player.setPitch(diagPitch.get().floatValue());
        sendStartFlyingPacket();

        double hspeed = diagHorizontalSpeedBps();
        boolean onGround = mc.player.isOnGround();
        Vec3d vel = mc.player.getVelocity();

        if (hspeed >= diagSpeed.get() * 0.75) diagRubberBandCount = 0;

        if (!diagPrevOnGround && onGround)
        {
            diagDbg("dbg land: bps=" + String.format("%.1f", hspeed)
                + " vel=(" + String.format("%.2f", vel.x) + "," + String.format("%.2f", vel.y) + "," + String.format("%.2f", vel.z) + ")"
                + " y=" + String.format("%.2f", mc.player.getY()));
        }
        if (diagPrevOnGround && !onGround)
        {
            diagDbg("dbg takeoff: bps=" + String.format("%.1f", hspeed)
                + " velY=" + String.format("%.2f", vel.y)
                + " gliding=" + mc.player.isGliding());
        }
        diagPrevOnGround = onGround;

        if (onGround && hspeed < diagSpeed.get()) mc.player.jump();

        if (hspeed < 1.0) diagZeroSpeedTicks++;
        else diagZeroSpeedTicks = 0;

        if (hspeed >= 40.0) diagWasCruising = true;

        if (hspeed < DIAG_BOOST_ENGAGE_SPEED)
        {
            if (diagWasCruising) diagLowBoostTicks++;
        }
        else
        {
            diagLowBoostTicks = 0;
        }

        if (diagZeroSpeedTicks > 40)
        {
            diagDbg("obstacle: zero speed " + diagZeroSpeedTicks + "t in BOOSTING");
            diagEngageObstaclePasser();
            return;
        }

        if (diagLowBoostTicks > 20)
        {
            diagDbg("obstacle: below boost speed for " + diagLowBoostTicks + "t");
            diagEngageObstaclePasser();
            return;
        }

        if (diagPostPasserCooldown > 0) diagPostPasserCooldown--;
        else if (diagShouldPassObstacle()) diagEngageObstaclePasser();
    }

    private void diagTickObstaclePassing()
    {
        if (!diagBaritoneLoaded)
        {
            diagState = DiagState.ALIGNING;
            return;
        }

        if (diagPrePasserDelay > 0)
        {
            diagPrePasserDelay--;
            return;
        }

        if (diagPendingPasserGoal != null)
        {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(diagPendingPasserGoal));
            diagDbg("passer: baritone start at " + diagPendingPasserGoal);
            diagPendingPasserGoal = null;
            return;
        }

        diagObstaclePassingTicks++;
        if (diagObstaclePassingTicks > 600)
        {
            diagDbg("passer: timeout at " + mc.player.getBlockPos() + ", returning to align");
            diagResetAfterPass();
            return;
        }

        if (BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().getGoal() == null)
        {
            diagDbg("passer: done at " + mc.player.getBlockPos());
            diagResetAfterPass();
        }
    }

    private void diagResetAfterPass()
    {
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(null);
        diagHoldForwardKey(false);
        diagStuckTimer = 0;
        diagZeroSpeedTicks = 0;
        diagLowBoostTicks = 0;
        diagWasCruising = false;
        diagCollisionTicks = 0;
        diagWasAlignedCollision = false;
        diagObstaclePassingTicks = 0;
        diagSettleTicksRemaining = 5;
        diagState = DiagState.ALIGNING;
    }

    private boolean diagShouldPassObstacle()
    {
        if (!diagBaritoneLoaded) return false;
        int y = (int) mc.player.getY();
        boolean yOutOfBounds = mc.player.isOnGround() && (y < diagCapturedTargetY - 1 || y > diagCapturedTargetY + 4);
        double drift = MoveUtils.distancePointToDirection(mc.player.getPos(), MoveUtils.yawToDirection(diagLockedYaw), diagActivationPos);

        if (drift > 3.0) diagDriftTicks++;
        else diagDriftTicks = 0;

        boolean hardDrift = drift > 7.0;
        boolean sustainedDrift = diagDriftTicks >= 6;

        if (yOutOfBounds || hardDrift || sustainedDrift)
        {
            if (yOutOfBounds) diagDbg("obstacle: y=" + y + " target=" + diagCapturedTargetY);
            if (hardDrift) diagDbg("obstacle: hard drift=" + String.format("%.1f", drift));
            else if (sustainedDrift) diagDbg("obstacle: sustained drift=" + String.format("%.1f", drift) + " t=" + diagDriftTicks);
            return true;
        }
        return false;
    }

    private void diagEngageObstaclePasser()
    {
        if (mc.world == null) return;
        diagDbg("passer: engage from " + diagState + " at " + mc.player.getBlockPos() + " targetY=" + diagCapturedTargetY);
        diagObstaclePassingTicks = 0;
        diagPrePasserDelay = 10;
        diagPendingPasserGoal = null;

        Vec3d unitYawVec = MoveUtils.yawToDirection(diagLockedYaw);
        Vec3d travelVec = mc.player.getPos().subtract(diagActivationPos).multiply(1, 0, 1);
        double parallelDot = travelVec.dotProduct(unitYawVec);
        Vec3d projectedPos = new Vec3d(diagActivationPos.x, 0, diagActivationPos.z).add(unitYawVec.multiply(parallelDot));

        int wallDx = (int) Math.round(unitYawVec.x);
        int wallDz = (int) Math.round(unitYawVec.z);

        BlockPos fallbackGoal = null;
        double currDistance = DIAG_OBSTACLE_DISTANCE;

        while (currDistance <= DIAG_MAX_OBSTACLE_RANGE)
        {
            Vec3d goalPos = MoveUtils.positionInDirection(projectedPos, diagLockedYaw, currDistance);
            BlockPos candidate = new BlockPos((int) Math.floor(goalPos.x), diagCapturedTargetY, (int) Math.floor(goalPos.z));
            currDistance++;

            if (mc.world.getBlockState(candidate).getBlock() == Blocks.VOID_AIR) break;
            if (!mc.world.getBlockState(candidate.down()).isSolidBlock(mc.world, candidate.down())) continue;
            if (!mc.world.getBlockState(candidate).isAir()) continue;
            if (!mc.world.getBlockState(candidate.up()).isAir()) continue;

            BlockPos fwdX = candidate.add(wallDx, 0, 0);
            BlockPos fwdZ = candidate.add(0, 0, wallDz);
            boolean xSolid = mc.world.getBlockState(fwdX).isSolidBlock(mc.world, fwdX);
            boolean zSolid = mc.world.getBlockState(fwdZ).isSolidBlock(mc.world, fwdZ);

            if (xSolid || zSolid)
            {
                diagDbg("passer: notch goal=" + candidate
                    + (xSolid ? " wallX=" + mc.world.getBlockState(fwdX).getBlock() : "")
                    + (zSolid ? " wallZ=" + mc.world.getBlockState(fwdZ).getBlock() : ""));
                diagPendingPasserGoal = candidate;
                diagState = DiagState.OBSTACLE_PASSING;
                return;
            }

            if (fallbackGoal == null) fallbackGoal = candidate;
            if (currDistance > DIAG_OBSTACLE_DISTANCE + 24) break;
        }

        if (fallbackGoal == null) fallbackGoal = new BlockPos(mc.player.getBlockX(), diagCapturedTargetY, mc.player.getBlockZ());
        diagDbg("passer: fallback goal=" + fallbackGoal);
        diagPendingPasserGoal = fallbackGoal;
        diagState = DiagState.OBSTACLE_PASSING;
    }

    private void diagDbg(String msg)
    {
        if (diagDebugMessages.get()) info(msg);
    }

    private double diagHorizontalSpeedBps()
    {
        if (diagLastPos == null) return 0.0;
        return mc.player.getPos().subtract(diagLastPos).multiply(20, 0, 20).length();
    }

    private void diagHoldForwardKey(boolean hold)
    {
        mc.options.forwardKey.setPressed(hold);
        Input.setKeyState(mc.options.forwardKey, hold);
    }

    private void diagReleaseForwardKey()
    {
        diagHoldForwardKey(false);
    }

    private static float nearestDiagonal(float yaw)
    {
        float n = ((yaw % 360) + 360) % 360;
        return (float) ((Math.round((n - 45) / 90.0) * 90 + 45 + 360) % 360);
    }
}
