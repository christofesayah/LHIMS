import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router";
import { motion } from "motion/react";
import {
  ArrowLeftIcon,
  AlertCircleIcon,
  UsersIcon,
  HeartPulseIcon,
  TrendingUpIcon,
  TrendingDownIcon,
} from "lucide-react";
import {
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
  LineChart,
  Line,
  AreaChart,
  Area
} from "recharts";
import { useApi } from "../hooks/useApi";

const getRiskColor = (risk: string) => {
  const r = (risk || "").toUpperCase();
  if (r === "CRITICAL" || r === "HIGH") return "#DC2626";
  if (r === "SEVERE" || r === "MEDIUM") return "#EA580C";
  return "#10B981";
};

const historicalData = [
  { month: 'Jan', refugeePressure: 65, poverty: 70, healthcareAccess: 45 },
  { month: 'Feb', refugeePressure: 68, poverty: 72, healthcareAccess: 48 },
  { month: 'Mar', refugeePressure: 75, poverty: 75, healthcareAccess: 42 },
  { month: 'Apr', refugeePressure: 80, poverty: 78, healthcareAccess: 38 },
  { month: 'May', refugeePressure: 82, poverty: 80, healthcareAccess: 35 },
  { month: 'Jun', refugeePressure: 85, poverty: 85, healthcareAccess: 30 },
];

