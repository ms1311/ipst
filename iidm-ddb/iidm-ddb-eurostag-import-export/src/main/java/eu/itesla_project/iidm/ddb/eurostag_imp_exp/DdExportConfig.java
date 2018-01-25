/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.ddb.eurostag_imp_exp;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DdExportConfig {

    private static final String MODULE_NAME = "ddImportExport";

    private static final boolean DEFAULT_AUTOMATON_A11 = false;
    private static final boolean DEFAULT_AUTOMATON_A12 = false;
    private static final boolean DEFAULT_AUTOMATON_A14 = false;
    private static final boolean DEFAULT_RST = false;
    private static final boolean DEFAULT_ACMC = false;
    private static final boolean DEFAULT_LV_LOAD_MODELING = false;
    private static final String DEFAULT_RST_REGUL_INJECTOR = "RSTN_PCA";
    private static final String DEFAULT_RST_REGUL_GENERATOR = "APRTH1";
    private static final String DEFAULT_RST_REGUL_GENERATOR_DELETE = "CONSIG";
    private static final String DEFAULT_ACMC_REGUL = "ACMC";
    private static final String DEFAULT_RST_PILOT_GENERATORS = "";
    private static final float DEFAULT_LOAD_PATTERN_ALPHA = 1;
    private static final float DEFAULT_LOAD_PATTERN_BETA = 2;
    private static final boolean DEFAULT_GENPQFILTER = false;
    private static final boolean DEFAULT_EXPORT_MAIN_CC_ONLY = false;
    private static final boolean DEFAULT_NOSWITCH = false;

    private boolean automatonA11;
    private boolean automatonA12;
    private boolean automatonA14;
    private boolean importExportRST;
    private boolean importExportACMC;
    private boolean LVLoadModeling;
    private String RSTRegulInjector;
    private String RSTRegulGenerator;
    private String RSTRegulGeneratorDelete;
    private String ACMCRegul;
    private String RSTPilotGenerators;
    private float loadPatternAlpha;
    private float loadPatternBeta;
    private boolean gensPQfilter;
    private boolean exportMainCCOnly;
    private boolean noSwitch;

    public static DdExportConfig load() {
        boolean automatonA11 = DEFAULT_AUTOMATON_A11;
        boolean automatonA12 = DEFAULT_AUTOMATON_A12;
        boolean automatonA14 = DEFAULT_AUTOMATON_A14;
        boolean importExportRST = DEFAULT_RST;
        boolean importExportACMC = DEFAULT_ACMC;
        boolean lvLoadModeling = DEFAULT_LV_LOAD_MODELING;
        String rstRegulInjector = DEFAULT_RST_REGUL_INJECTOR;
        String rstRegulGenerator = DEFAULT_RST_REGUL_GENERATOR;
        String rstRegulGeneratorDelete = DEFAULT_RST_REGUL_GENERATOR_DELETE;
        String acmcRegul = DEFAULT_ACMC_REGUL;
        String rstPilotGenerators = DEFAULT_RST_PILOT_GENERATORS;
        float loadPatternAlpha = DEFAULT_LOAD_PATTERN_ALPHA;
        float loadPatternBeta = DEFAULT_LOAD_PATTERN_BETA;
        boolean gensPQfilter = DEFAULT_GENPQFILTER;
        boolean exportMainCCOnly = DEFAULT_EXPORT_MAIN_CC_ONLY;
        boolean noSwitch = DEFAULT_NOSWITCH;

        if (PlatformConfig.defaultConfig().moduleExists(MODULE_NAME)) {
            ModuleConfig config = PlatformConfig.defaultConfig().getModuleConfig(MODULE_NAME);
            automatonA11 = config.getBooleanProperty("automatonA11", DEFAULT_AUTOMATON_A11);
            automatonA12 = config.getBooleanProperty("automatonA12", DEFAULT_AUTOMATON_A12);
            automatonA14 = config.getBooleanProperty("automatonA14", DEFAULT_AUTOMATON_A14);
            importExportRST = config.getBooleanProperty("importExportRST", DEFAULT_RST);
            importExportACMC = config.getBooleanProperty("importExportACMC", DEFAULT_ACMC);
            lvLoadModeling = config.getBooleanProperty("LVLoadModeling", DEFAULT_LV_LOAD_MODELING);
            rstRegulInjector = config.getStringProperty("RSTRegulInjector", DEFAULT_RST_REGUL_INJECTOR);
            rstRegulGenerator = config.getStringProperty("RSTRegulGenerator", DEFAULT_RST_REGUL_GENERATOR);
            rstRegulGeneratorDelete = config.getStringProperty("RSTRegulGeneratorDelete", DEFAULT_RST_REGUL_GENERATOR_DELETE);
            acmcRegul = config.getStringProperty("ACMCRegul", DEFAULT_ACMC_REGUL);
            rstPilotGenerators = config.getStringProperty("RSTPilotGenerators", DEFAULT_RST_PILOT_GENERATORS);
            loadPatternAlpha = config.getFloatProperty("loadPatternAlpha", DEFAULT_LOAD_PATTERN_ALPHA);
            loadPatternBeta = config.getFloatProperty("loadPatternBeta", DEFAULT_LOAD_PATTERN_BETA);
            gensPQfilter = config.getBooleanProperty("gensPQfilter", DEFAULT_GENPQFILTER);
        }

        if (PlatformConfig.defaultConfig().moduleExists("eurostag-ech-export")) {
            ModuleConfig config = PlatformConfig.defaultConfig().getModuleConfig("eurostag-ech-export");
            exportMainCCOnly = config.getBooleanProperty("exportMainCCOnly", DEFAULT_EXPORT_MAIN_CC_ONLY);
            noSwitch = config.getBooleanProperty("noSwitch", DEFAULT_NOSWITCH);
        }

        return new DdExportConfig(automatonA11, automatonA12, automatonA14, importExportRST, importExportACMC,
                                  lvLoadModeling, rstRegulInjector, rstRegulGenerator, rstRegulGeneratorDelete,
                                  acmcRegul, rstPilotGenerators, loadPatternAlpha, loadPatternBeta, gensPQfilter, exportMainCCOnly, noSwitch);
    }

    public DdExportConfig() {
        this(DEFAULT_AUTOMATON_A11, DEFAULT_AUTOMATON_A12, DEFAULT_AUTOMATON_A14, DEFAULT_RST, DEFAULT_ACMC,
                DEFAULT_LV_LOAD_MODELING, DEFAULT_RST_REGUL_INJECTOR, DEFAULT_RST_REGUL_GENERATOR, DEFAULT_RST_REGUL_GENERATOR_DELETE,
                DEFAULT_ACMC_REGUL, DEFAULT_RST_PILOT_GENERATORS, DEFAULT_LOAD_PATTERN_ALPHA, DEFAULT_LOAD_PATTERN_BETA, DEFAULT_GENPQFILTER, DEFAULT_EXPORT_MAIN_CC_ONLY, DEFAULT_NOSWITCH);
    }

    public DdExportConfig(boolean automatonA11, boolean automatonA12, boolean automatonA14, boolean importExportRST,
                          boolean importExportACMC, boolean lvLoadModeling, String rstRegulInjector,
                          String rstRegulGenerator, String rstRegulGeneratorDelete, String acmcRegul,
                          String rstPilotGenerators, float loadPatternAlpha, float loadPatternBeta, boolean gensPQfilter, boolean exportMainCCOnly, boolean noSwitch) {
        this.automatonA11 = automatonA11;
        this.automatonA12 = automatonA12;
        this.automatonA14 = automatonA14;
        this.importExportRST = importExportRST;
        this.importExportACMC = importExportACMC;
        this.LVLoadModeling = lvLoadModeling;
        this.RSTRegulInjector = rstRegulInjector;
        this.RSTRegulGenerator = rstRegulGenerator;
        this.RSTRegulGeneratorDelete = rstRegulGeneratorDelete;
        this.ACMCRegul = acmcRegul;
        this.RSTPilotGenerators = rstPilotGenerators;
        this.loadPatternAlpha = loadPatternAlpha;
        this.loadPatternBeta = loadPatternBeta;
        this.gensPQfilter = gensPQfilter;
        this.exportMainCCOnly = exportMainCCOnly;
        this.noSwitch = noSwitch;
    }

    public boolean getAutomatonA11() {
        return automatonA11;
    }

    public boolean getAutomatonA12() {
        return automatonA12;
    }

    public boolean getAutomatonA14() {
        return automatonA14;
    }

    public boolean getExportRST() {
        return importExportRST;
    }

    public boolean getExportACMC() {
        return importExportACMC;
    }

    public boolean getLVLoadModeling() {
        return LVLoadModeling;
    }

    public boolean getImportRST() {
        return importExportRST;
    }

    public boolean getImportACMC() {
        return importExportACMC;
    }

    public String getRSTRegulInjector() {
        return RSTRegulInjector;
    }

    public String getRSTRegulGenerator() {
        return RSTRegulGenerator;
    }

    public String getRSTRegulGeneratorDelete() {
        return RSTRegulGeneratorDelete;
    }

    public String getACMCRegul() {
        return ACMCRegul;
    }

    public String getRSTPilotGenerators() {
        return RSTPilotGenerators;
    }

    public float getLoadPatternAlpha() {
        return loadPatternAlpha;
    }

    public float getLoadPatternBeta() {
        return loadPatternBeta;
    }

    public boolean getGensPQfilter() {
        return gensPQfilter;
    }

    public void setAutomatonA11(Boolean automatonA11) {
        this.automatonA11 = automatonA11;
    }

    public void setAutomatonA12(Boolean automatonA12) {
        this.automatonA12 = automatonA12;
    }

    public void setAutomatonA14(Boolean automatonA14) {
        this.automatonA14 = automatonA14;
    }

    public void setImportExportRST(Boolean importExportRST) {
        this.importExportRST = importExportRST;
    }

    public void setLVLoadModeling(Boolean lvLoadModeling) {
        this.LVLoadModeling = lvLoadModeling;
    }

    public void setImportExportACMC(Boolean importExportACMC) {
        this.importExportACMC = importExportACMC;
    }

    public void setRSTRegulInjector(String rstRegulInjector) {
        this.RSTRegulInjector = rstRegulInjector;
    }

    public void setRSTRegulGenerator(String rstRegulGenerator) {
        this.RSTRegulGenerator = rstRegulGenerator;
    }

    public void setRSTRegulGeneratorDelete(String rstRegulGeneratorDelete) {
        this.RSTRegulGeneratorDelete = rstRegulGeneratorDelete;
    }

    public void setACMCRegul(String acmcRegul) {
        this.ACMCRegul = acmcRegul;
    }

    public void setRSTPilotGenerators(String rstPilotGenerators) {
        this.RSTPilotGenerators = rstPilotGenerators;
    }

    public void setLoadPatternAlpha(float loadPatternAlpha) {
        this.loadPatternAlpha = loadPatternAlpha;
    }

    public void setLoadPatternBeta(float loadPatternBeta) {
        this.loadPatternBeta = loadPatternBeta;
    }

    public void setGensPQfilter(boolean gensPQfilter) {
        this.gensPQfilter = gensPQfilter;
    }

    public boolean isExportMainCCOnly() {
        return exportMainCCOnly;
    }

    public boolean isNoSwitch() {
        return noSwitch;
    }
}
