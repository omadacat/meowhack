/*
 * Check logic originally from fabric-license-check by Flowey (GPL-3.0).
 * https://github.com/FloweyTheFlower/fabric-license-check
 * Embedded into meowhack, also GPL-3.0.
 */
package cat.omada.meowhack.license;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import cat.omada.meowhack.Kitty;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.fabricmc.loader.api.metadata.Person;

public final class LicenseChecker {
    private LicenseChecker() {}

    /**
     * Walks every loaded Fabric mod and throws if any of them ship under a license that isn't on the allowlist. 
     * Call this from onInitialize.
     */
    public static void run() {
        Kitty.LOG.info("Running free software check...");
        final var licenseSet = LicensePolicy.allowedLicenses();
        final var modSet = LicensePolicy.allowedModIds();

        final var nonCompliantMods = FabricLoader.getInstance().getAllMods()
            .stream()
            // For nested mods, fall back to the parent if the nested mod has no license
            .map(mod -> {
                if (!LicensePolicy.FALLBACK_TO_PARENT) return mod;
                while (mod.getMetadata().getLicense().isEmpty() && mod.getContainingMod().isPresent()) {
                    mod = mod.getContainingMod().get();
                }
                return mod;
            })
            .filter(mod -> !(LicensePolicy.WHITELIST_BUILTIN && mod.getMetadata().getType().equals("builtin")))
            .filter(mod -> {
                final var s = mod.getMetadata().getLicense()
                    .stream()
                    .map(LicensePolicy::cleanLicense)
                    .collect(Collectors.toSet());
                s.retainAll(licenseSet);
                return s.isEmpty();
            })
            // Drop explicitly whitelisted mods
            .filter(mod -> !modSet.contains(mod.getMetadata().getId()))
            .toList();

        if (!nonCompliantMods.isEmpty()) {
            Kitty.LOG.error("!!! Detected Violations of User Rights !!!");
            for (final var mod : nonCompliantMods) {
                printLicenseTrace(mod);
            }
            throw new RuntimeException(buildViolationMessage(nonCompliantMods));
        }

        Kitty.LOG.info("No proprietary & non-excluded mods found");
    }

    private static String buildViolationMessage(List<ModContainer> mods) {
        final var sb = new StringBuilder();
        sb.append("Non-free mods detected. Meowhack requires all mods to use free software only licenses.\n\n");
        for (final var mod : mods) {
            sb.append(String.format("  - %s (%s) by %s  [licenses: %s]\n",
                mod.getMetadata().getName(),
                mod.getMetadata().getId(),
                String.join(", ", mod.getMetadata().getAuthors().stream().map(Person::getName).toList()),
                String.join(", ", mod.getMetadata().getLicense())
            ));
        }
        sb.append("\nTo allow a mod, open a pull request to the meowhack repository .");
        return sb.toString();
    }

    private static String formatModName(ModContainer mod) {
        if (mod.getOrigin().getKind() == ModOrigin.Kind.PATH) {
            return String.format("mod '%s' (id %s) by '%s' (in files %s)",
                mod.getMetadata().getName(),
                mod.getMetadata().getId(),
                String.join(", ", mod.getMetadata().getAuthors().stream().map(Person::getName).toList()),
                String.join(", ", mod.getOrigin().getPaths().stream().map(Path::toString).toList())
            );
        }

        return String.format("mod '%s' (id %s) by '%s'",
            mod.getMetadata().getName(),
            mod.getMetadata().getId(),
            String.join(", ", mod.getMetadata().getAuthors().stream().map(Person::getName).toList())
        );
    }

    private static void printLicenseTrace(ModContainer mod) {
        Kitty.LOG.error(String.format(
            "Bad! %s has licenses [%s]!",
            formatModName(mod),
            String.join(", ", mod.getMetadata().getLicense())
        ));

        while (mod.getContainingMod().isPresent()) {
            mod = mod.getContainingMod().get();
            Kitty.LOG.error(String.format("    from %s", formatModName(mod)));
        }
    }
}
