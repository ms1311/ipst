/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016-2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.mcla.montecarlo;

import com.powsybl.computation.*;
import com.powsybl.iidm.network.Network;
import eu.itesla_project.mcla.NetworkUtils;
import eu.itesla_project.mcla.montecarlo.data.SampleData;
import eu.itesla_project.mcla.montecarlo.data.SampledData;
import eu.itesla_project.mcla.montecarlo.data.SamplingNetworkData;
import eu.itesla_project.sampling.util.Utils;
import eu.itesla_project.modules.mcla.ForecastErrorsAnalyzerParameters;
import eu.itesla_project.modules.mcla.ForecastErrorsDataStorage;
import eu.itesla_project.modules.mcla.MontecarloSampler;
import eu.itesla_project.modules.mcla.MontecarloSamplerParameters;
import eu.itesla_project.modules.online.TimeHorizon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class MontecarloSamplerImpl implements MontecarloSampler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MontecarloSamplerImpl.class);

    private static final String WORKING_DIR_PREFIX = "itesla_montecarlosampler_";
    private static final String MCSINPUTFILEPREFIX = "mcsamplerinput_";
    private static final String MCSOUTPUTFILENAME = "mcsampleroutput.mat";
    private static final String MCS_CSV_OUTPUTFILENAME = "printSamples.csv";
    private static final String MCLA = "mcla";

    private final ComputationManager computationManager;
    private final ForecastErrorsDataStorage forecastErrorsDataStorage;

    private Network network;
    private MontecarloSamplerConfig config = null;

    private TimeHorizon timeHorizon;
    private String feAnalysisId;
    private int nSamples;
    private int currentSampleIndex = 0;
    private ArrayList<String> generatorsIds = new ArrayList<String>();
    private ArrayList<String> loadsIds = new ArrayList<String>();
    private ArrayList<String> connectedGeneratorsIds = new ArrayList<String>();
    private ArrayList<String> connectedLoadsIds = new ArrayList<String>();
    private SamplingNetworkData samplingNetworkData;
    private SampledData sampledData = null;
    private Path networkDataMatFile = null;
    private ForecastErrorsAnalyzerParameters feaParams = null;

    public MontecarloSamplerImpl(Network network, ComputationManager computationManager, ForecastErrorsDataStorage forecastErrorsDataStorage, MontecarloSamplerConfig config) {
        Objects.requireNonNull(network, "network is null");
        Objects.requireNonNull(computationManager, "computationManager is null");
        Objects.requireNonNull(forecastErrorsDataStorage, "forecast errors data storage is null");
        Objects.requireNonNull(config, "config is null");
        LOGGER.info("Network {}: {}", network.getId(), config.toString());
        this.network = network;
        this.computationManager = computationManager;
        this.forecastErrorsDataStorage = forecastErrorsDataStorage;
        this.config = config;
        currentSampleIndex = -1;
    }

    public MontecarloSamplerImpl(Network network, ComputationManager client, ForecastErrorsDataStorage forecastErrorsDataStorage) {
        this(network, client, forecastErrorsDataStorage, MontecarloSamplerConfig.load());
    }

    @Override
    public String getName() {
        return "RSE Montecarlo Sampler";
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public void init(MontecarloSamplerParameters parameters) throws Exception {
        Objects.requireNonNull(parameters, "montecarlo sampler parameters value is null");
        this.timeHorizon = parameters.getTimeHorizon();
        this.feAnalysisId = parameters.getFeAnalysisId();
        this.nSamples = parameters.getnSamples();
        // check if forecast offline samples data file for this time horizon exists
        if (!forecastErrorsDataStorage.isForecastOfflineSamplesDataAvailable(feAnalysisId, timeHorizon)) {
            LOGGER.error("No forecast offline samples data available, for {} network, {} time horizon.", network.getId(), timeHorizon.getName());
            throw new Exception("Montecarlo sampler not ready to be used: No forecast offline samples data available, for " + network.getId() + " network, " + timeHorizon.getName() + " time horizon.");
        }
        feaParams = forecastErrorsDataStorage.getParameters(feAnalysisId, timeHorizon);
        LOGGER.info("Network {}: forecast errors analysis - Id: {},  time horizon: {}, number of samples available: {}, number of samples requested: {}.", network.getId(), feAnalysisId, timeHorizon.getName(), feaParams.getnSamples(), nSamples);
        if ((nSamples <= 0) || (nSamples > feaParams.getnSamples())) {
            LOGGER.error("Network {}: Not enough/incorrect number of samples available from FEA ( id {}, time horizon {} ): requested {} samples, available {} samples", network.getId(), feAnalysisId, timeHorizon.getName(), nSamples, feaParams.getnSamples());
            throw new Exception("Network " + network.getId() + ": Not enough/incorrect number of samples available from FEA (Id: " + feAnalysisId + ", time horizon: " + timeHorizon.getName() + "): requested " + nSamples + " samples, available " + feaParams.getnSamples() + " samples.");
        }
        // I put the generators and loads in a specific order, the same used when producing the matrix of historical data,
        // for the forecast errors analysis
        generatorsIds = NetworkUtils.getGeneratorsIds(network);
        connectedGeneratorsIds = NetworkUtils.getConnectedGeneratorsIds(network);
        loadsIds = NetworkUtils.getLoadsIds(network);
        connectedLoadsIds = NetworkUtils.getConnectedLoadsIds(network);
        // create the sampling network data
        LOGGER.info("Preparing sampling network data for {} network", network.getId());
        samplingNetworkData = new SamplingDataCreator(network, generatorsIds, loadsIds).createSamplingNetworkData();
        // write mat network data file
        // TODO use common file transfer feature
        networkDataMatFile = Files.createTempFile(config.getTmpDir(), MCSINPUTFILEPREFIX + network.getId().replaceAll(" ", "_") + "_" + timeHorizon.getLabel() + "_", ".mat");
        LOGGER.info("Writing sampling network data for {} network into mat file {}", network.getId(), networkDataMatFile);
        new MCSMatFileWriter(networkDataMatFile).writeSamplingNetworkData(samplingNetworkData);
    }

    @Override
    public void sample() throws Exception {
        String stateId = network.getStateManager().getWorkingStateId();
        LOGGER.info("Getting new sample for network " + network + ", working state id: " + stateId);
        SampleData sample = nextSample();
        putSampleDataIntoNetwork(sample);
    }

    private synchronized SampleData nextSample() throws Exception {
        if (currentSampleIndex == -1) {
            LOGGER.info("Network {}: executing Montecarlo sampler, getting {} samples", network.getId(), nSamples);
            sampledData = runMontecarloSampler();
        }
        currentSampleIndex++;
        if (currentSampleIndex >= nSamples) {
            LOGGER.error("Network {}: reached max number of samples: {} - FEA id: {}", network.getId(), feaParams.getnSamples(), feaParams.getFeAnalysisId());
            throw new Exception("Network " + network.getId() + ": reached max number of samples: " + feaParams.getnSamples() + " - FEA id: " + feaParams.getFeAnalysisId());
        }
        LOGGER.debug("Network {} -> current sample index: {}", network.getId(), currentSampleIndex);
        float[] generatorsActivePower = null;
        if (sampledData.getGeneratorsActivePower() != null) {
            generatorsActivePower = Utils.toFloatArray(sampledData.getGeneratorsActivePower()[currentSampleIndex]);
        }
        float[] loadsActivePower = null;
        if (sampledData.getLoadsActivePower() != null) {
            loadsActivePower = Utils.toFloatArray(sampledData.getLoadsActivePower()[currentSampleIndex]);
        }
        float[] loadsReactivePower = null;
        if (sampledData.getLoadsReactivePower() != null) {
            loadsReactivePower = Utils.toFloatArray(sampledData.getLoadsReactivePower()[currentSampleIndex]);
        }
        SampleData sampleData = new SampleData(generatorsActivePower, loadsActivePower, loadsReactivePower);
        return sampleData;
    }

    private SampledData runMontecarloSampler() throws Exception {

        return computationManager.execute(new ExecutionEnvironment(createEnv(), WORKING_DIR_PREFIX, config.isDebug()), new AbstractExecutionHandler<SampledData>() {
            @Override
            public List<CommandExecution> before(Path workingDir) throws IOException {
                // put mat file(s) in working dir
                Path forecastOfflineSamplesDataFile = null;
                if (config.isCopyFEFile()) {
                    forecastOfflineSamplesDataFile = Paths.get(workingDir.toString(), MCSINPUTFILEPREFIX + "forecast_offline_samples_" + timeHorizon.getLabel() + ".mat");
                    forecastErrorsDataStorage.getForecastOfflineSamplesFile(feAnalysisId, timeHorizon, forecastOfflineSamplesDataFile);
                } else {
                    forecastOfflineSamplesDataFile = forecastErrorsDataStorage.getForecastOfflineSamplesFilePath(feAnalysisId, timeHorizon);
                }
                Path localNetworkDataMatFile = Paths.get(workingDir.toString(), MCSINPUTFILEPREFIX + network.getId().replaceAll(" ", "_") + ".mat");
                Files.copy(networkDataMatFile, localNetworkDataMatFile);

                LOGGER.info("Running montecarlo sampler on {} network, asking for {} samples", network.getId(), nSamples);
                Command cmd = createCommand(forecastOfflineSamplesDataFile, localNetworkDataMatFile);
                return Collections.singletonList(new CommandExecution(cmd, 1));
            }

            @Override
            public SampledData after(Path workingDir, ExecutionReport report) throws IOException {
                report.log();
                LOGGER.debug("Network {}: retrieving sampling results from file {}", network.getId(), MCSOUTPUTFILENAME);
                try {
                    return new MCSMatFileReader(workingDir.resolve(MCSOUTPUTFILENAME)).getSampledData();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).join();
    }

    private Map<String, String> createEnv() {
        Map<String, String> env = new HashMap<>();
        env.put("MCRROOT", config.getRuntimeHomeDir().toString());
        env.put("LD_LIBRARY_PATH", config.getRuntimeHomeDir().resolve("runtime").resolve("glnxa64").toString()
                + ":" + config.getRuntimeHomeDir().resolve("bin").resolve("glnxa64").toString()
                + ":" + config.getRuntimeHomeDir().resolve("sys").resolve("os").resolve("glnxa64").toString());
        return env;
    }


    private Command createCommand(Path forecastErrorsDataFile, Path localNetworkDataMatFile) {
        List<String> args1 = new ArrayList<>();
        args1.add(localNetworkDataMatFile.getFileName().toString());
        args1.add(config.isCopyFEFile() ? forecastErrorsDataFile.getFileName().toString() : forecastErrorsDataFile.toAbsolutePath().toString());
        args1.add(MCSOUTPUTFILENAME);
        args1.add("" + nSamples);
        args1.add("" + config.getOptionSign());
        args1.add("" + config.getCentering());
        args1.add("" + config.getFull_dependence()); //added in v1.8.1

        if (config.isCopyFEFile()) {
            return new SimpleCommandBuilder()
                    .id("matmcs")
                    .program(config.getBinariesDir().resolve(MCLA).toAbsolutePath().toString())
                    .args(args1)
                    .inputFiles(new InputFile(localNetworkDataMatFile.getFileName().toString()),
                                new InputFile(forecastErrorsDataFile.getFileName().toString()))
                    .outputFiles(new OutputFile(MCSOUTPUTFILENAME), new OutputFile(MCS_CSV_OUTPUTFILENAME))
                    .build();
        } else {
            return new SimpleCommandBuilder()
                    .id("matmcs")
                    .program(config.getBinariesDir().resolve(MCLA).toAbsolutePath().toString())
                    .args(args1)
                    .inputFiles(new InputFile(localNetworkDataMatFile.getFileName().toString()))
                    .outputFiles(new OutputFile(MCSOUTPUTFILENAME), new OutputFile(MCS_CSV_OUTPUTFILENAME))
                    .build();
        }
    }

    private void putSampleDataIntoNetwork(SampleData sample) {
        LOGGER.debug("Storing new sample in the working state {} of {} network", network.getStateManager().getWorkingStateId(), network.getId());
        LOGGER.debug("Network {}: connected network generators = {} - sampled generators = {}", network.getId(), connectedGeneratorsIds.size(), sample.getGeneratorsActivePower().length);
        float qThreshold = 1000;
        float totalPGenBS = 0;
        float totalPGenAS = 0;
        float totalPLoadBS = 0;
        float totalPLoadAS = 0;
        float totalQLoadBS = 0;
        float totalQLoadAS = 0;
        if (sample.getGeneratorsActivePower() != null) {
            for (int i = 0; i < connectedGeneratorsIds.size(); i++) {
                String generatorId = connectedGeneratorsIds.get(i);
                double newActivePower = sample.getGeneratorsActivePower()[i];
                double oldActivePower = network.getGenerator(generatorId).getTerminal().getP();
                totalPGenBS += oldActivePower;
                totalPGenAS += newActivePower;
                LOGGER.debug("Network {} state {}: generator {} - P:{} -> P:{} - limits[{},{}]",
                        network.getId(), network.getStateManager().getWorkingStateId(), generatorId, oldActivePower, newActivePower,
                        network.getGenerator(generatorId).getMinP(), network.getGenerator(generatorId).getMaxP());
                if (network.getGenerator(generatorId).getMaxP() < -newActivePower) {
                    LOGGER.warn("Network {} state {}: generator {} - new P ({}) > max P ({})",
                            network.getId(), network.getStateManager().getWorkingStateId(), generatorId, -newActivePower, network.getGenerator(generatorId).getMaxP());
                }
                if (network.getGenerator(generatorId).getMinP() > -newActivePower) {
                    LOGGER.warn("Network {} state {}: generator {} - new P ({}) < min P ({})",
                            network.getId(), network.getStateManager().getWorkingStateId(), generatorId, -newActivePower, network.getGenerator(generatorId).getMinP());
                }
                if (!Double.isNaN(newActivePower)) {
                    network.getGenerator(generatorId).setTargetP(-newActivePower);
                    network.getGenerator(generatorId).getTerminal().setP(newActivePower);
                } else {
                    LOGGER.debug("Network {} state {}: new sampled P for generator {} is NaN: skipping assignment", network.getId(), network.getStateManager().getWorkingStateId(), generatorId);
                }
            }
        }
        LOGGER.debug("Network {}: connected network loads = {} - sampled loads = [{},{}]", network.getId(), connectedLoadsIds.size(), sample.getLoadsActivePower().length, sample.getLoadsReactivePower().length);
        if (sample.getLoadsActivePower() != null || sample.getLoadsReactivePower() != null) {
            for (int i = 0; i < connectedLoadsIds.size(); i++) {
                String loadId = connectedLoadsIds.get(i);
                if (sample.getLoadsActivePower() != null) {
                    double newActivePower = sample.getLoadsActivePower()[i];
                    double oldActivePower = network.getLoad(loadId).getTerminal().getP();
                    totalPLoadBS += oldActivePower;
                    totalPLoadAS += newActivePower;
                    LOGGER.debug("Network {} state {}: load {} - P:{} -> P:{} ", network.getId(), network.getStateManager().getWorkingStateId(), loadId, oldActivePower, newActivePower);
                    if (!Double.isNaN(newActivePower)) {
                        network.getLoad(loadId).setP0(newActivePower);
                        network.getLoad(loadId).getTerminal().setP(newActivePower);
                    } else {
                        LOGGER.debug("Network {} state {}: new sampled P for load {} is NaN: skipping assignment", network.getId(), network.getStateManager().getWorkingStateId(), loadId);
                    }
                }
                if (sample.getLoadsReactivePower() != null) {
                    double newReactivePower = sample.getLoadsReactivePower()[i];
                    double oldReactivePower = network.getLoad(loadId).getTerminal().getQ();
                    totalQLoadBS += oldReactivePower;
                    // filter suggested by RSE: skip assignment if the new value is greater than a certain threshold (e.g. 1000 MVar)
                    // it is necessary to have consistent data (to make the load flow converge) when Q is computed based on P
                    if (Math.abs(newReactivePower) <= qThreshold) {
                        totalQLoadAS += newReactivePower;
                        LOGGER.debug("Network {} state {}: load {} - Q:{} -> Q:{} ", network.getId(), network.getStateManager().getWorkingStateId(), loadId, oldReactivePower, newReactivePower);
                        if (!Double.isNaN(newReactivePower)) {
                            network.getLoad(loadId).setQ0(newReactivePower);
                            network.getLoad(loadId).getTerminal().setQ(newReactivePower);
                        } else {
                            LOGGER.debug("Network {} state {}: new sampled Q for load {} is NaN: skipping assignment", network.getId(), network.getStateManager().getWorkingStateId(), loadId);
                        }
                    } else {
                        totalQLoadAS += oldReactivePower;
                        LOGGER.warn("Network {} state {}: load {} - |new Q({})| > {}: skipping assignment and keeping old Q({})",
                                network.getId(), network.getStateManager().getWorkingStateId(), loadId, newReactivePower, qThreshold, oldReactivePower);
                    }
                }
            }
        }
        LOGGER.debug("Network {} state {}: gen total P:{} -> total P:{} ", network.getId(), network.getStateManager().getWorkingStateId(), totalPGenBS, totalPGenAS);
        LOGGER.debug("Network {} state {}: load total P:{} -> total P:{} ", network.getId(), network.getStateManager().getWorkingStateId(), totalPLoadBS, totalPLoadAS);
        LOGGER.debug("Network {} state {}: load total Q:{} -> total Q:{} ", network.getId(), network.getStateManager().getWorkingStateId(), totalQLoadBS, totalQLoadAS);
    }
}
