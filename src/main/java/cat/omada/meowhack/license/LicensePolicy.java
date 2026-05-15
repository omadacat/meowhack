/*
 * License lists originally from fabric-license-check by Flowey (GPL-3.0).
 * https://github.com/FloweyTheFlower/fabric-license-check
 * Embedded into meowhack, also GPL-3.0.
 */
package cat.omada.meowhack.license;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Hardcoded policy for the embedded license check. No external config file.
 * If you want to change what's allowed, edit this class and rebuild.
 */
public final class LicensePolicy {
    private LicensePolicy() {}

    /** OSI-approved license identifiers from https://github.com/spdx/license-list-data */
    private static final List<String> OSI_LICENSES = List.of(
        "0bsd", "aal", "afl1.1", "afl1.2", "afl2.0", "afl2.1", "afl3.0", "agpl3.0", "agpl3.0only",
        "agpl3.0orlater", "apache1.1", "apache2.0", "apl1.0", "apsl1.0", "apsl1.1", "apsl1.2", "apsl2.0", "artistic1.0",
        "artistic1.0cl8", "artistic1.0perl", "artistic2.0", "blueoak1.0.0", "bsd1clause", "bsd2clause",
        "bsd2clausepatent", "bsd3clause", "bsd3clauselbnl", "bsl1.0", "cal1.0", "cal1.0combinedworkexception",
        "catosl1.1", "cddl1.0", "cecill2.1", "cernohlp2.0", "cernohls2.0", "cernohlw2.0", "cnripython", "cpal1.0",
        "cpl1.0", "cuaopl1.0", "ecl1.0", "ecl2.0", "efl1.0", "efl2.0", "entessa", "epl1.0", "epl2.0", "eudatagrid",
        "eupl1.1", "eupl1.2", "fair", "frameworx1.0", "gpl2.0", "gpl2.0+", "gpl2.0only", "gpl2.0orlater", "gpl3.0",
        "gpl3.0+", "gpl3.0only", "gpl3.0orlater", "gpl3.0withgccexception", "hpnd", "icu", "intel", "ipa", "ipl1.0",
        "isc", "jam", "lgpl2.0", "lgpl2.0+", "lgpl2.0only", "lgpl2.0orlater", "lgpl2.1", "lgpl2.1+", "lgpl2.1only",
        "lgpl2.1orlater", "lgpl3.0", "lgpl3.0+", "lgpl3.0only", "lgpl3.0orlater", "liliqp1.1", "liliqr1.1",
        "liliqrplus1.1", "lpl1.0", "lpl1.02", "lppl1.3c", "miros", "mit", "mit0", "mitmodernvariant", "motosoto",
        "mpl1.0", "mpl1.1", "mpl2.0", "mpl2.0nocopyleftexception", "mspl", "msrl", "mulanpsl2.0", "multics", "nasa1.3",
        "naumen", "ncsa", "ngpl", "nokia", "nposl3.0", "ntp", "oclc2.0", "ofl1.1", "ofl1.1norfn", "ofl1.1rfn", "ogtsl",
        "oldap2.8", "olfl1.3", "osetpl2.1", "osl1.0", "osl2.0", "osl2.1", "osl3.0", "php3.0", "php3.01", "postgresql",
        "python2.0", "qpl1.0", "rpl1.1", "rpl1.5", "rpsl1.0", "rscpl", "simpl2.0", "sissl", "sleepycat", "spl1.0",
        "ucl1.0", "unicode3.0", "unicodedfs2016", "unlicense", "upl1.0", "vsl1.0", "w3c", "watcom1.0", "wxwindows",
        "xnet", "zlib", "zpl2.0", "zpl2.1"
    );

    /** Common alternative spellings mods use in their fabric.mod.json that don't match SPDX exactly. */
    private static final List<String> ALT_LICENSES = List.of(
        "gpl3",
        "gnulgplv3",
        "lgplv3",
        "gnulgplv2.1",
        "gnulgplv3.0",
	"GNU Lesser General Public License v2.1",
	"LGPL-3",
	"MIT Licence",
	"MIT License",
	"CC0-1.0",
	"CC-BY-NC-SA-4.0",
	"Mozilla Public License Version 2.0",
	"GPLv3",
	"Apache License 2.0",
	"tr7zw Protective License" // MIT rebrand
    );

    /** Mod IDs to allow through regardless of license. */
    private static final List<String> ALLOWED_MOD_IDS = List.of(
        "nochatreports",
	"sodium", //open but not free
	"xaerominimap", //proprietary trash
        "xaeroworldmap", 
	"xaerolib",
	"badoptimizations" //mit
    );

    /** If true, treat "builtin" mods (java, minecraft) as always allowed. */
    public static final boolean WHITELIST_BUILTIN = true;

    /** If true and a nested mod has no license, fall back to checking its parent's license. */
    public static final boolean FALLBACK_TO_PARENT = true;

    public static Set<String> allowedLicenses() {
        return Stream.concat(OSI_LICENSES.stream(), ALT_LICENSES.stream())
            .map(LicensePolicy::cleanLicense)
            .collect(Collectors.toSet());
    }

    public static Set<String> allowedModIds() {
        return Set.copyOf(ALLOWED_MOD_IDS);
    }

    /** Normalize a license string for comparison: lowercase, strip dashes/underscores/spaces. */
    public static String cleanLicense(String str) {
        return str.toLowerCase().replaceAll("[-_ ]", "");
    }
}
