import { useState } from "react";
import { useNavigate } from "react-router";
import { motion } from "motion/react";
import { Building2, Hospital, Globe, EyeIcon, EyeOffIcon, CheckCircle, Clock } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { useApi } from "../hooks/useApi";

type PortalType = "ministry" | "hospital" | "public" | null;
type UserRole = "MINISTRY_OFFICIAL" | "HOSPITAL_ADMIN" | "PUBLIC";

const roles = [
  { value: "MINISTRY_OFFICIAL", label: "Ministry Official", requiresApproval: true },
  { value: "HOSPITAL_ADMIN", label: "Hospital Admin", requiresApproval: true },
  { value: "PUBLIC", label: "Public Viewer", requiresApproval: false },
];

export default function Login() {
  const [selectedPortal, setSelectedPortal] = useState<PortalType>(null);
  const [isRegister, setIsRegister] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [selectedRole, setSelectedRole] = useState<UserRole>("PUBLIC");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [registrationSuccess, setRegistrationSuccess] = useState(false);
  const [needsApproval, setNeedsApproval] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();
  const { login } = useAuth();
  const api = useApi();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError("");

    try {
      if (isRegister) {
        if (email && password && confirmPassword && fullName && password === confirmPassword) {
          await api.post("/api/auth/request-account", {
            username: fullName,
            email,
            password,
            role: selectedRole,
          });

          if (selectedRole !== "PUBLIC") {
            setNeedsApproval(true);
          }
          setRegistrationSuccess(true);
          
          if (selectedRole === "PUBLIC") {
            setTimeout(() => navigate("/"), 3000);
          }
        } else {
          setError("Please fill all fields and ensure passwords match");
        }
      } else {
        if (email && password) {
          let role: UserRole = "PUBLIC";
          if (selectedPortal === "ministry") role = "MINISTRY_OFFICIAL";
          else if (selectedPortal === "hospital") role = "HOSPITAL_ADMIN";

          const response = await api.post("/api/auth/login", {
            email,
            password,
            role,
          });

          login(response.token, response.role, response.userId, email);
          navigate("/app/dashboard");
        }
      }
    } catch (err: any) {
      setError(err.message || "An error occurred during authentication");
    } finally {
      setIsLoading(false);
    }
  };

  const resetToLogin = () => {
    setIsRegister(false);
    setRegistrationSuccess(false);
    setNeedsApproval(false);
    setEmail("");
    setPassword("");
    setConfirmPassword("");
    setFullName("");
    setSelectedRole("PUBLIC");
    setError("");
  };

  const portals = [
    {
      id: "ministry" as PortalType,
      title: "Ministry Official",
      icon: Building2,
      color: "from-[#8B3A3A] to-[#6B2A2A]",
    },
    {
      id: "hospital" as PortalType,
      title: "Hospital Admin",
      icon: Hospital,
      color: "from-[#7DD3FC] to-[#38BDF8]",
    },
    {
      id: "public" as PortalType,
      title: "Public Viewer",
      icon: Globe,
      color: "from-slate-600 to-slate-700",
    },
  ];

  return (
    <div className="min-h-screen w-full flex">
      {/* Left Side - Login Form */}
      <div className="flex-1 bg-white flex items-center justify-center p-8">
        <div className="w-full max-w-md">
          {!selectedPortal ? (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
            >
              <div className="mb-8">
                {/* Logo inspired by the image */}
                <div className="relative w-16 h-16 mb-6">
                  <svg viewBox="0 0 100 100" className="w-full h-full">
                    <path
                      d="M 20 40 L 50 20 L 80 40 L 80 85 L 20 85 Z"
                      fill="#8B3A3A"
                    />
                    <rect x="35" y="45" width="30" height="8" fill="#7DD3FC" rx="2" />
                    <rect x="46" y="36" width="8" height="28" fill="#7DD3FC" rx="2" />
                  </svg>
                </div>
                <h1 className="text-4xl font-bold text-slate-900 mb-2">
                  LHIMS Portal
                </h1>
                <p className="text-slate-600">
                  Lebanon Health Impact Management System
                </p>
              </div>

              <div className="space-y-3">
                <p className="text-sm font-medium text-slate-700 mb-4">
                  Select your portal to continue
                </p>
                {portals.map((portal, index) => {
                  const Icon = portal.icon;
                  return (
                    <motion.button
                      key={portal.id}
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: index * 0.1 }}
                      onClick={() => setSelectedPortal(portal.id)}
                      className="w-full p-4 border-2 border-slate-200 rounded-xl hover:border-[#8B3A3A] hover:shadow-lg transition-all text-left group"
                    >
                      <div className="flex items-center gap-4">
                        <div
                          className={`p-3 rounded-lg bg-gradient-to-br ${portal.color} group-hover:scale-110 transition-transform`}
                        >
                          <Icon className="text-white" size={24} />
                        </div>
                        <h3 className="font-semibold text-slate-900 text-lg">
                          {portal.title}
                        </h3>
                      </div>
                    </motion.button>
                  );
                })}
              </div>
            </motion.div>
          ) : (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
            >
              {!registrationSuccess ? (
                <>
                  <button
                    onClick={() => {
                      setSelectedPortal(null);
                      resetToLogin();
                    }}
                    className="text-sm text-[#8B3A3A] hover:text-[#6B2A2A] mb-6 flex items-center gap-1"
                  >
                    ← Back to portal selection
                  </button>

                  <div className="mb-8">
                    {/* Logo */}
                    <div className="relative w-12 h-12 mb-4">
                      <svg viewBox="0 0 100 100" className="w-full h-full">
                        <path
                          d="M 20 40 L 50 20 L 80 40 L 80 85 L 20 85 Z"
                          fill="#8B3A3A"
                        />
                        <rect x="35" y="45" width="30" height="8" fill="#7DD3FC" rx="2" />
                        <rect x="46" y="36" width="8" height="28" fill="#7DD3FC" rx="2" />
                      </svg>
                    </div>
                    <h1 className="text-3xl font-bold text-slate-900 mb-2">
                      {isRegister ? "Create Account" : "Welcome back!"}
                    </h1>
                    <p className="text-slate-600">
                      {portals.find((p) => p.id === selectedPortal)?.title}
                    </p>
                    {error && (
                      <div className="mt-4 p-3 bg-red-50 border border-red-200 text-red-600 rounded-lg text-xs font-medium">
                        {error}
                      </div>
                    )}
                  </div>

                  <form onSubmit={handleSubmit} className="space-y-5">
                {isRegister && (
                  <>
                    <div>
                      <label
                        htmlFor="fullName"
                        className="block text-sm font-medium text-slate-700 mb-2"
                      >
                        Full Name
                      </label>
                      <input
                        id="fullName"
                        type="text"
                        value={fullName}
                        onChange={(e) => setFullName(e.target.value)}
                        className="w-full px-4 py-3 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#7DD3FC] focus:border-transparent transition"
                        placeholder="Enter your full name"
                        required
                      />
                    </div>

                    <div>
                      <label
                        htmlFor="role"
                        className="block text-sm font-medium text-slate-700 mb-2"
                      >
                        Role Request
                      </label>
                      <select
                        id="role"
                        value={selectedRole}
                        onChange={(e) => setSelectedRole(e.target.value as UserRole)}
                        className="w-full px-4 py-3 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#7DD3FC] focus:border-transparent transition bg-white"
                        required
                      >
                        {roles.map((role) => (
                          <option key={role.value} value={role.value}>
                            {role.label}
                            {role.requiresApproval ? " (Requires Approval)" : ""}
                          </option>
                        ))}
                      </select>
                      <p className="text-xs text-slate-500 mt-1">
                        {roles.find((r) => r.value === selectedRole)?.requiresApproval
                          ? "This role requires admin approval before access is granted."
                          : "You will have immediate access upon registration."}
                      </p>
                    </div>
                  </>
                )}

                <div>
                  <label
                    htmlFor="email"
                    className="block text-sm font-medium text-slate-700 mb-2"
                  >
                    Email address
                  </label>
                  <input
                    id="email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full px-4 py-3 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#7DD3FC] focus:border-transparent transition"
                    placeholder="Enter your email"
                    required
                  />
                </div>

                <div>
                  <label
                    htmlFor="password"
                    className="block text-sm font-medium text-slate-700 mb-2"
                  >
                    Password
                  </label>
                  <div className="relative">
                    <input
                      id="password"
                      type={showPassword ? "text" : "password"}
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="w-full px-4 py-3 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#7DD3FC] focus:border-transparent transition pr-12"
                      placeholder="Enter your password"
                      required
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                    >
                      {showPassword ? (
                        <EyeOffIcon size={20} />
                      ) : (
                        <EyeIcon size={20} />
                      )}
                    </button>
                  </div>
                </div>

                {isRegister && (
                  <div>
                    <label
                      htmlFor="confirmPassword"
                      className="block text-sm font-medium text-slate-700 mb-2"
                    >
                      Confirm Password
                    </label>
                    <div className="relative">
                      <input
                        id="confirmPassword"
                        type={showConfirmPassword ? "text" : "password"}
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 transition pr-12 ${
                          confirmPassword && password !== confirmPassword
                            ? "border-red-300 focus:ring-red-500"
                            : "border-slate-300 focus:ring-[#7DD3FC]"
                        }`}
                        placeholder="Confirm your password"
                        required
                      />
                      <button
                        type="button"
                        onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                      >
                        {showConfirmPassword ? (
                          <EyeOffIcon size={20} />
                        ) : (
                          <EyeIcon size={20} />
                        )}
                      </button>
                    </div>
                    {confirmPassword && password !== confirmPassword && (
                      <p className="text-xs text-red-600 mt-1">
                        Passwords do not match
                      </p>
                    )}
                  </div>
                )}

                {!isRegister && (
                  <div className="flex items-center justify-between">
                    <label className="flex items-center gap-2 cursor-pointer">
                      <input
                        type="checkbox"
                        className="w-4 h-4 text-[#7DD3FC] border-slate-300 rounded focus:ring-[#7DD3FC]"
                      />
                      <span className="text-sm text-slate-600">Remember me</span>
                    </label>
                    <a
                      href="#"
                      className="text-sm text-[#8B3A3A] hover:text-[#6B2A2A]"
                    >
                      Forgot password?
                    </a>
                  </div>
                )}

                <motion.button
                  whileHover={{ scale: 1.01 }}
                  whileTap={{ scale: 0.99 }}
                  type="submit"
                  disabled={isLoading}
                  className="w-full py-3 bg-[#8B3A3A] hover:bg-[#6B2A2A] text-white font-medium rounded-lg transition shadow-lg shadow-[#8B3A3A]/20 disabled:opacity-70 disabled:cursor-not-allowed"
                >
                  {isLoading ? "Please wait..." : (isRegister ? "Create Account" : "Log in")}
                </motion.button>
                  </form>

                  <div className="text-center mt-5">
                    <p className="text-sm text-slate-600 inline">
                      {isRegister ? "Already have an account? " : "Don't have an account? "}
                    </p>
                    <button
                      onClick={(e) => {
                        e.preventDefault();
                        setIsRegister(!isRegister);
                        setEmail("");
                        setPassword("");
                        setConfirmPassword("");
                        setFullName("");
                      }}
                      className="text-[#8B3A3A] hover:text-[#6B2A2A] font-semibold hover:underline cursor-pointer bg-transparent border-none text-sm ml-1"
                    >
                      {isRegister ? "Sign in" : "Sign up"}
                    </button>
                  </div>
                </>
              ) : (
                <motion.div
                  initial={{ opacity: 0, scale: 0.9 }}
                  animate={{ opacity: 1, scale: 1 }}
                  className="text-center py-8"
                >
                  <div className="mb-6">
                    {needsApproval ? (
                      <div className="w-20 h-20 mx-auto bg-amber-100 rounded-full flex items-center justify-center mb-4">
                        <Clock className="text-amber-600" size={40} />
                      </div>
                    ) : (
                      <div className="w-20 h-20 mx-auto bg-green-100 rounded-full flex items-center justify-center mb-4">
                        <CheckCircle className="text-green-600" size={40} />
                      </div>
                    )}
                  </div>

                  <h2 className="text-2xl font-bold text-slate-900 mb-3">
                    {needsApproval ? "Pending Approval" : "Registration Successful!"}
                  </h2>

                  {needsApproval ? (
                    <div className="space-y-4">
                      <p className="text-slate-600">
                        Your account has been created successfully.
                      </p>
                      <div className="bg-amber-50 border border-amber-200 rounded-lg p-4">
                        <p className="text-sm text-amber-800 font-medium mb-2">
                          Awaiting Admin Approval
                        </p>
                        <p className="text-sm text-amber-700">
                          As a <strong>{roles.find((r) => r.value === selectedRole)?.label}</strong>,
                          your account requires approval from a system administrator. You will
                          receive an email notification once your access has been granted.
                        </p>
                      </div>
                      <button
                        onClick={() => resetToLogin()}
                        className="w-full py-3 bg-[#8B3A3A] hover:bg-[#6B2A2A] text-white font-medium rounded-lg transition mt-6"
                      >
                        Back to Login
                      </button>
                    </div>
                  ) : (
                    <div className="space-y-4">
                      <p className="text-slate-600">
                        Your account has been created successfully. Redirecting you to the
                        dashboard...
                      </p>
                      <div className="flex justify-center">
                        <div className="w-8 h-8 border-4 border-[#7DD3FC] border-t-[#8B3A3A] rounded-full animate-spin"></div>
                      </div>
                    </div>
                  )}
                </motion.div>
              )}
            </motion.div>
          )}
        </div>
      </div>

      {/* Right Side - Healthcare Illustration */}
      <div className="hidden lg:flex flex-1 bg-gradient-to-br from-[#F5E6E8] via-[#E8F5F9] to-[#F0F4F8] relative overflow-hidden">
        {/* Healthcare illustration with brand colors */}
        <div className="absolute inset-0 flex items-center justify-center">
          <svg viewBox="0 0 600 600" className="w-full h-full max-w-2xl">
            {/* Background decorative elements */}
            <motion.circle
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ duration: 0.8 }}
              cx="150"
              cy="150"
              r="80"
              fill="#7DD3FC"
              opacity="0.15"
            />
            <motion.circle
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ duration: 0.8, delay: 0.2 }}
              cx="450"
              cy="450"
              r="100"
              fill="#8B3A3A"
              opacity="0.1"
            />

            {/* Hospital Building */}
            <motion.g
              initial={{ y: 50, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ duration: 0.8, delay: 0.3 }}
            >
              <rect x="200" y="150" width="200" height="180" fill="#8B3A3A" rx="8" />
              <rect x="215" y="170" width="35" height="35" fill="#E8F5F9" rx="4" />
              <rect x="265" y="170" width="35" height="35" fill="#E8F5F9" rx="4" />
              <rect x="315" y="170" width="35" height="35" fill="#E8F5F9" rx="4" />
              <rect x="215" y="220" width="35" height="35" fill="#E8F5F9" rx="4" />
              <rect x="265" y="220" width="35" height="35" fill="#E8F5F9" rx="4" />
              <rect x="315" y="220" width="35" height="35" fill="#E8F5F9" rx="4" />
              <rect x="215" y="270" width="35" height="35" fill="#E8F5F9" rx="4" />
              <rect x="265" y="270" width="35" height="35" fill="#E8F5F9" rx="4" />
              <rect x="315" y="270" width="35" height="35" fill="#E8F5F9" rx="4" />

              {/* Medical Cross on building */}
              <rect x="280" y="100" width="40" height="12" fill="#7DD3FC" rx="4" />
              <rect x="294" y="86" width="12" height="40" fill="#7DD3FC" rx="4" />
            </motion.g>

            {/* Doctor Figure 1 */}
            <motion.g
              initial={{ x: -100, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              transition={{ duration: 0.8, delay: 0.5 }}
            >
              <circle cx="120" cy="380" r="25" fill="#F5D0B8" />
              <rect x="95" y="405" width="50" height="80" fill="#E8F5F9" rx="8" />
              <rect x="100" y="420" width="40" height="3" fill="#7DD3FC" />
              <rect x="100" y="430" width="40" height="3" fill="#7DD3FC" />
            </motion.g>

            {/* Doctor Figure 2 */}
            <motion.g
              initial={{ x: 100, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              transition={{ duration: 0.8, delay: 0.6 }}
            >
              <circle cx="480" cy="400" r="25" fill="#F5D0B8" />
              <rect x="455" y="425" width="50" height="80" fill="#8B3A3A" rx="8" opacity="0.8" />
            </motion.g>

            {/* Nurse Figure */}
            <motion.g
              initial={{ y: 50, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ duration: 0.8, delay: 0.7 }}
            >
              <circle cx="350" cy="420" r="22" fill="#F5D0B8" />
              <rect x="328" y="442" width="44" height="70" fill="#7DD3FC" rx="8" opacity="0.9" />
            </motion.g>

            {/* Medical symbols floating */}
            <motion.g
              animate={{ y: [0, -10, 0] }}
              transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
            >
              <rect x="420" y="180" width="25" height="6" fill="#7DD3FC" rx="3" opacity="0.6" />
              <rect x="429.5" y="170.5" width="6" height="25" fill="#7DD3FC" rx="3" opacity="0.6" />
            </motion.g>

            <motion.g
              animate={{ y: [0, 10, 0] }}
              transition={{ duration: 2.5, repeat: Infinity, ease: "easeInOut", delay: 0.5 }}
            >
              <circle cx="520" cy="250" r="4" fill="#8B3A3A" opacity="0.4" />
              <circle cx="540" cy="270" r="6" fill="#8B3A3A" opacity="0.3" />
              <circle cx="510" cy="290" r="5" fill="#7DD3FC" opacity="0.4" />
            </motion.g>
          </svg>
        </div>

        <div className="relative z-10 flex items-end justify-center p-12 pb-20">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.8 }}
            className="text-center"
          >
            <h2 className="text-3xl font-bold text-slate-800 mb-3">
              Strategic Health Intelligence
            </h2>
            <p className="text-lg text-slate-600 max-w-md">
              Geospatial health simulations across Lebanon's 26 districts for
              data-driven decision making
            </p>
          </motion.div>
        </div>
      </div>
    </div>
  );
}
