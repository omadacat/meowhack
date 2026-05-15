package cat.omada.meowhack;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import cat.omada.meowhack.license.LicenseChecker;
import cat.omada.meowhack.modules.combat.BetterCrystal;
import cat.omada.meowhack.modules.misc.AutoMeow;
import cat.omada.meowhack.modules.misc.AutoPawjob;
import cat.omada.meowhack.modules.movement.ElytraBounce;
import cat.omada.meowhack.modules.player.InventoryFix;
import cat.omada.meowhack.modules.player.Printer;
import cat.omada.meowhack.modules.player.autoDoor;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class Kitty extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Initializing your GNU/Hacks");

        LicenseChecker.run();

        //Combat
        Modules.get().add(new BetterCrystal());
        //Player
        Modules.get().add(new autoDoor());
        Modules.get().add(new InventoryFix());
        Modules.get().add(new Printer());       
        //Movement
        Modules.get().add(new ElytraBounce());
        //Render
        //World
        //Misc
        Modules.get().add(new AutoPawjob());
        Modules.get().add(new AutoMeow());
    }

    @Override
    public String getPackage() {
        return "cat.omada.meowhack";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("omadacat", "meowhack");
    }
}