export default function RegionDetails() {
  const { regionId } = useParams();
  const navigate = useNavigate();
  const api = useApi();
  const [score, setScore] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (regionId) {
      api.get(`/api/districts/${regionId}`).catch(() => ({}))
        .then(async districtData => {
          // Try fallback to /api/regions if the first object is empty
          if (!districtData || Object.keys(districtData).length === 0) {
            districtData = await api.get(`/api/regions/${regionId}`).catch(() => ({}));
          }
          let scoreData = await api.get(`/api/scores/${regionId}`).catch(() => null);
          if (!scoreData) {
            scoreData = await api.get(`/api/districts/${regionId}/scores`).catch(() => ({}));
          }
          
          const vals = districtData?._embedded ? (Object.values(districtData._embedded) as any[]) : [];
          const d = districtData?.content || districtData?.data || (vals.length > 0 ? vals[0]?.[0] : null) || districtData;
          let sRaw = scoreData?.content || scoreData?.data || (scoreData?._embedded ? Object.values(scoreData._embedded)[0] : null) || scoreData;
          const s = Array.isArray(sRaw) ? (sRaw[0] || {}) : (sRaw || {});

          const combined = { ...s, ...d };
          if (combined.id || combined.regionId) {
            const sc = combined.latestScore || combined.score || combined.computedScore || combined;
            const rawCiri = d?.ciriScore || s?.ciriScore || sc.ciriScore || 0;
            const rawHai = d?.haiScore || s?.haiScore || sc.haiScore || 0;
            const rawRvi = d?.rviScore || s?.rviScore || sc.rviScore || 0;
            const risk = [d?.riskCategory, s?.riskCategory, sc.riskCategory].find(r => r && r !== "LOW") || combined.riskCategory || "LOW";
            
            const parseScore = (val: any) => {
              const num = Number(val);
              if (isNaN(num)) return 0;
              let percentage = num;
              if (Math.abs(num) > 0 && Math.abs(num) <= 1.5) percentage = num * 100;
            return Math.max(0, percentage);
          };
          
          setScore({
            ...combined, ...sc,
            regionId: combined.regionId || combined.id || sc.regionId || sc.id,
            regionName: combined.name || combined.regionName || sc.name || sc.regionName || `District ${combined.regionId || combined.id || '?'}`,
            population: combined.population,
            ciriScore: parseScore(rawCiri),
            haiScore: parseScore(rawHai),
            rviScore: parseScore(rawRvi),
            riskCategory: risk ? risk.toUpperCase() : "LOW"
          });
        } else {
          setError("Region not found");
        }
      })
      .catch(err => setError("Failed to load region data"))
      .finally(() => setIsLoading(false));
    }
  }, [regionId]);

  if (!score || error) {
    return (
      <div className="text-center text-slate-900 p-8">
        <p>{error || "Region not found"}</p>
        <button
          onClick={() => navigate("/app/dashboard")}
          className="mt-4 text-[#8B3A3A] hover:text-[#6B2A2A] font-medium"
        >
          Back to Dashboard
        </button>
      </div>
    );
  }

  const stats: Array<{label: string, value: string | number, max: number, color: string, icon: any, status?: string, trend?: number}> = [
    {
      label: "Overall Status",
      value: (score.ciriScore || 0).toFixed(1),
      max: 100,
      color: getRiskColor(score.riskCategory),
      icon: AlertCircleIcon,
      status: score.riskCategory || "LOW",
    },
    {
      label: "Vulnerability (RVI)",
      value: (score.rviScore || 0).toFixed(1),
      max: 100,
      color: "#DC2626",
      icon: UsersIcon,
    },
    {
      label: "Healthcare Access (HAI)",
      value: (score.haiScore || 0).toFixed(1),
      max: 100,
      color: "#7DD3FC",
      icon: HeartPulseIcon,
    },
  ];

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <button
            onClick={() => navigate("/app/dashboard")}
            className="flex items-center gap-2 text-gray-600 hover:text-slate-900 transition mb-4"
          >
            <ArrowLeftIcon size={18} />
            <span>Back to Dashboard</span>
          </button>
          <h1 className="text-3xl font-bold text-slate-900 mb-2">{score.regionName || `Region ${regionId}`}</h1>
          <p className="text-gray-600">
            Last updated: {new Date(score.lastComputedAt).toLocaleString()}
          </p>
        </div>
        <div className="text-right">
          <div className="text-sm text-gray-600 mb-1">Overall Score</div>
          <div
            className="text-4xl font-bold"
            style={{ color: getRiskColor(score.riskCategory) }}
          >
            {(score.ciriScore || 0).toFixed(1)}
            <span className="text-2xl text-gray-500">/100</span>
          </div>
          <div
            className="text-sm font-semibold"
            style={{ color: getRiskColor(score.riskCategory) }}
          >
            {score.riskCategory || "LOW"}
          </div>
        </div>
      </div>

      <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
        {stats.map((stat, index) => {
          const Icon = stat.icon;
          const isHealthcare = stat.label.includes("Healthcare");
          const isPositive = stat.trend && stat.trend > 0;
          return (
            <motion.div
              key={stat.label}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.1 }}
              className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm"
            >
              <div className="flex items-center justify-between mb-4">
                <div
                  className="p-3 rounded-lg"
                  style={{ backgroundColor: `${stat.color}20` }}
                >
                  <Icon size={24} style={{ color: stat.color }} />
                </div>
                {stat.trend && (
                  <div
                    className={`flex items-center gap-1 text-sm font-semibold ${
                      (isHealthcare && isPositive) ||
                      (!isHealthcare && !isPositive)
                        ? "text-green-500"
                        : "text-red-500"
                    }`}
                  >
                    {isPositive ? (
                      <TrendingUpIcon size={14} />
                    ) : (
                      <TrendingDownIcon size={14} />
                    )}
                    <span>{Math.abs(stat.trend)}%</span>
                  </div>
                )}
              </div>
              <div className="mb-2">
                <div className="text-3xl font-bold text-slate-900 mb-1">
                  {stat.value}
                  <span className="text-xl text-gray-500">/{stat.max}</span>
                </div>
                <div className="text-sm text-gray-600">{stat.label}</div>
                {stat.status && (
                  <div
                    className="text-sm font-semibold mt-1"
                    style={{ color: stat.color }}
                  >
                    {stat.status}
                  </div>
                )}
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2 mt-4">
                <motion.div
                  initial={{ width: 0 }}
                  animate={{ width: `${(Number(stat.value) / stat.max) * 100}%` }}
                  transition={{ duration: 0.8, delay: index * 0.1 }}
                  className="h-2 rounded-full"
                  style={{ backgroundColor: stat.color }}
                ></motion.div>
              </div>
            </motion.div>
          );
        })}
      </div>

      <div className="grid lg:grid-cols-2 gap-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm"
        >
          <h2 className="text-xl font-semibold text-slate-900 mb-6">
            Trends Over Time
          </h2>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={historicalData}>
              <CartesianGrid key="grid-trends" strokeDasharray="3 3" stroke="#e5e7eb" />
              <XAxis key="xaxis-trends" dataKey="month" stroke="#6b7280" fontSize={12} />
              <YAxis key="yaxis-trends" stroke="#6b7280" fontSize={12} />
              <Tooltip
                key="tooltip-trends"
                contentStyle={{
                  backgroundColor: "#ffffff",
                  border: "1px solid #e5e7eb",
                  borderRadius: "8px",
                  color: "#1e293b",
                }}
              />
              <Legend key="legend-trends" />
              <Line
                key="line-refugee"
                type="monotone"
                dataKey="refugeePressure"
                stroke="#DC2626"
                strokeWidth={2}
                name="Demographic Pressure"
                dot={false}
              />
              <Line
                key="line-poverty"
                type="monotone"
                dataKey="poverty"
                stroke="#EA580C"
                strokeWidth={2}
                name="Poverty"
                dot={false}
              />
              <Line
                key="line-healthcare"
                type="monotone"
                dataKey="healthcareAccess"
                stroke="#7DD3FC"
                strokeWidth={2}
                name="Healthcare Access"
                dot={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm"
        >
          <h2 className="text-xl font-semibold text-slate-900 mb-6">
            Demographic Pressure Distribution
          </h2>
          <ResponsiveContainer width="100%" height={300}>
            <AreaChart data={historicalData}>
              <defs key="defs-area">
                <linearGradient id="colorRefugee" x1="0" y1="0" x2="0" y2="1">
                  <stop key="stop-start" offset="5%" stopColor="#DC2626" stopOpacity={0.8} />
                  <stop key="stop-end" offset="95%" stopColor="#DC2626" stopOpacity={0.1} />
                </linearGradient>
              </defs>
              <CartesianGrid key="grid-area" strokeDasharray="3 3" stroke="#e5e7eb" />
              <XAxis key="xaxis-area" dataKey="month" stroke="#6b7280" fontSize={12} />
              <YAxis key="yaxis-area" stroke="#6b7280" fontSize={12} />
              <Tooltip
                key="tooltip-area"
                contentStyle={{
                  backgroundColor: "#ffffff",
                  border: "1px solid #e5e7eb",
                  borderRadius: "8px",
                  color: "#1e293b",
                }}
              />
              <Area
                key="area-refugee"
                type="monotone"
                dataKey="refugeePressure"
                stroke="#DC2626"
                fillOpacity={1}
                fill="url(#colorRefugee)"
                strokeWidth={2}
              />
            </AreaChart>
          </ResponsiveContainer>
        </motion.div>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.6 }}
        className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm"
      >
        <h2 className="text-xl font-semibold text-slate-900 mb-4">Key Insights</h2>
        <div className="grid md:grid-cols-2 gap-6">
          <div className="space-y-2">
            <div className="text-sm text-gray-600">Priority Level</div>
            <div
              className="text-2xl font-bold"
            style={{ color: getRiskColor(score.riskCategory) }}
            >
            {score.riskCategory || "LOW"}
            </div>
            <p className="text-sm text-gray-500">
              Based on composite humanitarian indicators
            </p>
          </div>
          <div className="space-y-2">
            <div className="text-sm text-gray-600">Intervention Need</div>
            <div className="text-2xl font-bold text-slate-900">
              {(score.ciriScore || 0) >= 80
                ? "Immediate"
                : (score.ciriScore || 0) >= 50
                ? "High"
                : "Moderate"}
            </div>
            <p className="text-sm text-gray-500">
              Recommended response timeline
            </p>
          </div>
        </div>
      </motion.div>
    </div>
  );
}
