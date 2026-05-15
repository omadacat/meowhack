package cat.omada.meowhack.modules.misc;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

/* Taken from faghax for meow */
public class AutoMeow extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private long lastResponseTime = 0;

    private final Setting<String> triggerTexts = sgGeneral.add(new StringSetting.Builder()
        .name("triggers")
        .description("Comma-separated list of texts that trigger a response")
        .defaultValue(":3,meow")
        .build()
    );

    private final Setting<String> respondText = sgGeneral.add(new StringSetting.Builder()
        .name("response")
        .description("Text to respond with")
        .defaultValue("meow")
        .build()
    );

    private final Setting<Integer> delaySeconds = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between responses in seconds")
        .defaultValue(3)
        .min(0)
        .sliderMin(0)
        .sliderMax(300)
        .build()
    );

    public AutoMeow() {
        super(Categories.Misc, "auto-meow", "meow meow meow. mrrow meow.");
    }

    private String extractContent(String message, String after) {
        try {
            int index = message.indexOf(after);
            if (index == -1) return "";
            return message.substring(index + after.length()).trim().toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        if (mc.player == null) return;

        String message = event.getMessage().getString();
        if (message.contains(mc.player.getGameProfile().getName())) return;
        if (System.currentTimeMillis() - lastResponseTime < delaySeconds.get() * 1000L) return;

        // get trigger words
        String[] triggers = triggerTexts.get().split(",");
        for (int i = 0; i < triggers.length; i++) {
            triggers[i] = triggers[i].trim().toLowerCase();
        }

        String content = null;
        String responsePrefix = "";

        if (message.contains("<") && message.contains(">")) {
            content = extractContent(message, ">");
        }

        if (content != null) {
            content = content.toLowerCase();
            for (String trigger : triggers) {
                if (!trigger.isEmpty() && content.contains(trigger)) {
                    ChatUtils.sendPlayerMsg(responsePrefix + respondText.get());
                    lastResponseTime = System.currentTimeMillis();
                    return;
                }
            }
        }
    }
}
