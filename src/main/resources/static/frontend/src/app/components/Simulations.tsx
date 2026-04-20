import { useState, useEffect } from "react";
import { motion } from "motion/react";
import {
  PlayIcon,
  PlusIcon,
  TrendingDownIcon,
  TrendingUpIcon,
  Building2,
  HeartPulse,
  Users,
  BedDouble,
  ClockIcon,
  UserIcon,
  XIcon,
} from "lucide-react";
import { useApi } from "../hooks/useApi";
import { useAuth } from "../context/AuthContext";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";

const getRiskColor = (risk: string) => {
  const r = (risk || "").toUpperCase();
  if (r === "CRITICAL" || r === "HIGH") return "#DC2626";
  if (r === "SEVERE" || r === "MEDIUM") return "#EA580C";
  return "#10B981";
};

type SimulationMode = "baseline" | "scenario";

interface Action {
  actionId?: number;
  id?: number;
  actionType: string;
  regionId: number;
  district?: number;
  oldValue: string | number;
  newValue: string | number;
  numericDelta?: number;
  metricType?: string;
}

interface ScenarioSimulation {
  scenarioId: number;
  name: string;
  createdByUserId: number;
  actions: Action[];
  status?: string;
}

export default function Simulations() {
  const api = useApi();
  const { user } = useAuth();
  const [mode, setMode] = useState<SimulationMode>("baseline");
  const [scenarios, setScenarios] = useState<ScenarioSimulation[]>([]);
  const [baselineScores, setBaselineScores] = useState<any[]>([]);
  const [selectedScenario, setSelectedScenario] = useState<number | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newScenarioName, setNewScenarioName] = useState("");
  const [newScenarioActions, setNewScenarioActions] = useState<any[]>([]);
  const [facilities, setFacilities] = useState<any[]>([]);
  const [simulationResults, setSimulationResults] = useState<any>(null);
  const [isRunning, setIsRunning] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // New action form state
  const [actionMetric, setActionMetric] = useState("UPDATE_BEDS");
  const [actionDistrict, setActionDistrict] = useState<number>(0);
  const [actionOldValue, setActionOldValue] = useState<string | number>("");
  const [actionNewValue, setActionNewValue] = useState<string | number>("");

  const metricTypes = [
    { value: "UPDATE_BEDS", label: "Update Beds" },
    { value: "ADD_FACILITY", label: "Add Facility" },
  ];

  useEffect(() => {
    const fetchData = async () => {
      try {
        let districtsData: any = [];

        try { districtsData = await api.get("/api/districts"); } catch(e) {
          try { districtsData = await api.get("/api/regions"); } catch(e2) {}
        }

        const scensRaw = await api.get("/api/scenarios");
        
        const extractArray = (data: any) => {
          if (!data) return [];
          if (Array.isArray(data)) return data;
          if (data.content && Array.isArray(data.content)) return data.content;
          if (data.data && Array.isArray(data.data)) return data.data;
          if (data._embedded) {
            const vals = Object.values(data._embedded);
            if (vals.length > 0 && Array.isArray(vals[0])) return vals[0];
          }
          return [];
        };

        let dArr = extractArray(districtsData);
        
        const mapped = await Promise.all(dArr.map(async (d: any) => {
          const dId = d.id || d.regionId;
          let s: any = {};
          if (dId) {
            let sData = await api.get(`/api/scores/${dId}`).catch(() => null);
            if (!sData) sData = await api.get(`/api/districts/${dId}/scores`).catch(() => ({}));
            
            let sRaw = sData?.content || sData?.data || (sData?._embedded ? Object.values(sData._embedded)[0] : null) || sData;
            s = Array.isArray(sRaw) ? (sRaw[0] || {}) : (sRaw || {});
          }
          
          const sc = s.latestScore || s.score || s.computedScore || s;
          const dSc = d.latestScore || d.score || d.computedScore || d;

          const rawCiri = d.ciriScore || dSc.ciriScore || s.ciriScore || sc.ciriScore || d.overallScore || s.overallScore || sc.overallScore || 0;
          const rawHai = d.haiScore || dSc.haiScore || s.haiScore || sc.haiScore || d.hai || dSc.hai || s.hai || sc.hai || d.accessScore || s.accessScore || 0;
          const rawRvi = d.rviScore || dSc.rviScore || s.rviScore || sc.rviScore || d.rvi || dSc.rvi || s.rvi || sc.rvi || d.vulnerabilityScore || s.vulnerabilityScore || 0;
          const risk = [d.riskCategory, dSc.riskCategory, s.riskCategory, sc.riskCategory].find(r => r && r !== "LOW") || d.riskCategory || "LOW";

          const parseScore = (val: any) => {
            const num = Number(val);
            if (isNaN(num)) return 0;
            let percentage = num;
            if (Math.abs(num) > 0 && Math.abs(num) <= 1.5) percentage = num * 100;
            return Math.max(0, percentage);
          };

          return {
            ...d, ...s,
            regionId: dId || s.regionId || s.id,
            regionName: d.name || d.regionName || s.regionName || s.name || `District ${dId || '?'}`,
            population: d.population || s.population,
            ciriScore: parseScore(rawCiri),
            haiScore: parseScore(rawHai),
            rviScore: parseScore(rawRvi),
            riskCategory: risk ? risk.toUpperCase() : "LOW"
          };
        }));
        
        const scensArr = extractArray(scensRaw);

        let facsData = await api.get("/api/facilities").catch(() => null);
        if (!facsData) facsData = await api.get("/api/healthFacilities").catch(() => []);
        let facsArrRaw = extractArray(facsData);
        
        const facsMapped = await Promise.all(facsArrRaw.map(async (h: any) => {
          const hId = h.facilityId || h.id;
          let cap = h.capacity || h.latestCapacity;
          
          if (!cap && hId) {
            let capData = await api.get(`/api/facilities/${hId}/capacity`).catch(() => null);
            if (!capData) capData = await api.get(`/api/healthFacilities/${hId}/capacities`).catch(() => null);
            let cRaw = capData?.content || capData?.data || (capData?._embedded ? Object.values(capData._embedded)[0] : null) || capData;
            cap = Array.isArray(cRaw) ? (cRaw[0] || {}) : (cRaw || {});
          }
          return {
            ...h,
            regionId: h.regionId || h.region?.regionId || h.region?.id,
            totalBeds: cap?.totalBeds ?? cap?.total_beds ?? 0
          };
        }));

        setFacilities(facsMapped);
        setBaselineScores(mapped);
        setScenarios(scensArr);
        if (mapped.length > 0) setActionDistrict(mapped[0].regionId);
      } catch (err) {
        console.error("Failed to fetch simulation data", err);
      } finally {
        setIsLoading(false);
      }
    };
    fetchData();
  }, []);

  useEffect(() => {
    if (!actionDistrict) return;
    const districtFacs = facilities.filter((f) => f.regionId === actionDistrict);
    if (actionMetric === "UPDATE_BEDS") {
      const totalBeds = districtFacs.reduce((sum, f) => sum + (Number(f.totalBeds) || 0), 0);
      setActionOldValue(totalBeds);
    } else if (actionMetric === "ADD_FACILITY") {
      setActionOldValue(districtFacs.length);
    }
  }, [actionDistrict, actionMetric, facilities]);

  const handleAddAction = () => {
    const newAction = {
      id: Date.now(),
      actionType: actionMetric,
      metricType: actionMetric,
      regionId: actionDistrict,
      district: actionDistrict,
      oldValue: actionOldValue,
      newValue: actionNewValue,
      numericDelta: Number(actionNewValue) - Number(actionOldValue),
    };
    setNewScenarioActions([...newScenarioActions, newAction]);
    setActionNewValue("");
  };

  const handleRemoveAction = (idToRemove?: number) => {
    if (!idToRemove) return;
    setNewScenarioActions(newScenarioActions.filter((a) => a.id !== idToRemove));
  };

  const handleCreateScenario = async () => {
    if (!newScenarioName || newScenarioActions.length === 0) return;

    try {
      const created = await api.post("/api/scenarios", {
        name: newScenarioName,
        regionId: newScenarioActions[0].regionId // Assuming main region from first action
      });

      for (const action of newScenarioActions) {
        await api.post(`/api/scenarios/${created.scenarioId}/actions`, action);
      }

      const refreshed = await api.get(`/api/scenarios/${created.scenarioId}`);
      setScenarios([...scenarios, refreshed]);
      setShowCreateModal(false);
      setNewScenarioName("");
      setNewScenarioActions([]);
    } catch (err) {
      console.error("Failed to create scenario", err);
    }
  };

  const runScenarioSimulation = async (scenarioId: number) => {
    setIsRunning(true);
    try {
      const response = await api.post(`/api/scenarios/${scenarioId}/compute`);
      
      const res = Array.isArray(response) ? response[0] : (response?.content || response?.data || response || {});
      
      const scenario = scenarios.find((s) => s.scenarioId === scenarioId);
      const fallbackRegionId = scenario?.actions?.[0]?.regionId || scenario?.actions?.[0]?.district;
      const rId = res.regionId || res.region?.id || res.region?.regionId || fallbackRegionId;
      
      // Get baseline for the same region to compare
      const baseline = baselineScores.find(s => s.regionId === rId) || baselineScores[0];
      
      const parseVal = (v: any) => {
        const num = Number(v);
        if (isNaN(num)) return 0;
        return Math.abs(num) > 0 && Math.abs(num) <= 1.5 ? num * 100 : num;
      };

      const simCiri = parseVal(res.simulatedCiri ?? res.simulatedCIRI ?? res.ciriScore ?? 0);
      const simHai = parseVal(res.simulatedHai ?? res.simulatedHAI ?? res.haiScore ?? 0);
      const simRvi = parseVal(res.simulatedRvi ?? res.simulatedRVI ?? res.rviScore ?? 0);
      
      const baselineCiri = baseline?.ciriScore || 0;
      // Mathematically enforce the delta: positive means improvement (CIRI dropped), negative means deterioration (CIRI rose)
      const impactDelta = baselineCiri - simCiri;

      setSimulationResults({
        results: [{
          name: baseline?.regionName || "District",
          baselineCIRI: Number(baselineCiri.toFixed(1)),
          simulatedCIRI: Number(simCiri.toFixed(1)),
          impactDelta: Number(impactDelta.toFixed(1)),
          haiChange: Number((simHai - (baseline?.haiScore || 0)).toFixed(1)),
          rviChange: Number(((baseline?.rviScore || 0) - simRvi).toFixed(1)),
        }]
      });
    } catch (err) {
      console.error("Simulation failed", err);
    } finally {
      setIsRunning(false);
    }
  };

  if (isLoading) return <div className="p-8 text-center">Loading simulations...</div>;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 mb-2">
          Simulation Center
        </h1>
        <p className="text-slate-600">
          Model intervention impacts and compare scenarios
        </p>
      </div>

      {/* Mode Toggle */}
      <div className="bg-white rounded-xl border border-gray-200 p-6 shadow-sm">
        <div className="flex items-center justify-between">
          <div className="flex gap-2">
            <button
              onClick={() => setMode("baseline")}
              className={`px-4 py-2 rounded-lg font-medium transition ${
                mode === "baseline"
                  ? "bg-[#8B3A3A] text-white"
                  : "bg-gray-100 text-gray-700 hover:bg-gray-200"
              }`}
            >
              Baseline Simulations
            </button>
            <button
              onClick={() => setMode("scenario")}
              className={`px-4 py-2 rounded-lg font-medium transition ${
                mode === "scenario"
                  ? "bg-[#8B3A3A] text-white"
                  : "bg-gray-100 text-gray-700 hover:bg-gray-200"
              }`}
            >
              Scenario-Based Simulations
            </button>
          </div>
          {mode === "scenario" && (
            <button
              onClick={() => setShowCreateModal(true)}
              className="flex items-center gap-2 px-4 py-2 bg-[#7DD3FC] hover:bg-[#38BDF8] text-white font-medium rounded-lg transition"
            >
              <PlusIcon size={18} />
              Create New Scenario
            </button>
          )}
        </div>
      </div>

      {/* Baseline Mode */}
      {mode === "baseline" && (
        <div className="bg-white rounded-xl border border-gray-200 p-6 shadow-sm">
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-slate-900 mb-1">
              Current District Status (Baseline CIRI)
            </h2>
            <p className="text-sm text-gray-600">
              Real-world calculated data from RVI and HAI metrics
            </p>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase">District</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase">CIRI Score</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Risk Category</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase">HAI (Health Access)</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase">RVI (Vulnerability)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {baselineScores.map((score) => (
                  <tr key={score.regionId} className="hover:bg-gray-50">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div
                          className="w-2 h-2 rounded-full"
                  style={{ backgroundColor: getRiskColor(score.riskCategory) }}
                        ></div>
                        <span className="font-medium text-slate-900">{score.regionName}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-2xl font-bold text-slate-900">{(score.ciriScore || 0).toFixed(1)}</span>
                      <span className="text-sm text-gray-500">/100</span>
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className="inline-flex px-3 py-1 rounded-full text-xs font-semibold"
                        style={{
                  backgroundColor: `${getRiskColor(score.riskCategory)}20`,
                  color: getRiskColor(score.riskCategory),
                        }}
                      >
                {score.riskCategory || "LOW"}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-slate-900">{(score.haiScore || 0).toFixed(1)}/100</td>
                    <td className="px-4 py-3 text-slate-900">
                      {(score.rviScore || 0).toFixed(1)}/100
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <p className="text-sm text-blue-800">
              <strong>Note:</strong> Baseline simulations are automatically calculated from real-world data
              and updated during fixed reporting periods. These represent the current state across all 26 districts.
            </p>
          </div>
        </div>
      )}

      {/* Scenario Mode */}
      {mode === "scenario" && (
        <div className="grid lg:grid-cols-2 gap-6">
          {/* Scenarios List */}
          <div className="space-y-4">
            <h2 className="text-lg font-semibold text-slate-900">Your Scenarios</h2>
            {scenarios.map((scenario) => (
              <motion.div
                key={scenario.scenarioId}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className={`bg-white border-2 rounded-xl p-4 cursor-pointer transition ${
                  selectedScenario === scenario.scenarioId
                    ? "border-[#7DD3FC] shadow-md"
                    : "border-gray-200 hover:border-gray-300"
                }`}
                onClick={() => setSelectedScenario(scenario.scenarioId)}
              >
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <h3 className="font-semibold text-slate-900">{scenario.name}</h3>
                    <div className="flex items-center gap-3 mt-1 text-xs text-gray-600">
                      <div className="flex items-center gap-1">
                        <UserIcon size={12} />
                        User {scenario.createdByUserId}
                      </div>
                      <div className="flex items-center gap-1">
                        <ClockIcon size={12} />
                        Recently
                      </div>
                    </div>
                  </div>
                  <span
                    className={`px-2 py-1 rounded text-xs font-semibold ${
                      scenario.status === "completed"
                        ? "bg-green-100 text-green-700"
                        : scenario.status === "running"
                        ? "bg-blue-100 text-blue-700"
                        : "bg-gray-100 text-gray-700"
                    }`}
                  >
                    {scenario.status}
                  </span>
                </div>
                <div className="text-sm text-gray-600">
                  {scenario.actions.length} action{scenario.actions.length !== 1 && "s"}
                </div>
              </motion.div>
            ))}
          </div>

          {/* Scenario Details & Results */}
          <div className="space-y-6">
            {selectedScenario ? (
              <>
                {(() => {
                  const scenario = scenarios.find((s) => s.scenarioId === selectedScenario);
                  if (!scenario) return null;

                  return (
                    <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm">
                      <h2 className="text-lg font-semibold text-slate-900 mb-4">Scenario Actions</h2>
                      <div className="space-y-2 mb-6">
                        {scenario.actions.map((action) => {
                          const regionId = action.regionId || action.district;
                          const region = baselineScores.find((r) => r.regionId === regionId);
                          const delta = action.numericDelta ?? (Number(action.newValue) - Number(action.oldValue));
                          return (
                            <div
                              key={action.id || action.actionId}
                              className="p-3 bg-gray-50 rounded-lg border border-gray-200"
                            >
                              <div className="flex items-center justify-between">
                                <div>
                                  <span className="font-medium text-slate-900">{action.actionType || action.metricType}</span>
                                  <span className="text-gray-600 mx-2">in</span>
                                  <span className="font-medium text-slate-900">{region?.regionName || "Unknown"}</span>
                                </div>
                                <div className="flex items-center gap-2 text-sm">
                                  <span className="text-gray-600">{action.oldValue}</span>
                                  <span className="text-gray-400">→</span>
                                  <span className="font-semibold text-[#7DD3FC]">{action.newValue}</span>
                                  <span className={`${delta > 0 ? 'text-green-600' : delta < 0 ? 'text-red-600' : 'text-gray-600'} font-semibold`}>
                                    ({delta > 0 ? '+' : ''}{delta})
                                  </span>
                                </div>
                              </div>
                            </div>
                          );
                        })}
                      </div>

                      <button
                        onClick={() => runScenarioSimulation(selectedScenario)}
                        disabled={isRunning || scenario.status === "running"}
                        className="w-full py-3 bg-[#8B3A3A] hover:bg-[#6B2A2A] disabled:bg-gray-400 text-white font-medium rounded-lg transition flex items-center justify-center gap-2"
                      >
                        {isRunning ? (
                          <>
                            <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                            <span>Running Simulation...</span>
                          </>
                        ) : (
                          <>
                            <PlayIcon size={18} />
                            <span>Run Simulation</span>
                          </>
                        )}
                      </button>
                    </div>
                  );
                })()}

                {/* Results */}
                {simulationResults && (
                  <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm"
                  >
                    <h2 className="text-lg font-semibold text-slate-900 mb-4">
                      Before vs After Comparison
                    </h2>

                    <ResponsiveContainer width="100%" height={250}>
                      <BarChart data={simulationResults.results}>
                        <CartesianGrid key="grid-bar" strokeDasharray="3 3" stroke="#e5e7eb" />
                        <XAxis key="xaxis-bar" dataKey="name" stroke="#6b7280" fontSize={12} />
                        <YAxis key="yaxis-bar" stroke="#6b7280" fontSize={12} />
                        <Tooltip
                          key="tooltip-bar"
                          contentStyle={{
                            backgroundColor: "#fff",
                            border: "1px solid #e5e7eb",
                            borderRadius: "8px",
                          }}
                        />
                        <Legend key="legend-bar" />
                        <Bar
                          key="bar-baseline"
                          dataKey="baselineCIRI"
                          fill="#9ca3af"
                          name="Baseline CIRI"
                        />
                        <Bar
                          key="bar-simulated"
                          dataKey="simulatedCIRI"
                          fill="#7DD3FC"
                          name="Simulated CIRI"
                        />
                      </BarChart>
                    </ResponsiveContainer>

                    <div className="mt-6 space-y-3">
                      {simulationResults.results.map((result: any) => {
                        const isImprovement = result.impactDelta > 0;
                        const isWorse = result.impactDelta < 0;
                        const deltaColor = isImprovement ? "text-green-600" : isWorse ? "text-red-600" : "text-gray-500";
                        const DeltaIcon = isImprovement ? TrendingDownIcon : TrendingUpIcon;

                        return (
                          <div
                            key={result.name}
                            className="p-4 bg-gray-50 rounded-lg border border-gray-200"
                          >
                            <div className="flex items-center justify-between mb-2">
                              <span className="font-semibold text-slate-900">{result.name}</span>
                              <div className={`flex items-center gap-1 text-sm font-semibold ${deltaColor}`}>
                                {result.impactDelta !== 0 && <DeltaIcon size={14} />}
                                <span>{result.impactDelta > 0 ? '+' : ''}{result.impactDelta.toFixed(1)} CIRI Impact</span>
                              </div>
                            </div>
                            <div className="grid grid-cols-3 gap-4 text-sm">
                              <div>
                                <div className="text-gray-600">Baseline</div>
                                <div className="font-semibold text-slate-900">{result.baselineCIRI}</div>
                              </div>
                              <div>
                                <div className="text-gray-600">Simulated</div>
                                <div className="font-semibold text-[#7DD3FC]">{result.simulatedCIRI}</div>
                              </div>
                              <div>
                                <div className="text-gray-600">{result.impactDelta > 0 ? "Improvement" : result.impactDelta < 0 ? "Deterioration" : "Impact"}</div>
                                <div className={`font-semibold ${deltaColor}`}>{result.impactDelta > 0 ? '+' : ''}{result.impactDelta.toFixed(1)}</div>
                              </div>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </motion.div>
                )}
              </>
            ) : (
              <div className="bg-white border border-gray-200 rounded-xl p-6 h-full flex items-center justify-center">
                <p className="text-gray-500 text-center">
                  Select a scenario to view details and run simulation
                </p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Create Scenario Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-white rounded-xl p-6 max-w-2xl w-full max-h-[90vh] overflow-y-auto"
          >
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-xl font-bold text-slate-900">Create New Scenario</h2>
              <button
                onClick={() => setShowCreateModal(false)}
                className="p-2 hover:bg-gray-100 rounded-lg"
              >
                <XIcon size={20} />
              </button>
            </div>

            <div className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  Scenario Name
                </label>
                <input
                  type="text"
                  value={newScenarioName}
                  onChange={(e) => setNewScenarioName(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#7DD3FC]"
                  placeholder="e.g., Healthcare Expansion - Q2 2026"
                />
              </div>

              <div className="border-t border-gray-200 pt-6">
                <h3 className="font-semibold text-slate-900 mb-4">Add Actions</h3>

                <div className="grid grid-cols-2 gap-4 mb-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">
                      Metric Type
                    </label>
                    <select
                      value={actionMetric}
                      onChange={(e) => setActionMetric(e.target.value)}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#7DD3FC]"
                    >
                      {metricTypes.map((mt) => (
                        <option key={mt.value} value={mt.value}>
                          {mt.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">
                      District
                    </label>
                    <select
                      value={actionDistrict}
                      onChange={(e) => setActionDistrict(Number(e.target.value))}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#7DD3FC]"
                    >
                      {baselineScores.map((r) => (
                        <option key={r.regionId} value={r.regionId}>
                          {r.regionName}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">
                      Current Value
                    </label>
                    <div className="w-full px-4 py-2 border border-gray-200 bg-gray-50 rounded-lg text-slate-700 font-medium">
                      {actionOldValue !== "" ? actionOldValue : 0}
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">
                      New Value
                    </label>
                    <input
                      type="number"
                      value={actionNewValue}
                      onChange={(e) => setActionNewValue(Number(e.target.value))}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#7DD3FC]"
                    />
                  </div>
                </div>

                <button
                  onClick={handleAddAction}
                  className="w-full py-2 bg-gray-100 hover:bg-gray-200 text-slate-900 font-medium rounded-lg transition"
                >
                  + Add Action
                </button>

                {newScenarioActions.length > 0 && (
                  <div className="mt-4 space-y-2">
                    <h4 className="text-sm font-semibold text-slate-900">Actions Added:</h4>
                    {newScenarioActions.map((action) => {
                      const regionId = action.regionId || action.district;
                      const region = baselineScores.find((r) => r.regionId === regionId);
                      const delta = action.numericDelta ?? (Number(action.newValue) - Number(action.oldValue));
                      return (
                        <div
                          key={action.id || action.actionId}
                          className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                        >
                          <div className="text-sm">
                            <span className="font-medium">{action.metricType}</span> in{" "}
                            <span className="font-medium">{region?.regionName || "Unknown"}</span>:{" "}
                            {action.oldValue} → {action.newValue}{" "}
                            <span className={`${delta > 0 ? 'text-green-600' : delta < 0 ? 'text-red-600' : 'text-gray-600'} font-semibold`}>
                              ({delta > 0 ? '+' : ''}{delta})
                            </span>
                          </div>
                          <button
                            onClick={() => handleRemoveAction(action.id)}
                            className="p-1 hover:bg-gray-200 rounded"
                          >
                            <XIcon size={16} />
                          </button>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              <div className="flex gap-3 pt-4">
                <button
                  onClick={handleCreateScenario}
                  disabled={!newScenarioName || newScenarioActions.length === 0}
                  className="flex-1 py-3 bg-[#8B3A3A] hover:bg-[#6B2A2A] disabled:bg-gray-400 text-white font-medium rounded-lg transition"
                >
                  Create Scenario
                </button>
                <button
                  onClick={() => setShowCreateModal(false)}
                  className="px-6 py-3 bg-gray-100 hover:bg-gray-200 text-slate-900 font-medium rounded-lg transition"
                >
                  Cancel
                </button>
              </div>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}
