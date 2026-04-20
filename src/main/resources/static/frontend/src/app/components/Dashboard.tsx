import { useState, useEffect } from "react";
import { useNavigate, useSearchParams, useOutletContext } from "react-router";
import { motion } from "motion/react";
import { AlertCircleIcon, UsersIcon, HeartPulseIcon, Building2, MapIcon, TrendingUpIcon } from "lucide-react";
import { useApi } from "../hooks/useApi";
import { useAuth } from "../context/AuthContext";
import { MapContainer, TileLayer, GeoJSON, CircleMarker, Tooltip } from "react-leaflet";
// @ts-expect-error - Leaflet CSS import lacks TypeScript declarations
import "leaflet/dist/leaflet.css";
import L from "leaflet";

const getRiskColor = (risk: string) => {
  const r = (risk || "").toUpperCase();
  if (r === "CRITICAL" || r === "HIGH") return "#DC2626";
  if (r === "SEVERE" || r === "MEDIUM") return "#EA580C";
  return "#10B981";
};

type MapMode = "districts" | "hospitals";

interface ScoreResponse {
  regionId: number;
  regionName: string;
  haiScore: number;
  rviScore: number;
  ciriScore: number;
  riskCategory: string;
  population?: number;
}

interface KeyInsights {
  highestRisk: ScoreResponse;
  highestVulnerability: ScoreResponse;
  lowestAccess: ScoreResponse;
  totalHighRiskDistricts: number;
  totalMediumRiskDistricts: number;
  totalLowRiskDistricts: number;
}

