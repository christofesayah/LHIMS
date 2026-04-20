import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import { motion } from "motion/react";
import { SearchIcon, MapPinIcon, AlertCircleIcon } from "lucide-react";
import { useApi } from "../hooks/useApi";

const getRiskColor = (risk: string) => {
  const r = (risk || "").toUpperCase();
  if (r === "CRITICAL" || r === "HIGH") return "#DC2626";
  if (r === "SEVERE" || r === "MEDIUM") return "#EA580C";
  return "#10B981";
};

export default function DistrictExplorer() {
  const [searchTerm, setSearchTerm] = useState("");
  const [scores, setScores] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();
  const api = useApi();

  useEffect(() => {
    const fetchScores = async () => {
      try {
        let districtsData: any = [];
        
        try { districtsData = await api.get("/api/districts"); } catch(e) {
          try { districtsData = await api.get("/api/regions"); } catch(e2) {}
        }

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
        
        setScores(mapped);
      } catch (err) {
        console.error("Failed to fetch scores", err);
      } finally {
        setIsLoading(false);
      }
    };
    fetchScores();
  }, []);

  const filteredRegions = scores.filter((score) =>
    (score.regionName || "").toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handlePanToDistrict = (districtId: number, e: React.MouseEvent) => {
    e.stopPropagation(); // Prevent row click from firing
    navigate(`/app/dashboard?district=${districtId}`);
  };

  const handleRowClick = (districtId: number) => {
    navigate(`/app/region/${districtId}`);
  };

  if (isLoading) return <div className="p-8 text-center">Loading district data...</div>;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 mb-2">
          District Explorer
        </h1>
        <p className="text-slate-600">
          Browse and explore all districts of Lebanon
        </p>
      </div>

      {/* Search Bar */}
      <div className="bg-white rounded-xl border border-gray-200 p-4">
        <div className="relative">
          <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search districts..."
            className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#7DD3FC] focus:border-transparent"
          />
        </div>
      </div>

      {/* Districts Table */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  District Name
                </th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  HAI Score
                </th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  RVI Score
                </th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  Risk Level
                </th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  Overall Score
                </th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  Action
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filteredRegions.map((score, index) => (
                <motion.tr
                  key={score.regionId}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                  onClick={() => handleRowClick(score.regionId)}
                  className="hover:bg-gray-50 transition cursor-pointer"
                >
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center gap-3">
                      <div
                        className="w-3 h-3 rounded-full"
                        style={{ backgroundColor: getRiskColor(score.riskCategory) }}
                      ></div>
                      <span className="font-medium text-slate-900">{score.regionName}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {(score.haiScore || 0).toFixed(1)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {(score.rviScore || 0).toFixed(1)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span
                      className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold"
                      style={{
                        backgroundColor: `${getRiskColor(score.riskCategory)}20`,
                        color: getRiskColor(score.riskCategory),
                      }}
                    >
                      <AlertCircleIcon size={12} />
                      {score.riskCategory || "LOW"}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-semibold text-slate-900">
                        {(score.ciriScore || 0).toFixed(1)}
                      </span>
                      <span className="text-xs text-gray-500">/100</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <button
                      onClick={(e) => handlePanToDistrict(score.regionId, e)}
                      className="inline-flex items-center gap-2 px-4 py-2 bg-[#8B3A3A] hover:bg-[#6B2A2A] text-white text-sm font-medium rounded-lg transition"
                    >
                      <MapPinIcon size={16} />
                      View on Map
                    </button>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>

        {filteredRegions.length === 0 && (
          <div className="text-center py-12">
            <p className="text-gray-500">No districts found matching your search.</p>
          </div>
        )}
      </div>

      {/* Summary Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <p className="text-sm text-gray-600 mb-1">Total Districts</p>
          <p className="text-3xl font-bold text-slate-900">{scores.length}</p>
        </div>
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <p className="text-sm text-gray-600 mb-1">High Risk</p>
          <p className="text-3xl font-bold text-red-600">
            {scores.filter((s) => ["HIGH", "CRITICAL"].includes(s.riskCategory?.toUpperCase())).length}
          </p>
        </div>
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <p className="text-sm text-gray-600 mb-1">Medium Risk</p>
          <p className="text-3xl font-bold text-orange-600">
            {scores.filter((s) => ["MEDIUM", "SEVERE"].includes(s.riskCategory?.toUpperCase())).length}
          </p>
        </div>
      </div>
    </div>
  );
}
