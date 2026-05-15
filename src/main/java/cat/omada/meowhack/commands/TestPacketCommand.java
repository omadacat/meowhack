package cat.omada.meowhack.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.block.entity.JigsawBlockEntity;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.block.enums.StructureBlockMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.CookieResponseC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.book.RecipeBookType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.profiler.log.DebugSampleType;
import net.minecraft.world.Difficulty;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TestPacketCommand extends Command {

    public TestPacketCommand() {
        super("testpacket", "Send arbitrary serverbound packets with custom data for server pentesting.");
    }

    private boolean send(Packet<?> packet) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) {
            error("Not connected to a server!");
            return false;
        }
        mc.getNetworkHandler().sendPacket(packet);
        return true;
    }

    private MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    private String[] args(String input) {
        return input.trim().split("\\s+");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        // ==================== MOVEMENT ====================

        registerPacket(builder, "move_pos", "<x> <y> <z> <onGround> <horizontalCollision>", input -> {
            String[] a = args(input);
            if (a.length < 5) throw new IllegalArgumentException("Need 5 args: x y z onGround horizontalCollision");
            send(new PlayerMoveC2SPacket.PositionAndOnGround(
                Double.parseDouble(a[0]), Double.parseDouble(a[1]), Double.parseDouble(a[2]),
                Boolean.parseBoolean(a[3]), Boolean.parseBoolean(a[4])
            ));
            info("Sent MovePlayerPos (%.2f, %.2f, %.2f)", Double.parseDouble(a[0]), Double.parseDouble(a[1]), Double.parseDouble(a[2]));
        });

        registerPacket(builder, "move_pos_rot", "<x> <y> <z> <yaw> <pitch> <onGround> <horizontalCollision>", input -> {
            String[] a = args(input);
            if (a.length < 7) throw new IllegalArgumentException("Need 7 args: x y z yaw pitch onGround horizontalCollision");
            send(new PlayerMoveC2SPacket.Full(
                Double.parseDouble(a[0]), Double.parseDouble(a[1]), Double.parseDouble(a[2]),
                Float.parseFloat(a[3]), Float.parseFloat(a[4]),
                Boolean.parseBoolean(a[5]), Boolean.parseBoolean(a[6])
            ));
            info("Sent MovePlayerPosRot (%.2f, %.2f, %.2f) yaw=%.1f pitch=%.1f", Double.parseDouble(a[0]), Double.parseDouble(a[1]), Double.parseDouble(a[2]), Float.parseFloat(a[3]), Float.parseFloat(a[4]));
        });

        registerPacket(builder, "move_rot", "<yaw> <pitch> <onGround> <horizontalCollision>", input -> {
            String[] a = args(input);
            if (a.length < 4) throw new IllegalArgumentException("Need 4 args: yaw pitch onGround horizontalCollision");
            send(new PlayerMoveC2SPacket.LookAndOnGround(
                Float.parseFloat(a[0]), Float.parseFloat(a[1]),
                Boolean.parseBoolean(a[2]), Boolean.parseBoolean(a[3])
            ));
            info("Sent MovePlayerRot yaw=%.1f pitch=%.1f", Float.parseFloat(a[0]), Float.parseFloat(a[1]));
        });

        registerPacket(builder, "move_status", "<onGround> <horizontalCollision>", input -> {
            String[] a = args(input);
            if (a.length < 2) throw new IllegalArgumentException("Need 2 args: onGround horizontalCollision");
            send(new PlayerMoveC2SPacket.OnGroundOnly(
                Boolean.parseBoolean(a[0]), Boolean.parseBoolean(a[1])
            ));
            info("Sent MovePlayerStatus onGround=%s", a[0]);
        });

        registerPacket(builder, "move_vehicle", "<x> <y> <z> <yaw> <pitch> <onGround>", input -> {
            String[] a = args(input);
            if (a.length < 6) throw new IllegalArgumentException("Need 6 args: x y z yaw pitch onGround(bool)");
            Vec3d pos = new Vec3d(Double.parseDouble(a[0]), Double.parseDouble(a[1]), Double.parseDouble(a[2]));
            send(new VehicleMoveC2SPacket(pos, Float.parseFloat(a[3]), Float.parseFloat(a[4]), Boolean.parseBoolean(a[5])));
            info("Sent VehicleMove (%.2f, %.2f, %.2f)", pos.x, pos.y, pos.z);
        });

        registerPacket(builder, "boat_paddle", "<leftPaddle> <rightPaddle>", input -> {
            String[] a = args(input);
            if (a.length < 2) throw new IllegalArgumentException("Need 2 args: leftPaddle(bool) rightPaddle(bool)");
            send(new BoatPaddleStateC2SPacket(Boolean.parseBoolean(a[0]), Boolean.parseBoolean(a[1])));
            info("Sent BoatPaddle left=%s right=%s", a[0], a[1]);
        });

        // ==================== INTERACTION ====================

        registerPacket(builder, "swing", "<hand>", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1 arg: hand (main/off or 0/1)");
            send(new HandSwingC2SPacket(parseHand(a[0])));
            info("Sent Swing hand=%s", a[0]);
        });

        registerPacket(builder, "player_action", "<action> <x> <y> <z> <face> <sequence>", input -> {
            String[] a = args(input);
            if (a.length < 6) throw new IllegalArgumentException("Need 6 args: action(0-6) x y z face(down/up/north/south/west/east) sequence");
            PlayerActionC2SPacket.Action action = parsePlayerAction(a[0]);
            BlockPos pos = new BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]));
            Direction dir = parseDirection(a[4]);
            int seq = Integer.parseInt(a[5]);
            send(new PlayerActionC2SPacket(action, pos, dir, seq));
            info("Sent PlayerAction %s at (%d, %d, %d) face=%s seq=%d", action.name(), pos.getX(), pos.getY(), pos.getZ(), dir.name(), seq);
        });

        registerPacket(builder, "player_command", "<action> [jumpBoost]", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1-2 args: action(0-8) [jumpBoost(0-100)]");
            ClientCommandC2SPacket.Mode mode = parseClientCommandMode(a[0]);
            int jumpBoost = a.length > 1 ? Integer.parseInt(a[1]) : 0;
            send(new ClientCommandC2SPacket(mc().player, mode, jumpBoost));
            info("Sent PlayerCommand %s jumpBoost=%d", mode.name(), jumpBoost);
        });

        registerPacket(builder, "use_item", "<hand> <sequence> <yaw> <pitch>", input -> {
            String[] a = args(input);
            if (a.length < 4) throw new IllegalArgumentException("Need 4 args: hand(main/off) sequence yaw pitch");
            send(new PlayerInteractItemC2SPacket(parseHand(a[0]), Integer.parseInt(a[1]), Float.parseFloat(a[2]), Float.parseFloat(a[3])));
            info("Sent UseItem hand=%s seq=%s", a[0], a[1]);
        });

        registerPacket(builder, "use_item_on", "<hand> <x> <y> <z> <face> <cursorX> <cursorY> <cursorZ> <insideBlock> <sequence>", input -> {
            String[] a = args(input);
            if (a.length < 10) throw new IllegalArgumentException("Need 10 args: hand x y z face cursorX cursorY cursorZ insideBlock sequence");
            Hand hand = parseHand(a[0]);
            BlockPos pos = new BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]));
            Direction dir = parseDirection(a[4]);
            Vec3d cursor = new Vec3d(Double.parseDouble(a[5]), Double.parseDouble(a[6]), Double.parseDouble(a[7]));
            boolean inside = Boolean.parseBoolean(a[8]);
            int seq = Integer.parseInt(a[9]);
            BlockHitResult hitResult = new BlockHitResult(cursor, dir, pos, inside);
            send(new PlayerInteractBlockC2SPacket(hand, hitResult, seq));
            info("Sent UseItemOn at (%d, %d, %d) face=%s", pos.getX(), pos.getY(), pos.getZ(), dir.name());
        });

        registerPacket(builder, "interact_attack", "<entityId> <sneaking>", input -> {
            String[] a = args(input);
            if (a.length < 2) throw new IllegalArgumentException("Need 2 args: entityId sneaking(bool)");
            int entityId = Integer.parseInt(a[0]);
            boolean sneaking = Boolean.parseBoolean(a[1]);
            Entity entity = mc().world != null ? mc().world.getEntityById(entityId) : null;
            if (entity == null) {
                error("Entity %d not found in loaded world! (must be in render distance)", entityId);
                return;
            }
            send(PlayerInteractEntityC2SPacket.attack(entity, sneaking));
            info("Sent InteractAttack entity=%d sneaking=%s", entityId, a[1]);
        });

        registerPacket(builder, "interact_interact", "<entityId> <hand> <sneaking>", input -> {
            String[] a = args(input);
            if (a.length < 3) throw new IllegalArgumentException("Need 3 args: entityId hand(main/off) sneaking(bool)");
            int entityId = Integer.parseInt(a[0]);
            Entity entity = mc().world != null ? mc().world.getEntityById(entityId) : null;
            if (entity == null) {
                error("Entity %d not found in loaded world!", entityId);
                return;
            }
            send(PlayerInteractEntityC2SPacket.interact(entity, Boolean.parseBoolean(a[2]), parseHand(a[1])));
            info("Sent InteractInteract entity=%d hand=%s", entityId, a[1]);
        });

        registerPacket(builder, "interact_at", "<entityId> <hand> <targetX> <targetY> <targetZ> <sneaking>", input -> {
            String[] a = args(input);
            if (a.length < 6) throw new IllegalArgumentException("Need 6 args: entityId hand targetX targetY targetZ sneaking");
            int entityId = Integer.parseInt(a[0]);
            Entity entity = mc().world != null ? mc().world.getEntityById(entityId) : null;
            if (entity == null) {
                error("Entity %d not found in loaded world!", entityId);
                return;
            }
            Vec3d target = new Vec3d(Double.parseDouble(a[2]), Double.parseDouble(a[3]), Double.parseDouble(a[4]));
            send(PlayerInteractEntityC2SPacket.interactAt(entity, Boolean.parseBoolean(a[5]), parseHand(a[1]), target));
            info("Sent InteractAt entity=%d pos=(%.2f, %.2f, %.2f)", entityId, target.x, target.y, target.z);
        });

        // ==================== INVENTORY ====================

        registerPacket(builder, "select_slot", "<slot>", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1 arg: slot (any int, valid=0-8)");
            int slot = Integer.parseInt(a[0]);
            send(new UpdateSelectedSlotC2SPacket(slot));
            info("Sent SelectSlot slot=%d", slot);
        });

        registerPacket(builder, "close_screen", "<windowId>", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1 arg: windowId (int)");
            send(new CloseHandledScreenC2SPacket(Integer.parseInt(a[0])));
            info("Sent CloseScreen windowId=%s", a[0]);
        });

        registerPacket(builder, "button_click", "<windowId> <buttonId>", input -> {
            String[] a = args(input);
            if (a.length < 2) throw new IllegalArgumentException("Need 2 args: windowId buttonId");
            send(new ButtonClickC2SPacket(Integer.parseInt(a[0]), Integer.parseInt(a[1])));
            info("Sent ButtonClick window=%s button=%s", a[0], a[1]);
        });

        registerPacket(builder, "click_slot", "<syncId> <revision> <slot> <button> <actionType>", input -> {
            String[] a = args(input);
            if (a.length < 5) throw new IllegalArgumentException("Need 5 args: syncId revision slot button actionType(pickup/quick_move/swap/clone/throw/quick_craft/pickup_all or 0-6)");
            SlotActionType actionType = parseSlotActionType(a[4]);
            send(new ClickSlotC2SPacket(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]), actionType, ItemStack.EMPTY, new Int2ObjectOpenHashMap<>()));
            info("Sent ClickSlot syncId=%s slot=%s button=%s action=%s", a[0], a[2], a[3], actionType.name());
        });

        registerPacket(builder, "creative_slot", "<slot>", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1 arg: slot(int) (sends empty ItemStack, use for clearing slots)");
            send(new CreativeInventoryActionC2SPacket(Integer.parseInt(a[0]), ItemStack.EMPTY));
            info("Sent CreativeSlot slot=%s (empty stack)", a[0]);
        });

        registerPacket(builder, "rename_item", "<name>", input -> {
            send(new RenameItemC2SPacket(input));
            info("Sent RenameItem name='%s'", input);
        });

        registerPacket(builder, "select_trade", "<tradeIndex>", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1 arg: tradeIndex (int)");
            send(new SelectMerchantTradeC2SPacket(Integer.parseInt(a[0])));
            info("Sent SelectTrade index=%s", a[0]);
        });

        registerPacket(builder, "slot_state", "<slotId> <windowId> <newState>", input -> {
            String[] a = args(input);
            if (a.length < 3) throw new IllegalArgumentException("Need 3 args: slotId windowId newState(bool)");
            send(new SlotChangedStateC2SPacket(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Boolean.parseBoolean(a[2])));
            info("Sent SlotState slot=%s window=%s state=%s", a[0], a[1], a[2]);
        });

        registerPacket(builder, "bundle_item", "<slotId> <selectedItemIndex>", input -> {
            String[] a = args(input);
            if (a.length < 2) throw new IllegalArgumentException("Need 2 args: slotId selectedItemIndex");
            send(new BundleItemSelectedC2SPacket(Integer.parseInt(a[0]), Integer.parseInt(a[1])));
            info("Sent BundleItemSelected slot=%s index=%s", a[0], a[1]);
        });

        // ==================== WORLD ====================

        registerPacket(builder, "update_sign", "<x> <y> <z> <isFront> <line1|line2|line3|line4>", input -> {
            String[] a = input.split("\\s+", 5);
            if (a.length < 5) throw new IllegalArgumentException("Need 5 args: x y z isFront text (lines separated by |)");
            BlockPos pos = new BlockPos(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]));
            boolean front = Boolean.parseBoolean(a[3]);
            String[] lines = a[4].split("\\|", -1);
            String[] padded = new String[4];
            for (int i = 0; i < 4; i++) padded[i] = i < lines.length ? lines[i] : "";
            send(new UpdateSignC2SPacket(pos, front, padded[0], padded[1], padded[2], padded[3]));
            info("Sent UpdateSign at (%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
        });

        registerPacket(builder, "command_block", "<x> <y> <z> <command> <mode> <flags>", input -> {
            String[] a = input.split("\\s+", 6);
            if (a.length < 6) throw new IllegalArgumentException("Need 6 args: x y z command mode(sequence/auto/redstone) flags(int: 0x01=trackOutput 0x02=conditional 0x04=automatic)");
            BlockPos pos = new BlockPos(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]));
            String command = a[3];
            CommandBlockBlockEntity.Type mode = parseCommandBlockType(a[4]);
            int flags = Integer.parseInt(a[5]);
            boolean trackOutput = (flags & 0x01) != 0;
            boolean conditional = (flags & 0x02) != 0;
            boolean automatic = (flags & 0x04) != 0;
            send(new UpdateCommandBlockC2SPacket(pos, command, mode, trackOutput, conditional, automatic));
            info("Sent CommandBlock at (%d, %d, %d) cmd='%s' mode=%s flags=%d", pos.getX(), pos.getY(), pos.getZ(), command, mode.name(), flags);
        });

        registerPacket(builder, "command_minecart", "<entityId> <command> <trackOutput>", input -> {
            String[] a = input.split("\\s+", 3);
            if (a.length < 3) throw new IllegalArgumentException("Need 3 args: entityId command trackOutput(bool)");
            send(new UpdateCommandBlockMinecartC2SPacket(Integer.parseInt(a[0]), a[1], Boolean.parseBoolean(a[2])));
            info("Sent CommandMinecart entity=%s cmd='%s'", a[0], a[1]);
        });

        registerPacket(builder, "jigsaw_generate", "<x> <y> <z> <levels> <keepJigsaws>", input -> {
            String[] a = args(input);
            if (a.length < 5) throw new IllegalArgumentException("Need 5 args: x y z levels(int) keepJigsaws(bool)");
            BlockPos pos = new BlockPos(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]));
            send(new JigsawGeneratingC2SPacket(pos, Integer.parseInt(a[3]), Boolean.parseBoolean(a[4])));
            info("Sent JigsawGenerate at (%d, %d, %d) levels=%s", pos.getX(), pos.getY(), pos.getZ(), a[3]);
        });

        registerPacket(builder, "update_jigsaw", "<x> <y> <z> <name> <target> <pool> <finalState> <joint> <selectionPriority> <placementPriority>", input -> {
            String[] a = args(input);
            if (a.length < 10) throw new IllegalArgumentException("Need 10 args: x y z name target pool finalState joint(rollable/aligned) selectionPriority placementPriority");
            BlockPos pos = new BlockPos(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]));
            JigsawBlockEntity.Joint joint = a[7].equalsIgnoreCase("aligned") ? JigsawBlockEntity.Joint.ALIGNED : JigsawBlockEntity.Joint.ROLLABLE;
            send(new UpdateJigsawC2SPacket(pos, Identifier.of(a[3]), Identifier.of(a[4]), Identifier.of(a[5]), a[6], joint, Integer.parseInt(a[8]), Integer.parseInt(a[9])));
            info("Sent UpdateJigsaw at (%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
        });

        registerPacket(builder, "update_structure_block", "<x> <y> <z> <action> <mode> <name> <offX> <offY> <offZ> <sizeX> <sizeY> <sizeZ> <mirror> <rotation> <metadata> <integrity> <seed> <flags>", input -> {
            String[] a = input.split("\\s+", 18);
            if (a.length < 18) throw new IllegalArgumentException("Need 18 args: x y z action(update_data/save_area/load_area/scan_area) mode(save/load/corner/data) name offX offY offZ sizeX sizeY sizeZ mirror(none/left_right/front_back) rotation(none/cw90/cw180/ccw90) metadata integrity(0-1) seed(long) flags(int: 0x01=ignoreEntities 0x02=showAir 0x04=showBoundingBox)");
            BlockPos pos = new BlockPos(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]));
            StructureBlockBlockEntity.Action action = parseStructureAction(a[3]);
            StructureBlockMode mode = parseStructureMode(a[4]);
            BlockPos offset = new BlockPos(Integer.parseInt(a[6]), Integer.parseInt(a[7]), Integer.parseInt(a[8]));
            Vec3i size = new Vec3i(Integer.parseInt(a[9]), Integer.parseInt(a[10]), Integer.parseInt(a[11]));
            BlockMirror mirror = parseMirror(a[12]);
            BlockRotation rotation = parseRotation(a[13]);
            float integrity = Float.parseFloat(a[15]);
            long seed = Long.parseLong(a[16]);
            int flags = Integer.parseInt(a[17]);
            send(new UpdateStructureBlockC2SPacket(pos, action, mode, a[5], offset, size, mirror, rotation, a[14], (flags & 0x01) != 0, (flags & 0x02) != 0, (flags & 0x04) != 0, integrity, seed));
            info("Sent UpdateStructureBlock at (%d, %d, %d) mode=%s", pos.getX(), pos.getY(), pos.getZ(), mode.name());
        });

        // ==================== PLAYER ====================

        registerPacket(builder, "abilities", "<flying>", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1 arg: flying(bool)");
            PlayerAbilities abilities = mc().player.getAbilities();
            abilities.flying = Boolean.parseBoolean(a[0]);
            send(new UpdatePlayerAbilitiesC2SPacket(abilities));
            info("Sent Abilities flying=%s", a[0]);
        });

        registerPacket(builder, "teleport_confirm", "<teleportId>", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1 arg: teleportId(int)");
            send(new TeleportConfirmC2SPacket(Integer.parseInt(a[0])));
            info("Sent TeleportConfirm id=%s", a[0]);
        });

        registerPacket(builder, "keep_alive", "<id>", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1 arg: id(long)");
            send(new KeepAliveC2SPacket(Long.parseLong(a[0])));
            info("Sent KeepAlive id=%s", a[0]);
        });

        registerPacket(builder, "pong", "<id>", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1 arg: id(int)");
            send(new CommonPongC2SPacket(Integer.parseInt(a[0])));
            info("Sent Pong id=%s", a[0]);
        });

        registerPacket(builder, "difficulty", "<difficulty>", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1 arg: difficulty(peaceful/easy/normal/hard or 0-3)");
            send(new UpdateDifficultyC2SPacket(parseDifficulty(a[0])));
            info("Sent Difficulty %s", a[0]);
        });

        registerPacket(builder, "lock_difficulty", "<locked>", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1 arg: locked(bool)");
            send(new UpdateDifficultyLockC2SPacket(Boolean.parseBoolean(a[0])));
            info("Sent LockDifficulty locked=%s", a[0]);
        });

        registerPacket(builder, "client_status", "<action>", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1 arg: action(respawn/stats or 0/1)");
            send(new ClientStatusC2SPacket(parseClientStatus(a[0])));
            info("Sent ClientStatus %s", a[0]);
        });

        registerPacket(builder, "player_input", "<forward> <backward> <left> <right> <jump> <sneak> <sprint>", input -> {
            String[] a = args(input);
            if (a.length < 7) throw new IllegalArgumentException("Need 7 args: forward backward left right jump sneak sprint (all bool)");
            send(new PlayerInputC2SPacket(new PlayerInput(
                Boolean.parseBoolean(a[0]), Boolean.parseBoolean(a[1]), Boolean.parseBoolean(a[2]),
                Boolean.parseBoolean(a[3]), Boolean.parseBoolean(a[4]), Boolean.parseBoolean(a[5]),
                Boolean.parseBoolean(a[6])
            )));
            info("Sent PlayerInput fwd=%s back=%s left=%s right=%s jump=%s sneak=%s sprint=%s", a[0], a[1], a[2], a[3], a[4], a[5], a[6]);
        });

        builder.then(literal("player_loaded")
            .executes(ctx -> {
                send(new PlayerLoadedC2SPacket());
                info("Sent PlayerLoaded");
                return SINGLE_SUCCESS;
            })
        );

        builder.then(literal("client_tick_end")
            .executes(ctx -> {
                send(ClientTickEndC2SPacket.INSTANCE);
                info("Sent ClientTickEnd");
                return SINGLE_SUCCESS;
            })
        );

        // ==================== CHAT & COMMANDS ====================

        registerPacket(builder, "chat_command", "<command>", input -> {
            send(new CommandExecutionC2SPacket(input));
            info("Sent ChatCommand '%s'", input);
        });

        registerPacket(builder, "chat_ack", "<messageCount>", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1 arg: messageCount(int)");
            send(new MessageAcknowledgmentC2SPacket(Integer.parseInt(a[0])));
            info("Sent ChatAck count=%s", a[0]);
        });

        registerPacket(builder, "command_suggestion", "<transactionId> <text>", input -> {
            String[] a = input.split("\\s+", 2);
            if (a.length < 2) throw new IllegalArgumentException("Need 2 args: transactionId text");
            send(new RequestCommandCompletionsC2SPacket(Integer.parseInt(a[0]), a[1]));
            info("Sent CommandSuggestion id=%s text='%s'", a[0], a[1]);
        });

        // ==================== RECIPES & ADVANCEMENTS ====================

        registerPacket(builder, "craft_request", "<syncId> <recipeId> <craftAll>", input -> {
            String[] a = args(input);
            if (a.length < 3) throw new IllegalArgumentException("Need 3 args: syncId(int) recipeId(int) craftAll(bool)");
            send(new CraftRequestC2SPacket(Integer.parseInt(a[0]), new NetworkRecipeId(Integer.parseInt(a[1])), Boolean.parseBoolean(a[2])));
            info("Sent CraftRequest syncId=%s recipe=%s craftAll=%s", a[0], a[1], a[2]);
        });

        registerPacket(builder, "recipe_book_data", "<recipeId>", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1 arg: recipeId(int)");
            send(new RecipeBookDataC2SPacket(new NetworkRecipeId(Integer.parseInt(a[0]))));
            info("Sent RecipeBookData recipe=%s", a[0]);
        });

        registerPacket(builder, "recipe_category_options", "<category> <guiOpen> <filteringCraftable>", input -> {
            String[] a = args(input);
            if (a.length < 3) throw new IllegalArgumentException("Need 3 args: category(crafting/furnace/blast_furnace/smoker or 0-3) guiOpen(bool) filteringCraftable(bool)");
            send(new RecipeCategoryOptionsC2SPacket(parseRecipeBookType(a[0]), Boolean.parseBoolean(a[1]), Boolean.parseBoolean(a[2])));
            info("Sent RecipeCategoryOptions category=%s open=%s filter=%s", a[0], a[1], a[2]);
        });

        registerPacket(builder, "advancement_tab", "<action> [tabId]", input -> {
            String[] a = input.split("\\s+", 2);
            if (a.length < 1) throw new IllegalArgumentException("Need 1-2 args: action(open/close or 0/1) [tabId (identifier, only for open)]");
            if (a[0].equalsIgnoreCase("close") || a[0].equals("1")) {
                send(AdvancementTabC2SPacket.close());
                info("Sent AdvancementTab close");
            } else {
                if (a.length < 2) throw new IllegalArgumentException("Need tabId for open action");
                send(new AdvancementTabC2SPacket(AdvancementTabC2SPacket.Action.OPENED_TAB, Identifier.of(a[1])));
                info("Sent AdvancementTab open tab=%s", a[1]);
            }
        });

        // ==================== MISC ====================

        registerPacket(builder, "chunk_batch", "<chunksPerTick>", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1 arg: chunksPerTick(float)");
            send(new AcknowledgeChunksC2SPacket(Float.parseFloat(a[0])));
            info("Sent ChunkBatch chunksPerTick=%s", a[0]);
        });

        registerPacket(builder, "spectator_tp", "<uuid>", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1 arg: uuid (player/entity UUID)");
            send(new SpectatorTeleportC2SPacket(UUID.fromString(a[0])));
            info("Sent SpectatorTp uuid=%s", a[0]);
        });

        registerPacket(builder, "pick_block", "<x> <y> <z> <includeData>", input -> {
            String[] a = args(input);
            if (a.length < 4) throw new IllegalArgumentException("Need 4 args: x y z includeData(bool)");
            BlockPos pos = new BlockPos(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]));
            send(new PickItemFromBlockC2SPacket(pos, Boolean.parseBoolean(a[3])));
            info("Sent PickBlock at (%d, %d, %d) includeData=%s", pos.getX(), pos.getY(), pos.getZ(), a[3]);
        });

        registerPacket(builder, "pick_entity", "<entityId> <includeData>", input -> {
            String[] a = args(input);
            if (a.length < 2) throw new IllegalArgumentException("Need 2 args: entityId includeData(bool)");
            send(new PickItemFromEntityC2SPacket(Integer.parseInt(a[0]), Boolean.parseBoolean(a[1])));
            info("Sent PickEntity entity=%s includeData=%s", a[0], a[1]);
        });

        registerPacket(builder, "query_block_nbt", "<transactionId> <x> <y> <z>", input -> {
            String[] a = args(input);
            if (a.length < 4) throw new IllegalArgumentException("Need 4 args: transactionId x y z");
            BlockPos pos = new BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]));
            send(new QueryBlockNbtC2SPacket(Integer.parseInt(a[0]), pos));
            info("Sent QueryBlockNbt id=%s at (%d, %d, %d)", a[0], pos.getX(), pos.getY(), pos.getZ());
        });

        registerPacket(builder, "query_entity_nbt", "<transactionId> <entityId>", input -> {
            String[] a = args(input);
            if (a.length < 2) throw new IllegalArgumentException("Need 2 args: transactionId entityId");
            send(new QueryEntityNbtC2SPacket(Integer.parseInt(a[0]), Integer.parseInt(a[1])));
            info("Sent QueryEntityNbt id=%s entity=%s", a[0], a[1]);
        });

        registerPacket(builder, "book_update", "<slot> <title_or_none> <page1|page2|...>", input -> {
            String[] a = input.split("\\s+", 3);
            if (a.length < 3) throw new IllegalArgumentException("Need 3 args: slot(int) title(string or 'none') pages(separated by |)");
            int slot = Integer.parseInt(a[0]);
            Optional<String> title = a[1].equalsIgnoreCase("none") ? Optional.empty() : Optional.of(a[1]);
            List<String> pages = Arrays.asList(a[2].split("\\|", -1));
            send(new BookUpdateC2SPacket(slot, pages, title));
            info("Sent BookUpdate slot=%d title=%s pages=%d", slot, title.orElse("none"), pages.size());
        });

        registerPacket(builder, "resource_pack", "<uuid> <status>", input -> {
            String[] a = args(input);
            if (a.length < 2) throw new IllegalArgumentException("Need 2 args: uuid status(loaded/declined/failed/accepted/downloaded/invalid_url/failed_reload/discarded or 0-7)");
            send(new ResourcePackStatusC2SPacket(UUID.fromString(a[0]), parseResourcePackStatus(a[1])));
            info("Sent ResourcePack uuid=%s status=%s", a[0], a[1]);
        });

        registerPacket(builder, "cookie_response", "<key> <data>", input -> {
            String[] a = input.split("\\s+", 2);
            if (a.length < 2) throw new IllegalArgumentException("Need 2 args: key(identifier) data(string, will be UTF-8 encoded)");
            send(new CookieResponseC2SPacket(Identifier.of(a[0]), a[1].getBytes(StandardCharsets.UTF_8)));
            info("Sent CookieResponse key=%s dataLen=%d", a[0], a[1].length());
        });

        registerPacket(builder, "debug_sample", "<sampleType>", input -> {
            String[] a = args(input);
            if (a.length < 1) throw new IllegalArgumentException("Need 1 arg: sampleType(tick_time or 0)");
            send(new DebugSampleSubscriptionC2SPacket(DebugSampleType.TICK_TIME));
            info("Sent DebugSample type=TICK_TIME");
        });

        builder.then(literal("acknowledge_reconfig")
            .executes(ctx -> {
                send(AcknowledgeReconfigurationC2SPacket.INSTANCE);
                info("Sent AcknowledgeReconfiguration");
                return SINGLE_SUCCESS;
            })
        );

        registerPacket(builder, "update_beacon", "<primaryEffectId> <secondaryEffectId>", input -> {
            String[] a = args(input);
            if (a.length < 2) throw new IllegalArgumentException("Need 2 args: primaryEffectId(int, -1 for none) secondaryEffectId(int, -1 for none). Effect IDs from registry.");
            Optional<RegistryEntry<StatusEffect>> primary = parseStatusEffect(Integer.parseInt(a[0]));
            Optional<RegistryEntry<StatusEffect>> secondary = parseStatusEffect(Integer.parseInt(a[1]));
            send(new UpdateBeaconC2SPacket(primary, secondary));
            info("Sent UpdateBeacon primary=%s secondary=%s", a[0], a[1]);
        });
    }

    // === Helper: register a packet subcommand with usage and handler ===
    private void registerPacket(LiteralArgumentBuilder<CommandSource> builder, String name, String usage, PacketHandler handler) {
        builder.then(literal(name)
            .executes(ctx -> {
                info("Usage: .testpacket %s %s", name, usage);
                return SINGLE_SUCCESS;
            })
            .then(argument("args", StringArgumentType.greedyString())
                .suggests((ctx, suggestionsBuilder) -> {
                    suggestionsBuilder.suggest(usage);
                    return suggestionsBuilder.buildFuture();
                })
                .executes(ctx -> {
                    try {
                        handler.handle(StringArgumentType.getString(ctx, "args"));
                    } catch (NumberFormatException e) {
                        error("Invalid number: %s", e.getMessage());
                    } catch (IllegalArgumentException e) {
                        error("%s", e.getMessage());
                    } catch (Exception e) {
                        error("Error: %s", e.getMessage());
                    }
                    return SINGLE_SUCCESS;
                })
            )
        );
    }

    @FunctionalInterface
    private interface PacketHandler {
        void handle(String input) throws Exception;
    }

    // === Enum Parsers ===

    private Hand parseHand(String s) {
        return switch (s.toLowerCase()) {
            case "main", "0" -> Hand.MAIN_HAND;
            case "off", "1" -> Hand.OFF_HAND;
            default -> throw new IllegalArgumentException("Unknown hand: " + s + " (use main/off or 0/1)");
        };
    }

    private Direction parseDirection(String s) {
        return switch (s.toLowerCase()) {
            case "down", "0" -> Direction.DOWN;
            case "up", "1" -> Direction.UP;
            case "north", "2" -> Direction.NORTH;
            case "south", "3" -> Direction.SOUTH;
            case "west", "4" -> Direction.WEST;
            case "east", "5" -> Direction.EAST;
            default -> throw new IllegalArgumentException("Unknown direction: " + s + " (use down/up/north/south/west/east or 0-5)");
        };
    }

    private Difficulty parseDifficulty(String s) {
        return switch (s.toLowerCase()) {
            case "peaceful", "0" -> Difficulty.PEACEFUL;
            case "easy", "1" -> Difficulty.EASY;
            case "normal", "2" -> Difficulty.NORMAL;
            case "hard", "3" -> Difficulty.HARD;
            default -> throw new IllegalArgumentException("Unknown difficulty: " + s + " (use peaceful/easy/normal/hard or 0-3)");
        };
    }

    private PlayerActionC2SPacket.Action parsePlayerAction(String s) {
        return switch (s.toLowerCase()) {
            case "start_destroy", "0" -> PlayerActionC2SPacket.Action.START_DESTROY_BLOCK;
            case "abort_destroy", "1" -> PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK;
            case "stop_destroy", "2" -> PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK;
            case "drop_all", "3" -> PlayerActionC2SPacket.Action.DROP_ALL_ITEMS;
            case "drop_item", "4" -> PlayerActionC2SPacket.Action.DROP_ITEM;
            case "release_use", "5" -> PlayerActionC2SPacket.Action.RELEASE_USE_ITEM;
            case "swap_hands", "6" -> PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND;
            default -> throw new IllegalArgumentException("Unknown action: " + s + " (use start_destroy/abort_destroy/stop_destroy/drop_all/drop_item/release_use/swap_hands or 0-6)");
        };
    }

    private ClientCommandC2SPacket.Mode parseClientCommandMode(String s) {
        return switch (s.toLowerCase()) {
            case "sneak", "press_shift", "0" -> ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY;
            case "unsneak", "release_shift", "1" -> ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY;
            case "stop_sleeping", "2" -> ClientCommandC2SPacket.Mode.STOP_SLEEPING;
            case "sprint", "start_sprint", "3" -> ClientCommandC2SPacket.Mode.START_SPRINTING;
            case "unsprint", "stop_sprint", "4" -> ClientCommandC2SPacket.Mode.STOP_SPRINTING;
            case "start_jump", "5" -> ClientCommandC2SPacket.Mode.START_RIDING_JUMP;
            case "stop_jump", "6" -> ClientCommandC2SPacket.Mode.STOP_RIDING_JUMP;
            case "open_inventory", "7" -> ClientCommandC2SPacket.Mode.OPEN_INVENTORY;
            case "elytra", "start_fall_flying", "8" -> ClientCommandC2SPacket.Mode.START_FALL_FLYING;
            default -> throw new IllegalArgumentException("Unknown mode: " + s + " (use sneak/unsneak/stop_sleeping/sprint/unsprint/start_jump/stop_jump/open_inventory/elytra or 0-8)");
        };
    }

    private ClientStatusC2SPacket.Mode parseClientStatus(String s) {
        return switch (s.toLowerCase()) {
            case "respawn", "0" -> ClientStatusC2SPacket.Mode.PERFORM_RESPAWN;
            case "stats", "1" -> ClientStatusC2SPacket.Mode.REQUEST_STATS;
            default -> throw new IllegalArgumentException("Unknown status: " + s + " (use respawn/stats or 0/1)");
        };
    }

    private CommandBlockBlockEntity.Type parseCommandBlockType(String s) {
        return switch (s.toLowerCase()) {
            case "sequence", "chain", "0" -> CommandBlockBlockEntity.Type.SEQUENCE;
            case "auto", "repeating", "1" -> CommandBlockBlockEntity.Type.AUTO;
            case "redstone", "impulse", "2" -> CommandBlockBlockEntity.Type.REDSTONE;
            default -> throw new IllegalArgumentException("Unknown type: " + s + " (use sequence/auto/redstone or 0-2)");
        };
    }

    private SlotActionType parseSlotActionType(String s) {
        return switch (s.toLowerCase()) {
            case "pickup", "0" -> SlotActionType.PICKUP;
            case "quick_move", "1" -> SlotActionType.QUICK_MOVE;
            case "swap", "2" -> SlotActionType.SWAP;
            case "clone", "3" -> SlotActionType.CLONE;
            case "throw", "4" -> SlotActionType.THROW;
            case "quick_craft", "5" -> SlotActionType.QUICK_CRAFT;
            case "pickup_all", "6" -> SlotActionType.PICKUP_ALL;
            default -> throw new IllegalArgumentException("Unknown action type: " + s + " (use pickup/quick_move/swap/clone/throw/quick_craft/pickup_all or 0-6)");
        };
    }

    private RecipeBookType parseRecipeBookType(String s) {
        return switch (s.toLowerCase()) {
            case "crafting", "0" -> RecipeBookType.CRAFTING;
            case "furnace", "1" -> RecipeBookType.FURNACE;
            case "blast_furnace", "2" -> RecipeBookType.BLAST_FURNACE;
            case "smoker", "3" -> RecipeBookType.SMOKER;
            default -> throw new IllegalArgumentException("Unknown recipe book type: " + s + " (use crafting/furnace/blast_furnace/smoker or 0-3)");
        };
    }

    private ResourcePackStatusC2SPacket.Status parseResourcePackStatus(String s) {
        return switch (s.toLowerCase()) {
            case "loaded", "0" -> ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED;
            case "declined", "1" -> ResourcePackStatusC2SPacket.Status.DECLINED;
            case "failed", "2" -> ResourcePackStatusC2SPacket.Status.FAILED_DOWNLOAD;
            case "accepted", "3" -> ResourcePackStatusC2SPacket.Status.ACCEPTED;
            case "downloaded", "4" -> ResourcePackStatusC2SPacket.Status.DOWNLOADED;
            case "invalid_url", "5" -> ResourcePackStatusC2SPacket.Status.INVALID_URL;
            case "failed_reload", "6" -> ResourcePackStatusC2SPacket.Status.FAILED_RELOAD;
            case "discarded", "7" -> ResourcePackStatusC2SPacket.Status.DISCARDED;
            default -> throw new IllegalArgumentException("Unknown status: " + s + " (use loaded/declined/failed/accepted/downloaded/invalid_url/failed_reload/discarded or 0-7)");
        };
    }

    private StructureBlockBlockEntity.Action parseStructureAction(String s) {
        return switch (s.toLowerCase()) {
            case "update_data", "0" -> StructureBlockBlockEntity.Action.UPDATE_DATA;
            case "save_area", "1" -> StructureBlockBlockEntity.Action.SAVE_AREA;
            case "load_area", "2" -> StructureBlockBlockEntity.Action.LOAD_AREA;
            case "scan_area", "3" -> StructureBlockBlockEntity.Action.SCAN_AREA;
            default -> throw new IllegalArgumentException("Unknown action: " + s + " (use update_data/save_area/load_area/scan_area or 0-3)");
        };
    }

    private StructureBlockMode parseStructureMode(String s) {
        return switch (s.toLowerCase()) {
            case "save", "0" -> StructureBlockMode.SAVE;
            case "load", "1" -> StructureBlockMode.LOAD;
            case "corner", "2" -> StructureBlockMode.CORNER;
            case "data", "3" -> StructureBlockMode.DATA;
            default -> throw new IllegalArgumentException("Unknown mode: " + s + " (use save/load/corner/data or 0-3)");
        };
    }

    private BlockMirror parseMirror(String s) {
        return switch (s.toLowerCase()) {
            case "none", "0" -> BlockMirror.NONE;
            case "left_right", "1" -> BlockMirror.LEFT_RIGHT;
            case "front_back", "2" -> BlockMirror.FRONT_BACK;
            default -> throw new IllegalArgumentException("Unknown mirror: " + s + " (use none/left_right/front_back or 0-2)");
        };
    }

    private BlockRotation parseRotation(String s) {
        return switch (s.toLowerCase()) {
            case "none", "0" -> BlockRotation.NONE;
            case "cw90", "clockwise_90", "1" -> BlockRotation.CLOCKWISE_90;
            case "cw180", "clockwise_180", "2" -> BlockRotation.CLOCKWISE_180;
            case "ccw90", "counterclockwise_90", "3" -> BlockRotation.COUNTERCLOCKWISE_90;
            default -> throw new IllegalArgumentException("Unknown rotation: " + s + " (use none/cw90/cw180/ccw90 or 0-3)");
        };
    }

    @SuppressWarnings("unchecked")
    private Optional<RegistryEntry<StatusEffect>> parseStatusEffect(int id) {
        if (id < 0) return Optional.empty();
        return (Optional<RegistryEntry<StatusEffect>>) (Optional<?>) Registries.STATUS_EFFECT.getEntry(id);
    }
}
