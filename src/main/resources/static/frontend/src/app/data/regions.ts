export interface Region {
  id: string;
  name: string;
  refugeePressure: number; // 0-100
  poverty: number; // 0-100
  healthcareAccess: number; // 0-100 (higher is better)
  overallScore: number; // composite score 0-100 (higher means worse conditions)
  population: number;
  refugeeCount: number;
  coordinates: { x: number; y: number }; // For map positioning
}

export interface HistoricalData {
  id: string;
  month: string;
  refugeePressure: number;
  poverty: number;
  healthcareAccess: number;
}

export const regions: Region[] = [
  {
    id: "akkar",
    name: "Akkar",
    refugeePressure: 92,
    poverty: 88,
    healthcareAccess: 25,
    overallScore: 85,
    population: 389899,
    refugeeCount: 125000,
    coordinates: { x: 35, y: 15 },
  },
  {
    id: "baalbek-hermel",
    name: "Baalbek-Hermel",
    refugeePressure: 88,
    poverty: 85,
    healthcareAccess: 28,
    overallScore: 82,
    population: 402000,
    refugeeCount: 110000,
    coordinates: { x: 70, y: 25 },
  },
  {
    id: "north",
    name: "North Lebanon",
    refugeePressure: 75,
    poverty: 72,
    healthcareAccess: 45,
    overallScore: 70,
    population: 732759,
    refugeeCount: 95000,
    coordinates: { x: 40, y: 25 },
  },
  {
    id: "mount-lebanon",
    name: "Mount Lebanon",
    refugeePressure: 45,
    poverty: 38,
    healthcareAccess: 75,
    overallScore: 42,
    population: 1980936,
    refugeeCount: 45000,
    coordinates: { x: 45, y: 45 },
  },
  {
    id: "beirut",
    name: "Beirut",
    refugeePressure: 55,
    poverty: 48,
    healthcareAccess: 82,
    overallScore: 48,
    population: 361366,
    refugeeCount: 38000,
    coordinates: { x: 40, y: 50 },
  },
  {
    id: "bekaa",
    name: "Bekaa",
    refugeePressure: 90,
    poverty: 80,
    healthcareAccess: 32,
    overallScore: 80,
    population: 495442,
    refugeeCount: 105000,
    coordinates: { x: 60, y: 50 },
  },
  {
    id: "south",
    name: "South Lebanon",
    refugeePressure: 65,
    poverty: 62,
    healthcareAccess: 52,
    overallScore: 62,
    population: 590014,
    refugeeCount: 68000,
    coordinates: { x: 45, y: 75 },
  },
  {
    id: "nabatieh",
    name: "Nabatieh",
    refugeePressure: 68,
    poverty: 65,
    healthcareAccess: 48,
    overallScore: 64,
    population: 378946,
    refugeeCount: 52000,
    coordinates: { x: 50, y: 68 },
  },
];

// Generate historical data for the last 12 months
export const generateHistoricalData = (region: Region): HistoricalData[] => {
  const months = [
    "Apr 2025",
    "May 2025",
    "Jun 2025",
    "Jul 2025",
    "Aug 2025",
    "Sep 2025",
    "Oct 2025",
    "Nov 2025",
    "Dec 2025",
    "Jan 2026",
    "Feb 2026",
    "Mar 2026",
  ];

  return months.map((month, index) => {
    // Simulate trend: conditions worsening slightly over time
    const trend = index * 0.5;
    return {
      id: `${region.id}-${index}`,
      month,
      refugeePressure: Math.min(100, region.refugeePressure - 10 + trend),
      poverty: Math.min(100, region.poverty - 8 + trend),
      healthcareAccess: Math.max(0, region.healthcareAccess + 5 - trend),
    };
  });
};

export const getScoreColor = (score: number): string => {
  if (score >= 80) return "#DC2626"; // red-600
  if (score >= 65) return "#EA580C"; // orange-600
  if (score >= 50) return "#D97706"; // amber-600
  if (score >= 35) return "#CA8A04"; // yellow-600
  return "#16A34A"; // green-600
};

export const getScoreLabel = (score: number): string => {
  if (score >= 80) return "Critical";
  if (score >= 65) return "Severe";
  if (score >= 50) return "High";
  if (score >= 35) return "Moderate";
  return "Low";
};