export default function Dashboard() {
  const [searchParams] = useSearchParams();
  const { searchQuery } = useOutletContext<{ searchQuery: string }>();
  const [hoveredRegion, setHoveredRegion] = useState<number | null>(null);
  const [hoveredHospitalData, setHoveredHospitalData] = useState<any>(null);
  const [scores, setScores] = useState<ScoreResponse[]>([]);
  const [insights, setInsights] = useState<KeyInsights | null>(null);
  const [hospitals, setHospitals] = useState<any[]>([]);
  const [mapMode, setMapMode] = useState<MapMode>("districts");
  const [geoData, setGeoData] = useState<any>(null);
  const [selectedDistrict, setSelectedDistrict] = useState<number | null>(
    searchParams.get("district") ? parseInt(searchParams.get("district")!) : null
  );
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();
  const api = useApi();
  const { user } = useAuth();

  useEffect(() => {
    const fetchData = async () => {
      setIsLoading(true);

      let loadedDistricts: any[] = [];

      // 1. Map Boundaries
      try {
        let geoResponse = null;
        
        const paths = [
          '/geoBoundaries-LBN-ADM2_simplified.geojson',
          '/frontend/public/geoBoundaries-LBN-ADM2_simplified.geojson',
          'geoBoundaries-LBN-ADM2_simplified.geojson',
          '/data/geo/adm2/geoBoundaries-LBN-ADM2_simplified.geojson',
          '/data/geo/geoBoundaries-LBN-ADM2_simplified.geojson'
        ];

        for (const path of paths) {
          if (geoResponse) break;
          try {
            const res = await fetch(path, {
              headers: {
                ...(localStorage.getItem('token') ? { 'Authorization': `Bearer ${localStorage.getItem('token')}` } : {})
              }
            });
            if (res.ok) {
              const text = await res.text();
              if (text.trim().startsWith('{')) geoResponse = JSON.parse(text);
            }
          } catch (e) {}
        }

        if (!geoResponse) {
          try {
            const res = await fetch('https://raw.githubusercontent.com/wmgeolab/geoBoundaries/main/releaseData/gbOpen/LBN/ADM2/geoBoundaries-LBN-ADM2_simplified.geojson');
            if (res.ok) {
              const text = await res.text();
              if (text.trim().startsWith('{')) geoResponse = JSON.parse(text);
            }
          } catch(e) {}
        }
        
        if (geoResponse) {
          setGeoData(geoResponse);
        } else {
          setGeoData({ type: "FeatureCollection", features: [] });
        }
      } catch (err) {
        console.error("Failed to fetch map boundaries", err);
        setGeoData({ type: "FeatureCollection", features: [] });
      }

      // 2. Scores
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
        
        loadedDistricts = mapped;
        setScores(mapped);
      } catch (err) { console.error("Failed to fetch scores", err); }

      // 3. Insights
      try {
        const insightsData = await api.get("/api/scores/insights");
        if (insightsData) {
          const formatScore = (s: any) => {
            if (!s) return null;
            const sc = s.latestScore || s.score || s.computedScore || s;
            const rawCiri = s.ciriScore || sc.ciriScore || s.overallScore || sc.overallScore || 0;
            const rawHai = s.haiScore || sc.haiScore || s.accessScore || s.hai || sc.hai || 0;
            const rawRvi = s.rviScore || sc.rviScore || s.vulnerabilityScore || s.rvi || sc.rvi || 0;
            const risk = [s.riskCategory, sc.riskCategory].find(r => r && r !== "LOW") || s.riskCategory || "LOW";
            
            const parseScore = (val: any) => {
              const num = Number(val);
              if (isNaN(num)) return 0;
              let percentage = num;
              if (Math.abs(num) > 0 && Math.abs(num) <= 1.5) percentage = num * 100;
              return Math.max(0, percentage);
            };

            return {
              ...s, ...sc,
              regionId: s.regionId || s.region?.regionId,
              regionName: s.regionName || s.region?.name || s.region?.regionName || `District ${s.regionId || s.region?.regionId || '?'}`,
              population: s.population || s.region?.population,
              ciriScore: parseScore(rawCiri),
              haiScore: parseScore(rawHai),
              rviScore: parseScore(rawRvi),
              riskCategory: risk
            };
          };
          setInsights({
            ...insightsData,
            highestRisk: formatScore(insightsData.highestRisk),
            highestVulnerability: formatScore(insightsData.highestVulnerability),
            lowestAccess: formatScore(insightsData.lowestAccess),
          });
        }
      } catch (err) { console.error("Failed to fetch insights", err); }

      // 4. Hospitals (Filtered for valid coordinates)
      try {
        let hospitalsData = await api.get("/api/facilities").catch(() => null);
        if (!hospitalsData) hospitalsData = await api.get("/api/healthFacilities").catch(() => []);
        
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
        let hospitalsArrRaw = extractArray(hospitalsData);
        
        const hospitalsMapped = await Promise.all(hospitalsArrRaw.map(async (h: any) => {
          const hId = h.facilityId || h.id;
          let cap = h.capacity || h.latestCapacity;
          
          if (!cap && hId) {
            let capData = await api.get(`/api/facilities/${hId}/capacity`).catch(() => null);
            if (!capData) capData = await api.get(`/api/healthFacilities/${hId}/capacities`).catch(() => null);
            if (!capData) capData = await api.get(`/api/facilities/${hId}/capacities`).catch(() => null);
            
            let cRaw = capData?.content || capData?.data || (capData?._embedded ? Object.values(capData._embedded)[0] : null) || capData;
            cap = Array.isArray(cRaw) ? (cRaw[0] || {}) : (cRaw || {});
          }
          
          // Cross-reference the region_id foreign key with the fetched CAZA districts
          const district = loadedDistricts.find((d: any) => d.regionId === h.regionId || d.regionId === h.region?.regionId);

          return {
            ...h,
            regionName: district?.regionName || h.regionName || h.region?.name || h.region?.regionName || "Unknown Region",
            latestCapacity: {
              totalBeds: cap?.totalBeds ?? cap?.total_beds ?? 0,
              icuBeds: cap?.icuBeds ?? cap?.icu_beds ?? 0,
              doctorsCount: cap?.doctorsCount ?? cap?.doctors_count ?? 0,
              nursesCount: cap?.nursesCount ?? cap?.nurses_count ?? 0
            }
          };
        }));
        
        const validHospitals = hospitalsMapped.filter((h: any) => {
          return h.latitude != null && h.longitude != null && !isNaN(Number(h.latitude)) && !isNaN(Number(h.longitude));
        });

        setHospitals(validHospitals);
      } catch (err) { console.error("Failed to fetch facilities", err); }

      setIsLoading(false);
    };
    fetchData();
  }, []);

  const handleRegionClick = (regionId: number) => {
    navigate(`/app/region/${regionId}`);
  };

  const hoveredData = hoveredRegion
    ? scores.find((s) => s.regionId === hoveredRegion)
    : null;

  const districtCoords: Record<string, [number, number]> = {
    "Akkar": [34.53, 36.12],
    "Tripoli": [34.43, 35.83],
    "Minieh-Danniyeh": [34.38, 35.98],
    "Zgharta": [34.33, 35.90],
    "Bcharre": [34.25, 36.02],
    "Koura": [34.30, 35.82],
    "Batroun": [34.25, 35.65],
    "Jbeil": [34.12, 35.65],
    "Keserwan": [34.02, 35.63],
    "Metn": [33.90, 35.58],
    "Beirut": [33.89, 35.50],
    "Baabda": [33.83, 35.53],
    "Aley": [33.80, 35.60],
    "Chouf": [33.70, 35.58],
    "Jezzine": [33.53, 35.58],
    "Zahle": [33.85, 35.90],
    "West Bekaa": [33.63, 35.80],
    "Rashaya": [33.50, 35.85],
    "Baalbek": [34.00, 36.20],
    "Hermel": [34.38, 36.38],
    "Sidon": [33.55, 35.38],
    "Tyre": [33.27, 35.20],
    "Nabatieh": [33.38, 35.48],
    "Bint Jbeil": [33.12, 35.43],
    "Marjeyoun": [33.35, 35.58],
    "Hasbaya": [33.40, 35.68],
  };

  const getRegionScore = (feature: any) => {
    const featureName = (feature.properties?.shapeName || feature.properties?.ADM2_EN || feature.properties?.admin2Name_en || feature.properties?.name || "").toLowerCase();
    if (!featureName) return undefined;
    
    const normalize = (name: string) => {
      let n = name.toLowerCase().replace(/[^a-z0-9]/g, '');
      if (n.includes('metn') || n.includes('meten') || n.includes('matn')) return 'meten';
      if (n.includes('kesrouan') || n.includes('kesrwan') || n.includes('keserwan')) return 'kesrwane';
      if (n.includes('batroun')) return 'batroun';
      if (n.includes('koura')) return 'koura';
      if (n.includes('minieh') || n.includes('dennie') || n.includes('danniyeh') || n.includes('dinnieh')) return 'miniehdennie';
      if (n.includes('bcharre') || n.includes('bsharri')) return 'bcharre';
      if (n.includes('bintjb') || n.includes('bentjb')) return 'bentjbeil';
      if (n.includes('nabati')) return 'nabatieh';
      if (n.includes('saida') || n.includes('sidon')) return 'saida';
      if (n.includes('sour') || n.includes('tyre')) return 'sour';
      if (n.includes('hermel')) return 'hermel';
      if (n.includes('westbekaa') || n.includes('westernbaqaa') || n.includes('bekaawest')) return 'westbekaa';
      if (n.includes('rachaya') || n.includes('rashaya')) return 'rachaya';
      if (n.includes('jbeil') || n.includes('jbail')) return 'jbeil';
      return n.replace(/^el/, '').replace(/^al/, '');
    };

    const normalizedFeature = normalize(featureName);

    return scores.find(s => {
      const rName = (s.regionName || "").toLowerCase();
      if (!rName) return false;
      return normalize(rName) === normalizedFeature;
    });
  };

  const geoJsonStyle = (feature: any) => {
    const score = getRegionScore(feature);
    const isSelected = score && selectedDistrict === score.regionId;
    return {
      fillColor: score ? getRiskColor(score.riskCategory) : '#e2e8f0',
      weight: isSelected ? 3 : 1,
      opacity: 1,
      color: isSelected ? '#1e40af' : 'white',
      fillOpacity: 0.7
    };
  };

  const onEachFeature = (feature: any, layer: L.Layer) => {
    const score = getRegionScore(feature);
    layer.on({
      mouseover: (e) => {
        const target = e.target;
        target.setStyle({ weight: 3, color: '#1e40af', fillOpacity: 0.9 });
        target.bringToFront();
        if (score) setHoveredRegion(score.regionId);
      },
      mouseout: (e) => {
        const target = e.target;
        const isSelected = score && selectedDistrict === score.regionId;
        target.setStyle({
          weight: isSelected ? 3 : 1,
          color: isSelected ? '#1e40af' : 'white',
          fillOpacity: 0.7
        });
        setHoveredRegion(null);
      },
      click: () => {
        if (score) handleRegionClick(score.regionId);
      }
    });
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 mb-2">
          Regional Overview
        </h1>
        <p className="text-slate-600">
          Click on a region to view detailed statistics and trends
        </p>
      </div>

      {/* Key Insights Stats */}
      {insights && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <motion.div 
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm"
          >
            <div className="flex items-center gap-3 mb-2">
              <div className="p-2 bg-red-100 rounded-lg">
                <AlertCircleIcon className="text-red-600" size={20} />
              </div>
              <span className="text-sm font-medium text-slate-600">Highest Risk</span>
            </div>
            <p className="text-lg font-bold text-slate-900">{insights.highestRisk?.regionName || "N/A"}</p>
            <p className="text-xs text-slate-500 mt-1">CIRI Score: {(insights.highestRisk?.ciriScore || 0).toFixed(1)}</p>
          </motion.div>

          <motion.div 
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm"
          >
            <div className="flex items-center gap-3 mb-2">
              <div className="p-2 bg-orange-100 rounded-lg">
                <TrendingUpIcon className="text-orange-600" size={20} />
              </div>
              <span className="text-sm font-medium text-slate-600">Most Vulnerable</span>
            </div>
            <p className="text-lg font-bold text-slate-900">{insights.highestVulnerability?.regionName || "N/A"}</p>
            <p className="text-xs text-slate-500 mt-1">RVI Score: {(insights.highestVulnerability?.rviScore || 0).toFixed(1)}</p>
          </motion.div>

          <motion.div 
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm"
          >
            <div className="flex items-center gap-3 mb-2">
              <div className="p-2 bg-blue-100 rounded-lg">
                <HeartPulseIcon className="text-blue-600" size={20} />
              </div>
              <span className="text-sm font-medium text-slate-600">Lowest Access</span>
            </div>
            <p className="text-lg font-bold text-slate-900">{insights.lowestAccess?.regionName || "N/A"}</p>
            <p className="text-xs text-slate-500 mt-1">HAI Score: {(insights.lowestAccess?.haiScore || 0).toFixed(1)}</p>
          </motion.div>

          <motion.div 
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm"
          >
            <div className="flex items-center gap-3 mb-2">
              <div className="p-2 bg-slate-100 rounded-lg">
                <UsersIcon className="text-slate-600" size={20} />
              </div>
              <span className="text-sm font-medium text-slate-600">High Risk Count</span>
            </div>
            <p className="text-3xl font-bold text-slate-900">{insights.totalHighRiskDistricts}</p>
            <p className="text-xs text-slate-500 mt-1">Districts requiring priority</p>
          </motion.div>
        </div>
      )}

      {/* Search Results Placeholder - can be restored if needed */}

      <div className="grid lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white border border-gray-200 rounded-xl p-6 shadow-sm">
          <div className="mb-6 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-slate-900">Lebanon Map</h2>

            {/* Mode Toggle */}
            <div className="flex items-center gap-3">
              <div className="bg-gray-100 rounded-lg p-1 flex">
                <button
                  onClick={() => setMapMode("districts")}
                  className={`px-4 py-2 rounded-md text-sm font-medium transition flex items-center gap-2 ${
                    mapMode === "districts"
                      ? "bg-white text-slate-900 shadow-sm"
                      : "text-gray-600 hover:text-gray-900"
                  }`}
                >
                  <MapIcon size={16} />
                  District Scores
                </button>
                <button
                  onClick={() => setMapMode("hospitals")}
                  className={`px-4 py-2 rounded-md text-sm font-medium transition flex items-center gap-2 ${
                    mapMode === "hospitals"
                      ? "bg-white text-slate-900 shadow-sm"
                      : "text-gray-600 hover:text-gray-900"
                  }`}
                >
                  <Building2 size={16} />
                  Hospitals
                </button>
              </div>
            </div>
          </div>

          {mapMode === "districts" && (
            <div className="mb-4 flex items-center gap-4 text-xs">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-red-600"></div>
                <span className="text-gray-600">Critical</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-orange-600"></div>
                <span className="text-gray-600">Severe</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-amber-600"></div>
                <span className="text-gray-600">High</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-yellow-600"></div>
                <span className="text-gray-600">Moderate</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-green-600"></div>
                <span className="text-gray-600">Low</span>
              </div>
            </div>
          )}

          <div className="relative bg-gray-50 rounded-xl overflow-hidden border border-gray-200 h-[500px] z-0">
            <MapContainer center={[33.8547, 35.8623]} zoom={8} scrollWheelZoom={true} className="w-full h-full z-0">
              <TileLayer
                attribution='&copy; <a href="https://carto.com/">CartoDB</a>'
                url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
              />
              {mapMode === "districts" && geoData && (
                <GeoJSON
                  key={JSON.stringify(geoData) + scores.length + (selectedDistrict || "")}
                  data={geoData}
                  style={geoJsonStyle}
                  onEachFeature={onEachFeature}
                />
              )}
              {mapMode === "districts" && !geoData && (
                <div className="absolute inset-0 flex items-center justify-center bg-white/50 z-[1000]">
                  <div className="bg-white p-4 rounded-lg shadow-lg border border-gray-200">
                    <p className="text-gray-600 font-medium">Loading map boundaries...</p>
                  </div>
                </div>
              )}
              {mapMode === "districts" && geoData && geoData.features && geoData.features.length === 0 && (
                <div className="absolute inset-0 flex items-center justify-center bg-white/50 z-[1000]">
                  <div className="bg-white p-5 rounded-lg shadow-lg border border-red-200 text-center max-w-sm">
                    <p className="text-red-600 font-bold mb-2">Map boundaries not found.</p>
                    <p className="text-sm text-gray-600">Please move your <strong>.geojson</strong> file into the frontend's <code className="bg-gray-100 px-1 py-0.5 rounded">public/</code> folder.</p>
                  </div>
                </div>
              )}
              {mapMode === "hospitals" && hospitals
                .filter((h) => !searchQuery || (h.name || "").toLowerCase().includes(searchQuery.toLowerCase()) || (h.regionName || "").toLowerCase().includes(searchQuery.toLowerCase()))
                .map((hospital) => {
                return (
                  <CircleMarker
                    key={hospital.facilityId || hospital.id}
                    center={[Number(hospital.latitude), Number(hospital.longitude)]}
                    radius={8}
                    pathOptions={{ fillColor: '#38BDF8', color: '#fff', weight: 1.5, fillOpacity: 0.8 }}
                    eventHandlers={{
                      mouseover: () => setHoveredHospitalData(hospital),
                      mouseout: () => setHoveredHospitalData(null),
                    }}
                  >
                    <Tooltip>{hospital.name}</Tooltip>
                  </CircleMarker>
                );
              })}
            </MapContainer>
          </div>
        </div>

        <div className="space-y-6">
          {mapMode === "hospitals" && hoveredHospitalData ? (
            <motion.div
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm"
            >
              <h3 className="text-lg font-semibold text-slate-900 mb-4">
                {hoveredHospitalData.name}
              </h3>
              <div className="space-y-4">
                <div className="border-b border-gray-200 pb-4">
                  <div className="text-sm text-gray-600">District</div>
                  <div className="text-sm font-medium text-slate-900 capitalize mt-1">
                    {hoveredHospitalData.regionName || "Unknown"}
                  </div>
                </div>

            {(user?.role === "MINISTRY_OFFICIAL" || user?.role === "HOSPITAL_ADMIN") && (
                  <div>
                    <div className="text-sm text-gray-600 mb-3">Live Capacity Status</div>
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Total Beds</span>
                        <span className="text-lg font-semibold text-slate-900">
                          {hoveredHospitalData.latestCapacity?.totalBeds || 0}
                        </span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">ICU Beds</span>
                        <span className="text-lg font-semibold text-slate-900">
                          {hoveredHospitalData.latestCapacity?.icuBeds || 0}
                        </span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Medical Staff</span>
                        <span className="text-lg font-semibold text-slate-900">
                          {(hoveredHospitalData.latestCapacity?.doctorsCount || 0) + (hoveredHospitalData.latestCapacity?.nursesCount || 0)}
                        </span>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </motion.div>
          ) : mapMode === "districts" && hoveredData ? (
            <motion.div
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm"
            >
              <h3 className="text-lg font-semibold text-slate-900 mb-4">
                {hoveredData.regionName}
              </h3>
              <div className="space-y-4">
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-gray-600 text-sm">
                      Overall Status
                    </span>
                    <span
                      className="text-sm font-semibold"
                      style={{ color: getRiskColor(hoveredData.riskCategory) }}
                    >
                      {hoveredData.riskCategory || "LOW"}
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <AlertCircleIcon
                      size={16}
                      style={{ color: getRiskColor(hoveredData.riskCategory) }}
                    />
                    <span className="text-2xl font-bold text-slate-900">
                      {(hoveredData.ciriScore || 0).toFixed(1)}/100
                    </span>
                  </div>
                </div>

                <div className="border-t border-gray-200 pt-4 space-y-3">
                  <div>
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-gray-600 text-sm">
                        Regional Vulnerability (RVI)
                      </span>
                      <span className="text-slate-900 font-semibold">
                        {(hoveredData.rviScore || 0).toFixed(1)}/100
                      </span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2">
                      <div
                        className="bg-red-600 h-2 rounded-full transition-all"
                        style={{
                          width: `${hoveredData.rviScore}%`,
                        }}
                      ></div>
                    </div>
                  </div>

                  <div>
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-gray-600 text-sm">
                        Healthcare Access (HAI)
                      </span>
                      <span className="text-slate-900 font-semibold">
                        {(hoveredData.haiScore || 0).toFixed(1)}/100
                      </span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2">
                      <div
                        className="h-2 rounded-full transition-all"
                        style={{
                          width: `${hoveredData.haiScore}%`,
                          backgroundColor: '#7DD3FC',
                        }}
                      ></div>
                    </div>
                  </div>
                </div>

                {hoveredData.population && (
                  <div className="border-t border-gray-200 pt-4 space-y-2">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <UsersIcon size={16} className="text-gray-600" />
                        <span className="text-gray-600 text-sm">Population</span>
                      </div>
                      <span className="text-slate-900 font-semibold">
                        {hoveredData.population.toLocaleString()}
                      </span>
                    </div>
                  </div>
                )}
              </div>
            </motion.div>
          ) : (
            <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
              <p className="text-gray-600 text-center">
                Hover over a region to view details
              </p>
            </div>
          )}

          <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
            <h3 className="text-lg font-semibold text-slate-900 mb-4">
              {searchQuery ? "Search Results" : "Top Priority Regions"}
            </h3>
            <div className="space-y-3">
              {[...scores]
                .filter(s => !searchQuery || (s.regionName || "").toLowerCase().includes(searchQuery.toLowerCase()))
                .sort((a, b) => (b.ciriScore || 0) - (a.ciriScore || 0))
                .slice(0, searchQuery ? 10 : 3)
                .map((score) => (
                  <div
                    key={score.regionId}
                    className="flex items-center justify-between p-3 bg-gray-200/50 rounded-lg cursor-pointer hover:bg-gray-200 transition"
                    onClick={() => handleRegionClick(score.regionId)}
                  >
                    <div className="flex items-center gap-3">
                      <div
                        className="w-3 h-3 rounded-full"
                        style={{
                        backgroundColor: getRiskColor(score.riskCategory),
                        }}
                      ></div>
                      <span className="text-slate-900 font-medium">
                        {score.regionName}
                      </span>
                    </div>
                    <span className="text-gray-600 text-sm">
                      {(score.ciriScore || 0).toFixed(1)}/100
                    </span>
                  </div>
                ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
